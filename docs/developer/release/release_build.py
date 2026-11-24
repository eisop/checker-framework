#!/usr/bin/env python3
"""Release the Checker Framework."""

# See README-release-process.html for more information

from __future__ import annotations

import datetime
import os
import re
import shutil
import subprocess
import sys
import tempfile
import textwrap
import urllib.error
import urllib.request
from pathlib import Path

from maven_central import check_central_token, gradle_user_properties
from release_errors import ReleaseError
from release_utils import (
    check_release_version,
    check_repo,
    check_tools,
    clone_or_reset,
    commit_and_tag,
    continue_or_exit,
    create_empty_file,
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
    CHECKER_FRAMEWORK,
    LIVE_CHECKER_REPO,
    RELEASE_BUILD_COMPLETED_FLAG_FILE,
    RELEASE_FILES_DIR,
    TOOLS,
    execute,
    execute_output,
    execute_status,
)

# Turned on by the --debug command-line option.
debug = False


def print_usage() -> None:
    """Print usage information."""
    print("Usage:    python3 release_build.py [options]")
    print("\n  --debug  turns on debugging mode which produces verbose output")


def clone_or_reset_repos() -> None:
    """Clone the Checker Framework and the annotated JDK, or reset the existing clones.

    The annotated JDK is cloned here, rather than by the build, so that a failure to download it
    happens before the release is committed, and is retried.
    """
    clone_or_reset(LIVE_CHECKER_REPO, CHECKER_FRAMEWORK)
    clone_or_reset(ANNOTATED_JDK_REPO, BUILD_DIR / "jdk", shallow=True)


def download_dependencies() -> None:
    """Download the dependencies of the build, before the release is committed."""
    execute_with_retries(["./gradlew", "resolveDependencies"], CHECKER_FRAMEWORK)


def check_public_key(gpg: str, signature: bytes) -> str | None:
    """Check that the published public key verifies the given signature of "test".

    Users of the release, and Maven Central, fetch the public key from a keyserver.  A copy there
    that has expired, or that lacks the key's user ID, makes the release's signatures look invalid,
    even though the local key is fine.  The signature is verified against the copy on
    keyserver.ubuntu.com, in a temporary keyring.

    Returns:
        a description of the problem, or None if the published key verifies the signature
    """
    packets = subprocess.run(
        [gpg, "--list-packets"], input=signature, capture_output=True, check=False
    ).stdout.decode(errors="replace")
    match = re.search(r"issuer fpr v\d ([0-9A-F]+)", packets) or re.search(
        r"keyid ([0-9A-F]+)", packets
    )
    if match is None:
        return "Could not find the key that signed the test message."
    fingerprint = match.group(1)
    upload = (
        f"Upload your current public key: gpg --keyserver hkps://keyserver.ubuntu.com"
        f" --send-keys {fingerprint}"
    )
    url = f"https://keyserver.ubuntu.com/pks/lookup?op=get&options=mr&search=0x{fingerprint}"
    try:
        with urllib.request.urlopen(url, timeout=60) as response:
            public_key = response.read()
    except urllib.error.HTTPError as e:
        if e.code == 404:
            return f"keyserver.ubuntu.com does not have your key {fingerprint}.  {upload}"
        return f"Could not get your key {fingerprint} from keyserver.ubuntu.com: {e}"
    except OSError as e:
        return f"Could not reach keyserver.ubuntu.com to check your public key: {e}"

    with tempfile.TemporaryDirectory() as home:
        Path(home).chmod(0o700)
        (Path(home) / "test.asc").write_bytes(signature)
        (Path(home) / "test").write_bytes(b"test")
        (Path(home) / "key.asc").write_bytes(public_key)
        options = [gpg, "--homedir", home, "--batch", "--no-autostart"]
        subprocess.run(
            [*options, "--import", str(Path(home) / "key.asc")], capture_output=True, check=False
        )
        status = subprocess.run(
            [
                *options,
                "--status-fd",
                "1",
                "--verify",
                str(Path(home) / "test.asc"),
                str(Path(home) / "test"),
            ],
            capture_output=True,
            check=False,
        ).stdout.decode(errors="replace")
    if "[GNUPG:] GOODSIG" in status:
        return None
    if "[GNUPG:] EXPKEYSIG" in status:
        return f"Your key {fingerprint} on keyserver.ubuntu.com has expired.  {upload}"
    return (
        f"Your key {fingerprint} on keyserver.ubuntu.com does not verify your signatures; it may"
        f" lack its user ID.  {upload}"
    )


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

    javac = Path(os.environ["JAVA_HOME"]) / "bin" / "javac"
    if not javac.exists():
        problems.append(f"{javac} does not exist; set JAVA_21_HOME to a JDK 21 or later.")

    problems.extend(
        f"git config {name} is not set; the release commit needs it."
        for name in ("user.name", "user.email")
        if not execute_output(["git", "config", name], CHECKER_FRAMEWORK).strip()
    )

    # A dry run still connects to GitHub and checks the permission to push.  Pushing the master
    # that was just fetched never fails for another reason.
    dry_run = ["git", "push", "--dry-run", "origin", "origin/master:refs/heads/master"]
    if execute_status(dry_run, CHECKER_FRAMEWORK) != 0:
        problems.append(f"You cannot push to {LIVE_CHECKER_REPO}; release_push pushes the release.")

    if execute_status(["gh", "auth", "status"]) != 0:
        problems.append("gh is not logged in; run gh auth login.  release_push uses it.")

    if problems:
        msg = "The release cannot proceed:\n" + "\n".join(f"* {p}" for p in problems)
        raise ReleaseError(msg)


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
    section = re.compile(
        "^## Version " + re.escape(version) + r" \(.*?(?=^## Version |\Z)", re.MULTILINE | re.DOTALL
    ).search(text)
    if section is None:
        msg = f"{changelog} has no section for version {version}."
        raise ReleaseError(msg)
    issues = re.compile(
        r"^### Closed issues\n\n(.+?)(?=\n\n|\n*\Z)", re.MULTILINE | re.DOTALL
    ).search(text, section.start(), section.end())
    if issues is None:
        return
    filled = textwrap.fill(
        " ".join(issues.group(1).split()), width=100, break_long_words=False, break_on_hyphens=False
    )
    changelog.write_text(text[: issues.start(1)] + filled + text[issues.end(1) :], encoding="utf-8")


def build_and_locally_deploy_maven() -> None:
    """Run `./gradlew publishToMavenLocal -Prelease=true`.

    The artifacts are signed, as for any release build; see gradle-mvn-push.gradle.
    """
    execute(
        "./gradlew publishToMavenLocal -Prelease=true --console=plain",
        working_dir=CHECKER_FRAMEWORK,
    )


def update_version_numbers(old_cf_version: str) -> None:
    """Update the version numbers and release dates in the documentation for the release."""
    execute("./gradlew clean", working_dir=CHECKER_FRAMEWORK)
    execute("./gradlew updateVersionNumbers --console=plain", working_dir=CHECKER_FRAMEWORK)

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
    gradle_cmd = "./gradlew buildAll -Prelease=true --console=plain"
    execute(gradle_cmd, CHECKER_FRAMEWORK)

    # make the checker framework tutorial
    checker_tutorial_dir = Path(CHECKER_FRAMEWORK) / "docs" / "tutorial"
    execute("make", checker_tutorial_dir)

    # Create checker-framework-X.Y.Z.zip and annotation-file-utilities-X.Y.Z.zip, which also makes
    # the manuals, and put them in RELEASE_FILES_DIR.
    execute(
        "./gradlew zip annotationFileUtilitiesZip -Prelease=true --console=plain", CHECKER_FRAMEWORK
    )
    delete_directory_if_exists(RELEASE_FILES_DIR)
    RELEASE_FILES_DIR.mkdir(parents=True)
    for name in (f"checker-framework-{version}.zip", f"annotation-file-utilities-{version}.zip"):
        shutil.copy2(Path(CHECKER_FRAMEWORK) / "build" / name, RELEASE_FILES_DIR)

    # Check the links of the pages that the zip ships, including the external ones, before
    # anything is pushed.  Fix a broken link and start over.
    with tempfile.TemporaryDirectory() as unpack_dir:
        execute(f"unzip -q {RELEASE_FILES_DIR}/checker-framework-{version}.zip -d {unpack_dir}")
        execute(
            f"python3 checker/bin-devel/check-release-links.py --online"
            f" {unpack_dir}/checker-framework-{version}",
            working_dir=CHECKER_FRAMEWORK,
        )

    # clean no longer necessary files left over from building the checker framework tutorial
    execute("make clean", checker_tutorial_dir)

    build_and_locally_deploy_maven()

    # The jars record the release commit, so a file that the build changes would be missing from
    # it.
    if execute_status("git diff-index --quiet HEAD", working_dir=CHECKER_FRAMEWORK) != 0:
        execute("git status --short --untracked-files=no", working_dir=CHECKER_FRAMEWORK)
        msg = "The build changed the files above, which the release commit does not contain."
        raise ReleaseError(msg)


def main(argv: list[str]) -> None:
    """Build the release artifacts for the Checker Framework, including the AFU.

    Also commit and tag the release in the build repository, without pushing it; release_push.py
    pushes it.
    """
    # MANUAL Indicates a manual step
    # AUTO Indicates the step is fully automated.

    delete_if_exists(RELEASE_BUILD_COMPLETED_FLAG_FILE)

    global debug
    debug = has_command_line_option(argv, "--debug")

    print("Building a new release of the Checker Framework and the Annotation File Utilities!")

    print("\nPATH:\n" + os.environ["PATH"] + "\n")

    # The release is built, committed, and tagged in a clone of the GitHub repository, but nothing
    # is pushed until release_push.py.  This step resets the clone, so that the script can be run
    # again after any failure before the push: a clone with an unpushed release commit and tag is
    # indistinguishable from a fresh clone afterwards.  The annotated JDK is cloned or reset, too.

    print_step("Build Step 1a: Clone or reset the repositories.")  # AUTO
    clone_or_reset_repos()

    # This step ensures the previous step worked. It checks to see if we have any modified files,
    # untracked files, or outgoing changesets. If so, it fails.

    print_step("Build Step 1b: Verify repository.")  # AUTO
    check_repo(CHECKER_FRAMEWORK)

    # Downloading the dependencies now, with retries, keeps a network problem from failing the
    # build after the release is committed.

    print_step("Build Step 1c: Download the dependencies.")  # AUTO
    download_dependencies()

    # The release script requires a number of common tools (Maven, make, etc...), a signing key,
    # and permission to push.  This step checks them, to avoid wasting time in the event one is
    # missing late in execution.

    print_step("Build Step 2: Check tools, the signing key, and access to GitHub.")  # AUTO
    check_tools(TOOLS)
    check_prerequisites()

    # The release script reads the current version of the Checker Framework from the latest GitHub
    # release.  The new version is releaseVersion in release.gradle, and must be greater.

    print_step("Build Step 3: Determine release versions.")  # MANUAL

    old_cf_version = current_release_version()
    cf_version = CF_VERSION
    print("Version: " + cf_version + "\n")

    # A version that is not greater would otherwise be noticed only by release_push.py, after the
    # whole build.  Update releaseVersion in release.gradle and start over.
    check_release_version(old_cf_version, cf_version)

    print_step("Build Step 4: Update the version numbers.")  # MANUAL
    update_version_numbers(old_cf_version)
    changelog = Path(CHECKER_FRAMEWORK) / "docs" / "CHANGELOG.md"
    set_changelog_date(changelog, cf_version)
    reflow_closed_issues(changelog, cf_version)

    # The files updated for the release, usually with the new release date and version, are
    # committed, and the commit is tagged.  release_push.py pushes both.  The release is committed
    # before it is built, so that the jars record the release commit.  If the build fails, run
    # this script again; Step 1a discards the commit and the tag.

    print_step("Build Step 5: Commit and tag the release.")  # AUTO
    commit_and_tag(cf_version, CHECKER_FRAMEWORK, f"checker-framework-{cf_version}")

    # The Checker Framework and the Annotation File Utilities are built together.  This is the most
    # time-consuming piece of the release.  There are no more prompts; you might want to get a cup
    # of coffee and do something else until it is done.

    print_step("Build Step 6: Build the Checker Framework and the Annotation File Utilities.")
    build_checker_framework_release(cf_version)

    create_empty_file(RELEASE_BUILD_COMPLETED_FLAG_FILE)


if __name__ == "__main__":
    main(sys.argv)
