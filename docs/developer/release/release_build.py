#!/usr/bin/env python3
"""Release the Checker Framework."""

# See README-release-process.html for more information

from __future__ import annotations

import os
import re
import shutil
import subprocess
import sys
import tempfile
from pathlib import Path

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


def gradle_user_properties() -> dict[str, str]:
    """Return the Gradle properties of the user, which the release build reads.

    They are the properties in gradle.properties in the Gradle user home, and the properties set by
    ORG_GRADLE_PROJECT_ environment variables, which take precedence.

    Returns:
        the Gradle properties of the user
    """
    properties = {}
    gradle_user_home = Path(os.environ.get("GRADLE_USER_HOME", Path.home() / ".gradle"))
    properties_file = gradle_user_home / "gradle.properties"
    if properties_file.exists():
        for line in properties_file.read_text(encoding="utf-8").splitlines():
            match = re.match(r"\s*([^#!\s=:][^\s=:]*)\s*[=:\s]\s*(.*)", line)
            if match:
                properties[match.group(1)] = match.group(2).strip()
    prefix = "ORG_GRADLE_PROJECT_"
    for name, value in os.environ.items():
        if name.startswith(prefix):
            properties[name[len(prefix) :]] = value
    return properties


def check_prerequisites() -> None:
    """Check what the release needs from the user's setup, before anything slow runs.

    The checks cover the signing key, the Maven Central token, the JDK, the git identity, and
    permission to push to GitHub.  gh is optional, so a problem with it is only reported.

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

    problems.extend(
        f"{name} is not set in ~/.gradle/gradle.properties; the publish to Maven Central needs"
        " your Central user token."
        for name in ("SONATYPE_NEXUS_USERNAME", "SONATYPE_NEXUS_PASSWORD")
        if not properties.get(name)
    )

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

    if shutil.which("gh") is None:
        print("gh is not installed, so you will post the GitHub release by hand.")
    elif execute_status(["gh", "auth", "status"]) != 0:
        print("gh is not logged in (gh auth login), so you would post the GitHub release by hand.")

    if problems:
        msg = "The release cannot proceed:\n" + "\n".join(f"* {p}" for p in problems)
        raise ReleaseError(msg)


def check_changelog_date(changelog: Path, version: str) -> None:
    """Check that the date of the changelog's section for the given version is not a placeholder.

    The section's heading is "## Version VERSION (DATE)".  Until release day, DATE may be a
    placeholder such as "June ?, 2026"; a release must not ship it.

    Raises:
        ReleaseError: If the changelog has no section for the version, or its date is a placeholder.
    """
    heading = re.compile("^## Version " + re.escape(version) + r" \((.*)\)$", re.MULTILINE)
    match = heading.search(changelog.read_text(encoding="utf-8"))
    if match is None:
        msg = f"{changelog} has no section for version {version}."
        raise ReleaseError(msg)
    date = match.group(1)
    if "?" in date or "TODO" in date:
        msg = f"The date of version {version} in {changelog} is a placeholder: {date}"
        raise ReleaseError(msg)


def build_and_locally_deploy_maven() -> None:
    """Run `./gradlew publishToMavenLocal -Prelease=true`.

    The artifacts are signed, as for any release build; see gradle-mvn-push.gradle.
    """
    execute("./gradlew publishToMavenLocal -Prelease=true", working_dir=CHECKER_FRAMEWORK)


def update_version_numbers(old_cf_version: str) -> None:
    """Update the version numbers and release dates in the documentation for the release."""
    execute("./gradlew clean", working_dir=CHECKER_FRAMEWORK)
    execute("./gradlew updateVersionNumbers", working_dir=CHECKER_FRAMEWORK)

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
    gradle_cmd = "./gradlew buildAll -Prelease=true"
    execute(gradle_cmd, CHECKER_FRAMEWORK)

    # make the checker framework tutorial
    checker_tutorial_dir = Path(CHECKER_FRAMEWORK) / "docs" / "tutorial"
    execute("make", checker_tutorial_dir)

    # Create checker-framework-X.Y.Z.zip and annotation-file-utilities-X.Y.Z.zip, which also makes
    # the manuals, and put them in RELEASE_FILES_DIR.
    execute("./gradlew zip annotationFileUtilitiesZip -Prelease=true", CHECKER_FRAMEWORK)
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
    check_changelog_date(Path(CHECKER_FRAMEWORK) / "docs" / "CHANGELOG.md", cf_version)

    # A version that is not greater would otherwise be noticed only by release_push.py, after the
    # whole build.  Update releaseVersion in release.gradle and start over.
    check_release_version(old_cf_version, cf_version)

    print_step("Build Step 4: Update the version numbers.")  # MANUAL
    update_version_numbers(old_cf_version)

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
