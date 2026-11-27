#!/usr/bin/env python3
"""Release the Checker Framework."""

# See README-release-process.html for more information

from __future__ import annotations

import datetime
import json
import os
import re
import shutil
import subprocess
import sys
import tempfile
import textwrap
from pathlib import Path

import maven_central
import release_utils
from maven_central import (
    check_central_token,
    clear_local_artifacts,
    drop_saved_deployment,
    gradle_user_properties,
    verify_local_artifacts,
    verify_with_published_key,
)
from release_errors import ReleaseError
from release_utils import (
    changelog_section_span,
    check_release_dir,
    check_release_version,
    check_tools,
    clone_or_reset,
    commit_and_tag,
    continue_or_exit,
    current_release_version,
    delete_directory_if_exists,
    delete_if_exists,
    execute_with_retries,
    has_command_line_option,
    print_step,
)
from release_vars import (
    ANNOTATED_JDK_REPO,
    BUILD_DIR,
    CF_VERSION,
    CHANGELOG,
    CHECKER_FRAMEWORK,
    GITHUB_REPO,
    HELPER_CLONES,
    LIVE_CHECKER_REPO,
    RELEASE_BUILD_COMPLETED_FLAG_FILE,
    RELEASE_FILES_DIR,
    SCRIPTS_CLONE,
    TMP_DIR,
    TOOLS,
    execute,
    execute_output,
    execute_status,
    gradle_release,
)

# Turned on by the --dry-run command-line option.
dry_run = False


def print_usage() -> None:
    """Print usage information."""
    print("Usage:    python3 release_build.py [--dry-run]")
    print(
        "\n  --dry-run  builds the current clone of the release scripts, unattended, without"
        "\n             credentials: it skips the checks of the signing key, the Maven Central"
        "\n             token, and access to GitHub, and checks links offline.  CI runs it."
    )


def clone_or_reset_repos() -> None:
    """Clone the Checker Framework and the annotated JDK, or reset the existing clones.

    The annotated JDK is cloned here, rather than by the build, so that a failure to download it
    happens before the release is committed, and is retried.
    """
    # A dry run builds the clone that runs the scripts, at its master branch.
    clone_or_reset(str(SCRIPTS_CLONE) if dry_run else LIVE_CHECKER_REPO, CHECKER_FRAMEWORK)
    clone_or_reset(ANNOTATED_JDK_REPO, BUILD_DIR / "jdk", shallow=True)


def download_dependencies() -> None:
    """Download the dependencies of the build, before the release is committed."""
    execute_with_retries(
        ["./gradlew", "resolveDependencies", "getPlumeScripts", "getHtmlTools"], CHECKER_FRAMEWORK
    )


def check_public_key(gpg: str, signature: bytes) -> str | None:
    """Check that the published public key verifies the given signature of "test".

    Returns:
        a description of the problem, or None if the published key verifies the signature
    """
    with tempfile.TemporaryDirectory() as tmp:
        test = Path(tmp) / "test"
        test.write_bytes(b"test")
        Path(f"{test}.asc").write_bytes(signature)
        problems = verify_with_published_key(gpg, [test])
    return problems[0] if problems else None


def check_jdk() -> str | None:
    """Check that the JDK of the release is 21 or later.

    A build on an older JDK would leave out framework-errorprone; see settings.gradle.

    Returns:
        a description of the problem, or None if the JDK is 21 or later
    """
    javac = Path(os.environ["JAVA_HOME"]) / "bin" / "javac"
    if not javac.exists():
        return f"{javac} does not exist; set JAVA_21_HOME to a JDK 21 or later."
    javac_version = subprocess.run(
        [str(javac), "-version"], capture_output=True, text=True, check=False
    )
    match = re.search(r"javac (\d+)", javac_version.stdout + javac_version.stderr)
    if match is None or int(match.group(1)) < 21:
        return f"{javac} is not a JDK 21 or later; set JAVA_21_HOME to one."
    return None


def check_prerequisites() -> None:
    """Check what the release needs from the user's setup, before anything slow runs.

    The checks cover the signing key and its published public key, the Maven Central token, the
    JDK, the git identity, and permission to push to GitHub, and that gh, which posts the GitHub
    release, is logged in.

    Raises:
        ReleaseError: If anything that the release needs is missing.
    """
    problems = []
    properties = gradle_user_properties()

    key = properties.get("signing.gnupg.keyName")
    if not key:
        problems.append(
            "signing.gnupg.keyName is not set in ~/.gradle/gradle.properties; set it to your GPG"
            " key, which signs the release."
        )
    else:
        gpg = properties.get("signing.gnupg.executable", "gpg")
        print(f"Signing a test message with {key}; gpg may ask for the passphrase.")
        sign = subprocess.run(
            [gpg, "--local-user", key, "--armor", "--detach-sign"],
            input=b"test",
            capture_output=True,
            check=False,
        )
        if sign.returncode != 0:
            problems.append(
                f"{gpg} cannot sign with {key}: {sign.stderr.decode(errors='replace').strip()}"
            )
        else:
            key_problem = check_public_key(gpg, sign.stdout)
            if key_problem:
                problems.append(key_problem)

    token_names = ("SONATYPE_NEXUS_USERNAME", "SONATYPE_NEXUS_PASSWORD")
    problems.extend(
        f"{name} is not set in ~/.gradle/gradle.properties; the publish to Maven Central needs"
        " your Central user token."
        for name in token_names
        if not properties.get(name)
    )
    if all(properties.get(name) for name in token_names):
        token_problem = check_central_token(*(properties[name] for name in token_names))
        if token_problem:
            problems.append(token_problem)

    jdk_problem = check_jdk()
    if jdk_problem:
        problems.append(jdk_problem)

    problems.extend(
        f"git config {name} is not set; the release commit needs it."
        for name in ("user.name", "user.email")
        # git config fails for a name that is not set.
        if not execute_output(["git", "config", name], CHECKER_FRAMEWORK, check=False).strip()
    )

    # A dry run still connects to GitHub and checks the permission to push.  Pushing the master
    # that was just fetched never fails for another reason.
    dry_run = ["git", "push", "--dry-run", "origin", "origin/master:refs/heads/master"]
    if execute_status(dry_run, CHECKER_FRAMEWORK) != 0:
        problems.append(f"You cannot push to {LIVE_CHECKER_REPO}; release_push pushes the release.")

    if execute_status(["gh", "auth", "status", "--hostname", "github.com"]) != 0:
        problems.append("gh is not logged in; run gh auth login.  release_push uses it.")

    if problems:
        msg = "The release cannot proceed:\n" + "\n".join(f"* {p}" for p in problems)
        raise ReleaseError(msg)


def check_versions(old_cf_version: str, cf_version: str) -> None:
    """Check that the given version can be released, after the given latest release.

    Raises:
        ReleaseError: If the version is not greater than the latest release, if the build clone
            has another version, or if the version's tag is already on GitHub.
    """
    # A version that is not greater would otherwise be noticed only by release_push.py, after the
    # whole build.  Update releaseVersion in release.gradle and start over.  A dry run in CI
    # between a release and the pull request that prepares the next one only warns.
    try:
        check_release_version(old_cf_version, cf_version)
    except ReleaseError as e:
        if not dry_run:
            raise
        print(f"Dry run: {e}")
    # The version comes from the clone that runs the scripts; the release is built from master.
    built_version = execute_output("./gradlew version -q -Prelease=true", CHECKER_FRAMEWORK).strip()
    if built_version != cf_version:
        msg = (
            f"master would release {built_version}, but the release scripts are for {cf_version}."
            f"  Update {SCRIPTS_CLONE} with git pull, and run release_build.py again."
        )
        raise ReleaseError(msg)
    tag = f"checker-framework-{cf_version}"
    if execute_output(["git", "ls-remote", "--tags", "origin", tag], CHECKER_FRAMEWORK).strip():
        msg = f"{tag} is already on GitHub; continue the release with release_push.py."
        raise ReleaseError(msg)


def check_master(cf_version: str) -> None:
    """Check master before it is released, and ask whether to continue on a problem.

    The checks are that CI passed on it, that the changelog names the issues closed since the
    last release, and that there is no open Dependabot alert.
    """
    commit = execute_output(["git", "rev-parse", "HEAD"], CHECKER_FRAMEWORK).strip()
    runs = json.loads(
        execute_output(
            [
                *["gh", "run", "list", "--repo", GITHUB_REPO, "--workflow", "ci.yml"],
                *["--commit", commit, "--json", "conclusion,status,url"],
            ]
        )
        or "[]"
    )
    if not any(run.get("conclusion") == "success" for run in runs):
        states = [
            f"{run.get('status')} {run.get('conclusion') or ''} {run.get('url')}" for run in runs
        ]
        continue_or_exit(f"CI has not passed on master, {commit}: {states or 'no run'}.", False)

    since = execute_output(
        ["gh", "api", f"repos/{GITHUB_REPO}/releases/latest", "--jq", ".published_at"]
    ).strip()
    closed = json.loads(
        execute_output(
            [
                *["gh", "issue", "list", "--repo", GITHUB_REPO, "--state", "closed"],
                *["--search", f"closed:>={since[:10]}", "--limit", "1000"],
                *["--json", "number,stateReason,title"],
            ]
        )
        or "[]"
    )
    text = CHANGELOG.read_text(encoding="utf-8")
    start, end = changelog_section_span(text, cf_version)
    section = text[start:end]
    listed = {
        int(n) for n in re.findall(r"eisop#(\d+)", section.rpartition("### Closed issues")[2])
    }
    missing = [
        f"  eisop#{issue['number']}: {issue['title']}"
        for issue in closed
        if issue.get("stateReason") == "COMPLETED" and issue["number"] not in listed
    ]
    if missing:
        print(f"Issues completed since {since[:10]} that the changelog does not list:")
        print("\n".join(missing))
        continue_or_exit(
            "Add those that the release fixes to its Closed issues, in a pull request, and run"
            " release_build.py again; or continue.",
            False,
        )

    # The alerts can be read only with the security permissions of the repository.
    alerts = execute_output(
        [
            "gh",
            "api",
            f"repos/{GITHUB_REPO}/dependabot/alerts?state=open&per_page=100",
            "--jq",
            "length",
        ],
        check=False,
    ).strip()
    dependabot = f"https://github.com/{GITHUB_REPO}/security/dependabot"
    if not alerts.isdigit():
        print(f"Could not read the Dependabot alerts; look at {dependabot}.")
    elif alerts != "0":
        continue_or_exit(f"There are {alerts} open Dependabot alerts: {dependabot}.", False)


def set_changelog_date(changelog: Path, version: str) -> None:
    """Set the date of the changelog's section for the given version to the release date.

    The section's heading is "## Version VERSION (DATE)".  Until the release, DATE is usually a
    placeholder such as "June ?, 2026".  The release date is today's date in UTC, as in the
    documentation that updateVersionNumbers updates.

    Raises:
        ReleaseError: If the changelog has no section for the version.
    """
    heading = re.compile("^(## Version " + re.escape(version) + r" \().*(\))$", re.MULTILINE)
    text = changelog.read_text(encoding="utf-8")
    if heading.search(text) is None:
        msg = f"{changelog} has no section for version {version}."
        raise ReleaseError(msg)
    today = datetime.datetime.now(datetime.timezone.utc).date()
    date = f"{today:%B} {today.day}, {today.year}"
    changelog.write_text(heading.sub(rf"\g<1>{date}\g<2>", text, count=1), encoding="utf-8")


def reflow_closed_issues(changelog: Path, version: str) -> None:
    """Reflow the "### Closed issues" list of the changelog's section for the given version.

    While a release is unreleased, its list has one issue per line, so that pull requests that add
    issues do not conflict.  The release fills the list into lines of at most 100 characters.
    """
    text = changelog.read_text(encoding="utf-8")
    start, end = changelog_section_span(text, version)
    issues = re.compile(
        r"^### Closed issues\n\n(.+?)(?=\n\n|\n*\Z)", re.MULTILINE | re.DOTALL
    ).search(text, start, end)
    if issues is None:
        return
    filled = textwrap.fill(
        " ".join(issues.group(1).split()), width=100, break_long_words=False, break_on_hyphens=False
    )
    changelog.write_text(text[: issues.start(1)] + filled + text[issues.end(1) :], encoding="utf-8")


def build_and_locally_deploy_maven(version: str) -> None:
    """Publish the release to the local Maven repository, and check it there.

    The artifacts are signed, as for any release build; see gradle-mvn-push.gradle.  release_push.py
    uploads exactly these files to Maven Central.

    Raises:
        ReleaseError: If the artifacts are not ready for Maven Central.
    """
    # Only the files of this build may be uploaded, not those of an earlier one.
    clear_local_artifacts(version)
    execute(gradle_release("publishToMavenLocal"), working_dir=CHECKER_FRAMEWORK)
    problems = verify_local_artifacts(version)
    if problems:
        msg = "The artifacts are not ready for Maven Central:\n" + "\n".join(
            f"* {p}" for p in problems
        )
        raise ReleaseError(msg)
    print(f"The artifacts of {version} are ready for Maven Central.")


def update_version_numbers(old_cf_version: str) -> None:
    """Update the version numbers and release dates in the documentation for the release."""
    execute(gradle_release("clean"), working_dir=CHECKER_FRAMEWORK)
    execute(gradle_release("updateVersionNumbers"), working_dir=CHECKER_FRAMEWORK)

    # Check that updating versions didn't overlook anything.
    print("Here are occurrences of the old version number, " + old_cf_version + ":")
    grep_cmd = f"grep -n -r --exclude-dir=build --exclude-dir=.git -F {old_cf_version}"
    execute_status(grep_cmd, CHECKER_FRAMEWORK)
    continue_or_exit(
        "If any occurrence is not acceptable, then stop the release, update target"
        ' "updateVersionNumbers" in file release.gradle, and start over.'
    )


def build_checker_framework_release(version: str) -> None:
    """Build the release files for the Checker Framework project and run tests.

    The release files are the zip file of the Checker Framework and the zip file of the
    Annotation File Utilities.  They are put in RELEASE_FILES_DIR.

    Raises:
        ReleaseError: If the build changed a file that the release commit tracks.
    """
    # Build the Checker Framework binaries and documents.  Tests are run by release_push.py.
    # Every Gradle command that builds release artifacts passes -Prelease=true; otherwise the
    # build's version is a -SNAPSHOT version.
    execute(gradle_release("buildAll"), CHECKER_FRAMEWORK)

    # make the checker framework tutorial
    checker_tutorial_dir = Path(CHECKER_FRAMEWORK) / "docs" / "tutorial"
    execute("make", checker_tutorial_dir)

    # Create checker-framework-X.Y.Z.zip and annotation-file-utilities-X.Y.Z.zip, which also makes
    # the manuals, and put them in RELEASE_FILES_DIR.
    execute(gradle_release("zip", "annotationFileUtilitiesZip"), CHECKER_FRAMEWORK)
    delete_directory_if_exists(RELEASE_FILES_DIR)
    RELEASE_FILES_DIR.mkdir(parents=True)
    for name in (f"checker-framework-{version}.zip", f"annotation-file-utilities-{version}.zip"):
        shutil.copy2(Path(CHECKER_FRAMEWORK) / "build" / name, RELEASE_FILES_DIR)

    # Check the links of the pages that the zip ships, including the external ones, except in a
    # dry run, before anything is pushed.  Fix a broken link and start over.
    online = [] if dry_run else ["--online"]
    with tempfile.TemporaryDirectory() as unpack_dir:
        execute(f"unzip -q {RELEASE_FILES_DIR}/checker-framework-{version}.zip -d {unpack_dir}")
        execute(
            [
                "python3",
                "checker/bin-devel/check-release-links.py",
                *online,
                f"{unpack_dir}/checker-framework-{version}",
            ],
            working_dir=CHECKER_FRAMEWORK,
        )

    # clean no longer necessary files left over from building the checker framework tutorial
    execute("make clean", checker_tutorial_dir)

    build_and_locally_deploy_maven(version)

    # The jars record the release commit, so a file that the build changes would be missing from
    # it.
    # git status, unlike git diff-index, ignores a file that was rewritten with the same contents.
    changed = execute_output(
        ["git", "status", "--porcelain", "--untracked-files=no"], CHECKER_FRAMEWORK
    )
    if changed:
        print(changed)
        msg = "The build changed the files above, which the release commit does not contain."
        if not dry_run:
            raise ReleaseError(msg)
        # docs/manual/contributors.tex lists the authors in the history, which a shallow clone,
        # and the dry run's own commit, change.
        print(f"Dry run: {msg}")


def main(argv: list[str]) -> None:
    """Build the release artifacts for the Checker Framework, including the AFU.

    Also commit and tag the release in the build repository, without pushing it; release_push.py
    pushes it.
    """
    # MANUAL Indicates a manual step
    # AUTO Indicates the step is fully automated.

    check_release_dir(TMP_DIR)
    delete_if_exists(RELEASE_BUILD_COMPLETED_FLAG_FILE)

    global dry_run
    dry_run = has_command_line_option(argv, "--dry-run")
    if dry_run:
        release_utils.answer_defaults = True
        maven_central.verify_with_local_keyring = True

    print("Building a new release of the Checker Framework and the Annotation File Utilities!")

    print("\nPATH:\n" + os.environ["PATH"] + "\n")

    # The release is built, committed, and tagged in a clone of the GitHub repository, but nothing
    # is pushed until release_push.py.  This step resets the clone, so that the script can be run
    # again after any failure before the push: a clone with an unpushed release commit and tag is
    # indistinguishable from a fresh clone afterwards.  The annotated JDK is cloned or reset, too.

    print_step("Build Step 1: Clone or reset the repositories.")  # AUTO
    clone_or_reset_repos()

    # The release script requires a number of common tools (Maven, make, etc...), a signing key,
    # and permission to push.  This step checks them, to avoid wasting time in the event one is
    # missing late in execution.

    print_step("Build Step 2: Check tools, the signing key, and access to GitHub.")  # AUTO
    if dry_run:
        check_tools([tool for tool in TOOLS if tool != "gh"])
        jdk_problem = check_jdk()
        if jdk_problem:
            raise ReleaseError(jdk_problem)
    else:
        check_tools(TOOLS)
        check_prerequisites()

    # Downloading the dependencies now, with retries, keeps a network problem from failing the
    # build after the release is committed.

    print_step("Build Step 3: Download the dependencies.")  # AUTO
    download_dependencies()

    # The release script reads the current version of the Checker Framework from the latest GitHub
    # release.  The new version is releaseVersion in release.gradle, and must be greater.

    print_step("Build Step 4: Determine release versions.")  # AUTO

    old_cf_version = current_release_version()
    cf_version = CF_VERSION
    print("Version: " + cf_version + "\n")
    check_versions(old_cf_version, cf_version)
    if not dry_run:
        check_master(cf_version)
    # release_push.py publishes the deployment that it uploaded, which this build replaces.
    problem = drop_saved_deployment(cf_version)
    if problem:
        raise ReleaseError(problem)

    print_step("Build Step 5: Update the version numbers.")  # MANUAL
    update_version_numbers(old_cf_version)
    set_changelog_date(CHANGELOG, cf_version)
    reflow_closed_issues(CHANGELOG, cf_version)

    # The files updated for the release, usually with the new release date and version, are
    # committed, and the commit is tagged.  release_push.py pushes both.  The release is committed
    # before it is built, so that the jars record the release commit.  If the build fails, run
    # this script again; Step 1 discards the commit and the tag.

    print_step("Build Step 6: Commit and tag the release.")  # AUTO
    helpers = "".join(
        f"{execute_output(['git', 'remote', 'get-url', 'origin'], clone).strip()}"
        f" {execute_output(['git', 'rev-parse', 'HEAD'], clone).strip()}\n"
        for clone in HELPER_CLONES
    )
    commit_and_tag(
        cf_version,
        CHECKER_FRAMEWORK,
        f"checker-framework-{cf_version}",
        f"Checker Framework {cf_version}\n\nBuilt with:\n{helpers}",
        gradle_user_properties().get("signing.gnupg.keyName"),
    )

    # The Checker Framework and the Annotation File Utilities are built together.  This is the most
    # time-consuming piece of the release.  There are no more prompts; you might want to get a cup
    # of coffee and do something else until it is done.

    print_step("Build Step 7: Build the Checker Framework and the Annotation File Utilities.")
    build_checker_framework_release(cf_version)

    RELEASE_BUILD_COMPLETED_FLAG_FILE.touch()


if __name__ == "__main__":
    try:
        main(sys.argv)
    except ReleaseError as e:
        sys.exit(f"\nrelease_build.py stopped: {e}")
