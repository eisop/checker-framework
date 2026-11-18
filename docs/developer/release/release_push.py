#!/usr/bin/env python3
"""Push the release."""

# See README-release-process.html for more information

import filecmp
import sys
import tempfile
import urllib.error
from pathlib import Path

from release_errors import ReleaseError
from release_utils import (
    check_release_version,
    continue_or_exit,
    current_release_version,
    delete_if_exists,
    download_binary,
    get_announcement_email,
    has_command_line_option,
    print_step,
    prompt_yes_no,
    push_release,
)
from release_vars import (
    CF_VERSION,
    CHECKER_FRAMEWORK,
    RELEASE_BUILD_COMPLETED_FLAG_FILE,
    RELEASE_FILES_DIR,
    execute,
)
from sanity_checks import (
    javac_sanity_check,
    maven_sanity_check,
)


def release_file_names(cf_version: str) -> list[str]:
    """Return the names of the files that are attached to the GitHub release.

    Returns:
        the names of the Checker Framework zip file and the Annotation File Utilities zip file.
    """
    return [f"checker-framework-{cf_version}.zip", f"annotation-file-utilities-{cf_version}.zip"]


def download_github_release_file(url: str, destination: Path) -> str | None:
    """Download a file attached to the GitHub release.

    Returns:
        why the file could not be downloaded, or None if it was.
    """
    try:
        download_binary(url, destination)
    except urllib.error.URLError as e:
        return f"Could not download {url}: {e}"
    return None


def github_release_files_problem(cf_version: str) -> str | None:
    """Compare the files attached to the GitHub release with the ones that were built.

    Returns:
        why the files attached to the GitHub release are not the built ones, or None if they are.
    """
    download_url = (
        "https://github.com/eisop/checker-framework/releases/download/"
        f"checker-framework-{cf_version}"
    )
    with tempfile.TemporaryDirectory() as download_dir:
        for name in release_file_names(cf_version):
            downloaded = Path(download_dir) / name
            print(f"Downloading {download_url}/{name}")
            problem = download_github_release_file(f"{download_url}/{name}", downloaded)
            if problem is not None:
                return problem
            if not filecmp.cmp(downloaded, RELEASE_FILES_DIR / name, shallow=False):
                return f"{name} attached to the GitHub release differs from {RELEASE_FILES_DIR}."
            print(f"{name} attached to the GitHub release is the built one.")
    return None


def check_github_release_files(cf_version: str) -> None:
    """Check the files attached to the GitHub release until they are right or the user gives up.

    A re-run of the script cannot reach the steps after this one, because the release then exists,
    so on a failure the user is asked to fix the GitHub release and try again.

    Raises:
        ReleaseError: If the files are not right and the user chooses not to try again.
    """
    while (problem := github_release_files_problem(cf_version)) is not None:
        print(problem)
        if not prompt_yes_no("Fix the GitHub release, then try again?", True):
            raise ReleaseError(problem)


def validate_args(argv: list[str]) -> None:
    """Validate the command-line arguments.

    Raises:
        ReleaseError: If the command-line arguments are not valid.
    """
    if len(argv) > 3:
        print_usage()
        raise ReleaseError("Invalid arguments. " + ",".join(argv))
    for i in range(1, len(argv)):
        if argv[i] != "release":
            print_usage()
            raise ReleaseError("Invalid arguments. " + ",".join(argv))


def print_usage() -> None:
    """Print instructions on how to use this script, including how to set test or release mode."""
    print(
        "Usage: python3 release_push.py [release]\n"
        'If the "release" argument is '
        "NOT specified then the script will execute all steps that checking and prompting "
        "steps but will NOT actually perform a release.  This is for testing the script."
    )


def main(argv: list[str]) -> None:
    """Test the release, push it to GitHub, and guide the user through the manual steps.

    The manual steps include posting the GitHub release, from which the web site is generated,
    and sending the release announcement e-mail.

    This script does not publish to Maven Central; that is a separate step.
    See docs/developer/maven-central-publishing.md.
    """
    # MANUAL Indicates a manual step
    # AUTO Indicates the step is fully automated.

    current_cf_version = current_release_version()
    new_cf_version = CF_VERSION
    test_mode = not has_command_line_option(argv, "release")
    validate_args(argv)

    if test_mode:
        msg = (
            "You have chosen test_mode.\n"
            "This means that this script will execute all build steps that do not have side "
            "effects.  That is, this is a test run of the script.  All checks and user prompts "
            "will be shown but no steps will be executed that will cause the release to be "
            "deployed or partially deployed.\n"
            'If you meant to do an actual release, re-run this script with one argument, "release".'
        )
    else:
        msg = (
            "You have chosen release_mode.  "
            "Please follow the prompts to run a full Checker Framework release."
        )

    continue_or_exit(msg + "\n")
    if test_mode:
        print("Continuing in test mode.")
    else:
        print("Continuing in release mode.")

    if not Path(RELEASE_BUILD_COMPLETED_FLAG_FILE).exists():
        continue_or_exit(
            "It appears that release_build.py has not been run since the last push to "
            "the Checker Framework repository.  Please ensure it has "
            "been run."
        )

    # The release script checks that the new release version is greater than the previous release
    # version.

    print_step("Push Step 1: Checking release versions")  # SEMIAUTO
    check_release_version(current_cf_version, new_cf_version)

    print(
        f"Checker Framework and AFU:  current-version={current_cf_version}"
        f"    new-version={new_cf_version}"
    )

    # Runs sanity tests on the release files that release_build.py built.  release_build.py
    # already checked the links of the pages in the Checker Framework zip file.

    print_step("Push Step 2: Run sanity tests")  # SEMIAUTO
    if prompt_yes_no("Perform this step?", True):
        print_step("2a: Run javac sanity test on the release zip file.")
        if prompt_yes_no("Run javac sanity test on the release zip file?", True):
            javac_sanity_check(
                RELEASE_FILES_DIR / f"checker-framework-{new_cf_version}.zip", new_cf_version
            )

        print_step("2b: Run Maven sanity test on the local Maven repository.")
        if prompt_yes_no("Run Maven sanity test on the local Maven repository?", True):
            maven_sanity_check("maven-dev")

    # Runs all tests on the release.

    print_step("Push Step 3: Run all tests (takes a long time)")
    if prompt_yes_no("Perform this step?", True):
        gradle_cmd = "./gradlew allTests"
        execute(gradle_cmd, CHECKER_FRAMEWORK)

        gradle_cmd = "./gradlew :annotation-file-utilities:allTests"
        execute(gradle_cmd, CHECKER_FRAMEWORK)

    # The web site is generated from the GitHub release.  Running the generator on the release zip
    # file shows problems with the generated pages before anything is published.

    release_zip = RELEASE_FILES_DIR / f"checker-framework-{new_cf_version}.zip"
    print_step("Push Step 4: Check the web site that will be generated")  # MANUAL
    continue_or_exit(
        "In a clone of https://github.com/eisop/eisop.github.io, run the web site generator on\n"
        'the release zip file and look at the generated pages (see its README, "Pre-release\n'
        'validation"):\n'
        "  mvn -q package\n"
        "  java -cp target/eisop.github.io-1.0-SNAPSHOT-jar-with-dependencies.jar \\\n"
        "    io.github.eisop.website.EisopSiteGenerator \\\n"
        f"    --local-release {release_zip} --only-latest\n"
    )

    # This step pushes the release commit and its tag to GitHub. This is the first irreversible
    # change. After this point, you can no longer backout changes and should do another release
    # in case of critical errors.

    tag = f"checker-framework-{new_cf_version}"
    print_step("Push Step 5. Push the release to GitHub")  # SEMIAUTO
    # This step could be performed without asking for user input but I think we should err on the
    # side of caution.
    if not test_mode:
        if not prompt_yes_no(f"Push master and {tag} to GitHub?  This is irreversible.", True):
            # Posting a GitHub release for a tag that is not on GitHub would create the tag on
            # the head of master, which is not the release commit.
            raise ReleaseError("The release was not pushed, so it cannot be posted.")
        push_release(CHECKER_FRAMEWORK, tag)
        print("Pushed to GitHub")
    else:
        print("Test mode: Skipping push to GitHub!")

    if test_mode:
        print("Test complete")
    else:
        print_step(
            "Push Step 6. Post the Checker Framework release, with the Annotation File Utilities,"
            " on GitHub."
        )  # MANUAL

        release_files = "".join(
            f"  {RELEASE_FILES_DIR / name}\n" for name in release_file_names(new_cf_version)
        )
        msg = (
            "\n"
            "The web site is generated from this GitHub release.  Its files are in "
            f"{RELEASE_FILES_DIR}; if this script runs on another machine, copy them to yours:\n"
            + release_files
            + "\n"
            "To post the Checker Framework release on GitHub:\n"
            "\n"
            f"* Browse to https://github.com/eisop/checker-framework/releases/new?tag={tag}\n"
            f"* For the release title, enter: Checker Framework {new_cf_version}\n"
            "* For the description, insert the latest Checker Framework changelog entry "
            f"(in {CHECKER_FRAMEWORK}/docs/CHANGELOG.md). Please include the first "
            "line with the release version and date.\n"
            '* Find the link below "Attach binaries by dropping them here or selecting them." '
            'Click on "selecting them" and upload both files from your machine.\n'
            "  The Annotation File Utilities zip must be attached to this release: the web site"
            " takes it from here.\n"
            '* Click on the green "Publish release" button.\n'
        )

        continue_or_exit(msg)

        print_step("Push Step 7. Check the files attached to the GitHub release.")  # AUTO
        check_github_release_files(new_cf_version)

        print_step("Push Step 8. Publish the web site.")  # MANUAL
        continue_or_exit(
            "The web site is published daily at 20:00 UTC.  To publish it now, run the\n"
            '"Publish website" workflow with dry_run unchecked:\n'
            "  https://github.com/eisop/eisop.github.io/actions/workflows/publish-website.yml\n"
            "or:\n"
            "  gh workflow run publish-website.yml -R eisop/eisop.github.io -f dry_run=false\n"
            f"When it is done, check that https://eisop.github.io/cf/ shows {new_cf_version}.\n"
        )

        print_step("Push Step 9. Announce the release.")  # MANUAL
        continue_or_exit(
            "Please announce the release using the email structure below.\n"
            + get_announcement_email(new_cf_version)
        )

        print_step("Push Step 10. Prep for next Checker Framework release.")  # MANUAL
        continue_or_exit(
            "Change releaseVersion in release.gradle to the version of the next release,\n"
            "for example by incrementing the -eisopN suffix.  Builds without -Prelease=true\n"
            "then produce -SNAPSHOT versions of that release.\n"
        )

        print_step("Push Step 11. Update the Checker Framework Gradle plugin.")  # MANUAL
        print(
            "Do this after publishing to Maven Central (see the release process README), once\n"
            "Maven Central has propagated the release.\n"
        )
        continue_or_exit(
            "Please update the Checker Framework Gradle plugin:\n"
            "https://github.com/kelloggm/checkerframework-gradle-plugin/blob/master/RELEASE.md#updating-the-checker-framework-version\n"
        )
        continue_or_exit(
            "Make a pull request to the Checker Framework that\n"
            "updates the version number of the Checker Framework\n"
            "Gradle Plugin in docs/examples/lombok and docs/examples/errorprone .\n"
            "The pull request's tests will fail; you will merge it in a day."
        )
        continue_or_exit(
            "Make a pull request to the Checker Framework that\n"
            "updates the CF version number in the BazelExample, by re-pinning the versions."
        )

    if not test_mode:
        delete_if_exists(RELEASE_BUILD_COMPLETED_FLAG_FILE)

    print("Done with release_push.py.\n")


if __name__ == "__main__":
    main(sys.argv)
