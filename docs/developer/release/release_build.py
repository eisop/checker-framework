#!/usr/bin/env python
"""Release the Checker Framework."""

# See README-release-process.html for more information

from __future__ import annotations

import os
import re
import shutil
import sys
import tempfile
from pathlib import Path

from release_errors import ReleaseError
from release_utils import (
    check_repo,
    check_tools,
    clone_from_scratch_or_update,
    commit_and_tag,
    continue_or_exit,
    create_empty_file,
    current_release_version,
    delete_directory_if_exists,
    delete_if_exists,
    has_command_line_option,
    print_step,
    prompt_to_continue,
    prompt_yes_no,
)
from release_vars import (
    CF_VERSION,
    CHECKER_FRAMEWORK,
    LIVE_CHECKER_REPO,
    RELEASE_BUILD_COMPLETED_FLAG_FILE,
    RELEASE_FILES_DIR,
    TOOLS,
    execute,
    execute_status,
)

# Turned on by the --debug command-line option.
debug = False


def print_usage() -> None:
    """Print usage information."""
    print("Usage:    python3 release_build.py [options]")
    print("\n  --debug  turns on debugging mode which produces verbose output")


def clone_or_update_repo() -> None:
    """Clone the Checker Framework from scratch or update it if it exists.

    The action taken depends on a user query.
    """
    message = f"""Before building the release, we clone or update the Checker Framework in
{CHECKER_FRAMEWORK}
However, if you have had to run the script multiple times today and no files
have changed since the last attempt, you may skip this step.
WARNING: IF THIS IS YOUR FIRST RUN OF THE RELEASE ON RELEASE DAY, DO NOT SKIP THIS STEP.
If an earlier run committed and tagged a release that was not pushed, clone from scratch.
Clone the repository from scratch (answer no to get a chance to update it instead)?"""

    clone_from_scratch = True

    if not prompt_yes_no(message, True):
        clone_from_scratch = False
        if not prompt_yes_no("Update the repository without cloning it from scratch?", True):
            print("WARNING: Continuing without refreshing the repository.\n")
            return

    clone_from_scratch_or_update(LIVE_CHECKER_REPO, CHECKER_FRAMEWORK, clone_from_scratch)


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


def build_checker_framework_release(version: str, old_cf_version: str) -> None:
    """Build the release files for the Checker Framework project and run tests.

    The release files are the zip file of the Checker Framework and the zip file of the
    Annotation File Utilities.  They are put in RELEASE_FILES_DIR.
    """
    execute("./gradlew clean", working_dir=CHECKER_FRAMEWORK)

    # update versions
    execute("./gradlew updateVersionNumbers", working_dir=CHECKER_FRAMEWORK)

    # Check that updating versions didn't overlook anything.
    print("Here are occurrences of the old version number, " + old_cf_version + ":")
    grep_cmd = f"grep -n -r --exclude-dir=build --exclude-dir=.git -F {old_cf_version}"
    execute_status(grep_cmd, CHECKER_FRAMEWORK)
    continue_or_exit(
        "If any occurrence is not acceptable, then stop the release, update target"
        ' "updateVersionNumbers" in file release.gradle, and start over.'
    )

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
    # is pushed until release_push.py.  If we are running the build script multiple times without
    # pushing the release, the clone must be cleaned before we run the release_build script again.
    # The "Clone/update repository" step updates the clone with respect to the GitHub repository,
    # but it is the "Verify repository" step that ensures that it is clean, i.e. indistinguishable
    # from a freshly cloned repository.

    print_step("Build Step 1a: Clone/update repository.")  # MANUAL
    clone_or_update_repo()

    # This step ensures the previous step worked. It checks to see if we have any modified files,
    # untracked files, or outgoing changesets. If so, it fails.

    print_step("Build Step 1b: Verify repository.")  # MANUAL
    check_repo(CHECKER_FRAMEWORK)

    # The release script requires a number of common tools (Maven, make, etc...). This step
    # checks to make sure all tools are available on the command line in order to avoid wasting time
    # in the event a tool is missing late in execution.

    print_step("Build Step 2: Check tools.")  # AUTO
    check_tools(TOOLS)

    # The release script reads the current version of the Checker Framework from the latest GitHub
    # release.  The new version is releaseVersion in release.gradle.  Lower or equivalent
    # versions are not possible and will be rejected when you try to push the release.

    print_step("Build Step 3: Determine release versions.")  # MANUAL

    old_cf_version = current_release_version()
    cf_version = CF_VERSION
    print("Version: " + cf_version + "\n")
    check_changelog_date(Path(CHECKER_FRAMEWORK) / "docs" / "CHANGELOG.md", cf_version)

    if old_cf_version == cf_version:
        print(
            "It is *strongly discouraged* to not update the release version numbers for "
            "the Checker Framework even if no changes were made to these in a month. "
            "This would break so much in the release scripts that they would become unusable. "
            "Update releaseVersion in checker-framework/release.gradle\n"
        )
        prompt_to_continue()

    # The Checker Framework and the Annotation File Utilities are built together.  This is the most
    # time-consuming piece of the release. There are no prompts from this step forward; you
    # might want to get a cup of coffee and do something else until it is done.

    print_step("Build Step 4: Build the Checker Framework and the Annotation File Utilities.")
    build_checker_framework_release(cf_version, old_cf_version)

    # The files updated for the release, usually with the new release date and version, are
    # committed, and the commit is tagged.  release_push.py pushes both.

    print_step("Build Step 5: Commit and tag the release.")  # AUTO
    commit_and_tag(cf_version, CHECKER_FRAMEWORK, f"checker-framework-{cf_version}")

    create_empty_file(RELEASE_BUILD_COMPLETED_FLAG_FILE)


if __name__ == "__main__":
    main(sys.argv)
