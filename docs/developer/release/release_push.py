#!/usr/bin/env python
"""Push the release."""

# See README-release-process.html for more information

import pathlib
import subprocess
import sys
from pathlib import Path

from release_errors import ReleaseError
from release_utils import (
    continue_or_exit,
    current_distribution_by_website,
    delete_directory,
    delete_directory_if_exists,
    delete_if_exists,
    ensure_group_access,
    get_announcement_email,
    has_command_line_option,
    print_step,
    prompt_yes_no,
    push_changes_prompt_if_fail,
    set_umask,
    version_number_to_array,
)
from release_vars import (
    AFU_LIVE_RELEASES_DIR,
    ANNO_FILE_UTILITIES,
    CF_VERSION,
    CHECKER_FRAMEWORK,
    CHECKER_LIVE_API_DIR,
    CHECKER_LIVE_RELEASES_DIR,
    CHECKLINK,
    DEV_SITE_DIR,
    DEV_SITE_URL,
    INTERM_ANNO_REPO,
    INTERM_CHECKER_REPO,
    LIVE_SITE_DIR,
    LIVE_SITE_URL,
    RELEASE_BUILD_COMPLETED_FLAG_FILE,
    SANITY_DIR,
    SCRIPTS_DIR,
    TMP_DIR,
    execute,
)
from sanity_checks import (
    javac_sanity_check,
    maven_sanity_check,
)


def check_release_version(previous_release: str, new_release: str) -> None:
    """Ensure that the given new release version is greater than the given previous one.

    Raises:
        ReleaseError: If the new version is not greater than the previous one.
    """
    if version_number_to_array(previous_release) >= version_number_to_array(new_release):
        raise ReleaseError(
            "Previous release version ("
            + previous_release
            + ") should be less than "
            + "the new release version ("
            + new_release
            + ")"
        )


def copy_release_dir(
    path_to_dev_releases: Path, path_to_live_releases: Path, release_version: str
) -> Path:
    """Copy a release directory from the dev site to the live site.

    For example,
    <DEV_SITE_DIR>/releases/2.0.0 ->
    <LIVE_SITE_DIR>/releases/2.0.0

    Returns:
        the destination location.

    Raises:
        ReleaseError: If the destination cannot be deleted.
    """
    source_location = path_to_dev_releases / release_version
    dest_location = path_to_live_releases / release_version

    if dest_location.exists():
        delete_directory(dest_location)

    if dest_location.exists():
        raise ReleaseError("Destination location exists: " + str(dest_location))

    # The / at the end of the source location is necessary so that
    # rsync copies the files in the source directory to the destination directory
    # rather than a subdirectory of the destination directory.
    cmd = (
        "rsync --no-p --no-group --omit-dir-times --recursive --links --quiet"
        f" {source_location}/ {dest_location}"
    )
    execute(cmd)

    return dest_location


def promote_release(path_to_releases: Path, release_version: str) -> None:
    """Copy a release directory to the top level.

    For example,
    <LIVE_SITE_DIR>/releases/2.0.0/* ->
    <LIVE_SITE_DIR>/*
    """
    from_dir = Path(path_to_releases) / release_version
    to_dir = Path(path_to_releases) / ".."
    # Trailing slash is crucial.
    cmd = f"rsync -aJ --no-perms --no-group --omit-dir-times {from_dir}/ {to_dir}"
    execute(cmd)


def copy_htaccess() -> None:
    """Copy the .htaccess file from the dev site to the live site."""
    live_htaccess = Path(LIVE_SITE_DIR) / ".htaccess"
    execute(f"rsync --times {Path(DEV_SITE_DIR) / '.htaccess'} {live_htaccess}")
    ensure_group_access(live_htaccess)


def copy_releases_to_live_site(cf_version: str) -> None:
    """Copy the new releases of the AFU and the Checker Framework from the dev to the live site."""
    checker_interm_releases_dir = Path(DEV_SITE_DIR) / "releases"
    copy_release_dir(checker_interm_releases_dir, CHECKER_LIVE_RELEASES_DIR, cf_version)
    delete_directory_if_exists(CHECKER_LIVE_API_DIR)
    promote_release(CHECKER_LIVE_RELEASES_DIR, cf_version)
    # NO-AFU: Until the Annotation File Utilities are part of the Checker Framework release, they
    # have their own releases directory.
    afu_interm_releases_dir = Path(DEV_SITE_DIR) / "annotation-file-utilities" / "releases"
    copy_release_dir(afu_interm_releases_dir, AFU_LIVE_RELEASES_DIR, cf_version)
    promote_release(AFU_LIVE_RELEASES_DIR, cf_version)


def ensure_group_access_to_releases() -> None:
    """Give group access to the "releases" directories on the live web site.

    That is, to all files and directories in them, for the AFU and the Checker Framework.
    """
    ensure_group_access(AFU_LIVE_RELEASES_DIR)  # NO-AFU
    ensure_group_access(CHECKER_LIVE_RELEASES_DIR)


def is_file_empty(filename: Path) -> bool:
    """Return true if the given file has size 0.

    Returns:
        true if the given file has size 0.
    """
    return filename.stat().st_size == 0


def run_link_checker(site: str, output_file: Path, additional_param: str = "") -> Path:
    """Run the link checker on the given web site and save the output to the given file.

    Additional parameters (if given) are passed directly to the link checker script.

    Returns:
        The given output file.

    Raises:
        ReleaseError: If the link checker fails and the user chooses not to continue.
    """
    delete_if_exists(output_file)
    check_links_script = Path(SCRIPTS_DIR) / "checkLinks.sh"
    if additional_param == "":
        cmd = ["sh", str(check_links_script), site]
    else:
        cmd = ["sh", str(check_links_script), additional_param, site]
    env = {"CHECKLINK": str(CHECKLINK)}

    print(
        "Executing: "
        + " ".join(f"{key2}={val2}" for (key2, val2) in list(env.items()))
        + " "
        + " ".join(cmd)
    )
    with Path.open(output_file, "w+") as out_file:
        process = subprocess.Popen(cmd, env=env, stdout=out_file, stderr=out_file)
        process.communicate()
        process.wait()

    if process.returncode != 0:
        msg = (
            f"Non-zero return code ({process.returncode}; see output in {output_file})"
            f" while executing {cmd}"
        )
        print(msg + "\n")
        if not prompt_yes_no("Continue despite link checker results?", True):
            raise ReleaseError(msg)

    return output_file


def check_all_links(
    afu_website: str,
    checker_website: str,
    suffix: str,
    test_mode: bool,
    cf_version_of_broken_link_to_suppress: str = "",
) -> None:
    """Check all links on the given web sites for the AFU and the Checker Framework.

    The suffix parameter should be "dev" for the
    dev web site and "live" for the live web site. test_mode indicates
    whether this script is being run in release or in test mode. The
    cf_version_of_broken_link_to_suppress parameter should be set to the
    new Checker Framework version and should only be passed when checking links
    for the dev web site (to prevent reporting of a broken link to the
    not-yet-live zip file for the new release).

    Raises:
        ReleaseError: If there are link checking errors.
    """
    afu_check = run_link_checker(afu_website, TMP_DIR / f"afu.{suffix}.check")  # NO-AFU
    additional_param = ""
    if cf_version_of_broken_link_to_suppress != "":
        additional_param = (
            f"--suppress-broken 404:{LIVE_SITE_URL}/checker-framework-"
            + cf_version_of_broken_link_to_suppress
            + ".zip"
        )
    checker_check = run_link_checker(
        checker_website,
        TMP_DIR / f"checker-framework.{suffix}.check",
        additional_param,
    )

    is_afu_check_empty = is_file_empty(afu_check)
    is_checker_check_empty = is_file_empty(checker_check)

    errors_reported = not (is_afu_check_empty and is_checker_check_empty)
    if errors_reported:
        print("Link checker results can be found at:\n")
    if not is_afu_check_empty:
        print(f"\t{afu_check}\n")
    if not is_checker_check_empty:
        print(f"\t{checker_check}\n")
    if errors_reported and not prompt_yes_no("Continue despite link checker results?", True):
        release_option = ""
        if not test_mode:
            release_option = " release"
        raise ReleaseError(
            "The link checker reported errors.  Please fix them by committing changes to the\n"
            "mainline repository and pushing them to GitHub, then updating the development\n"
            "and live sites by running\n"
            "  python3 release_build.py\n"
            "  python3 release_push" + release_option + "\n"
        )


def push_interm_to_release_repos() -> None:
    """Push the release to the GitHub repositories for the AFU and the Checker Framework.

    This is an irreversible step.
    """
    push_changes_prompt_if_fail(INTERM_ANNO_REPO)  # NO-AFU
    push_changes_prompt_if_fail(INTERM_CHECKER_REPO)


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
    """Copy the artifacts of the AFU and the Checker Framework from the dev site to the live site.

    It also performs link checking on the live site, pushes
    the release to GitHub repositories, and guides the user to
    perform manual steps such as sending the
    release announcement e-mail.

    This script does not publish to Maven Central; that is a separate step.
    See docs/developer/maven-central-publishing.md.

    Raises:
        ReleaseError: If the file does not exist.
    """
    # MANUAL Indicates a manual step
    # AUTO Indicates the step is fully automated.

    set_umask()

    validate_args(argv)
    test_mode = not has_command_line_option(argv, "release")

    m2_settings = str(pathlib.Path("~").expanduser()) + "/.m2/settings.xml"
    if not pathlib.Path(m2_settings).exists():
        raise ReleaseError("File does not exist: " + m2_settings)

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

    if not pathlib.Path(RELEASE_BUILD_COMPLETED_FLAG_FILE).exists():
        continue_or_exit(
            "It appears that release_build.py has not been run since the last push to "
            "the AFU or Checker Framework repositories.  Please ensure it has "
            "been run."
        )

    # The release script checks that the new release version is greater than the previous release
    # version.

    print_step("Push Step 1: Checking release versions")  # SEMIAUTO
    dev_afu_website = f"{DEV_SITE_URL}/annotation-file-utilities"  # NO-AFU
    live_afu_website = f"{LIVE_SITE_URL}/annotation-file-utilities"  # NO-AFU

    dev_checker_website = DEV_SITE_URL
    live_checker_website = LIVE_SITE_URL
    current_cf_version = current_distribution_by_website(live_checker_website)
    new_cf_version = CF_VERSION
    check_release_version(current_cf_version, new_cf_version)

    print(
        f"Checker Framework and AFU:  current-version={current_cf_version}"
        f"    new-version={new_cf_version}"
    )

    # Runs the link the checker on all websites at the dev site.
    # The output of the link checker is written to files in the /tmp/$USER/cf-release directory
    # whose locations will be output at the command prompt if the link checker reported errors.

    # In rare instances (such as when a link is correct but the link checker is
    # unable to check it), you may add a suppression to the checklink-args.txt file.
    # In extremely rare instances (such as when a website happens to be down at the
    # time you ran the link checker), you may ignore an error.

    print_step("Push Step 2: Check links on development site")  # SEMIAUTO

    if prompt_yes_no("Run link checker on DEV site?", True):
        check_all_links(dev_afu_website, dev_checker_website, "dev", test_mode, new_cf_version)

    # Runs sanity tests on the development release. Later, we will run a smaller set of sanity
    # tests on the live release to ensure no errors occurred when promoting the release.

    print_step("Push Step 3: Run development sanity tests")  # SEMIAUTO
    if prompt_yes_no("Perform this step?", True):
        print_step("3a: Run javac sanity test on development release.")
        if prompt_yes_no("Run javac sanity test on development release?", True):
            javac_sanity_check(dev_checker_website, new_cf_version)

        print_step("3b: Run Maven sanity test on development release.")
        if prompt_yes_no("Run Maven sanity test on development repo?", True):
            maven_sanity_check("maven-dev")

    # Runs all tests on the development release.

    print_step("Push Step 4: Run all tests (takes a long time)")
    if prompt_yes_no("Perform this step?", True):
        gradle_cmd = "./gradlew allTests"
        execute(gradle_cmd, CHECKER_FRAMEWORK)

        gradle_cmd = "./gradlew test"
        execute(gradle_cmd, ANNO_FILE_UTILITIES)

    # This step copies the development release directories to the live release directories.
    # It then adds the appropriate permissions to the release. Symlinks need to be updated to point
    # to the live website rather than the development website. A straight copy of the directory
    # will NOT update the symlinks.

    print_step("Push Step 5. Copy dev current release website to live website")  # SEMIAUTO
    if not test_mode:
        if prompt_yes_no("Copy release to the live website?"):
            print("Copying to live site")
            copy_releases_to_live_site(new_cf_version)
            copy_htaccess()
            ensure_group_access_to_releases()
    else:
        print("Test mode: Skipping copy to live site!")

    # This step downloads the checker-framework-X.Y.Z.zip file of the newly live release and ensures
    # we can run the Nullness Checker. If this step fails, you should backout the release.

    print_step("Push Step 6: Run javac sanity tests on the live release.")  # SEMIAUTO
    if not test_mode:
        if prompt_yes_no("Run javac sanity test on live release?", True):
            javac_sanity_check(live_checker_website, new_cf_version)
            sanity_test_checker_framework_dir = SANITY_DIR / "test-checker-framework"
            if not pathlib.Path(sanity_test_checker_framework_dir).is_dir():
                execute(f"mkdir -p {sanity_test_checker_framework_dir}")
            sanity_test_script = Path(SCRIPTS_DIR) / "test-checker-framework.sh"
            execute(
                "sh " + str(sanity_test_script) + " " + new_cf_version,
                sanity_test_checker_framework_dir,
            )
    else:
        print("Test mode: Skipping javac sanity tests on the live release.")

    # Runs the link the checker on all websites at:
    # https://eisop.github.io/
    # The output of the link checker is written to files in the /tmp/$USER/cf-release directory
    # whose locations will be output at the command prompt. Review the link checker output.

    # The set of broken links that is displayed by this check will differ from those in push
    # step 2 because the Checker Framework manual and website uses a mix of absolute and
    # relative links. Therefore, some links from the development site actually point to the
    # live site (the previous release). After step 5, these links point to the current
    # release and may be broken.

    print_step("Push Step 7. Check live site links")  # SEMIAUTO
    if not test_mode:
        if prompt_yes_no("Run link checker on LIVE site?", True):
            check_all_links(live_afu_website, live_checker_website, "live", test_mode)
    else:
        print("Test mode: Skipping checking of live site links.")

    # This step pushes the changes committed to the interm repositories to the GitHub
    # repositories. This is the first irreversible change. After this point, you can no longer
    # backout changes and should do another release in case of critical errors.

    print_step("Push Step 8. Push changes to repositories")  # SEMIAUTO
    # This step could be performed without asking for user input but I think we should err on the
    # side of caution.
    if not test_mode:
        if prompt_yes_no("Push the release to GitHub repositories?  This is irreversible.", True):
            push_interm_to_release_repos()
            print("Pushed to repos")
    else:
        print("Test mode: Skipping push to GitHub!")

    if test_mode:
        print("Test complete")
    else:
        # A prompt describes the email you should send to all relevant mailing lists.
        # Please fill out the email and announce the release.

        print_step(
            "Push Step 9. Post the Checker Framework and Annotation File Utilities releases on"
            " GitHub."
        )  # MANUAL

        msg = (
            "\n"
            "Download the following files to your local machine."
            "\n"
            f"  {LIVE_SITE_URL}/checker-framework-{new_cf_version}.zip\n"
            # NO-AFU
            f"  https://eisop.github.io/afu/annotation-tools-{new_cf_version}.zip\n"
            "\n"
            "To post the Checker Framework release on GitHub:\n"
            "\n"
            "* Browse to https://github.com/eisop/checker-framework/releases/new?tag=checker-framework-"
            + new_cf_version
            + "\n"
            + "* For the release title, enter: Checker Framework "
            + new_cf_version
            + "\n"
            + "* For the description, insert the latest Checker Framework changelog entry "
            + f"(available at {LIVE_SITE_URL}/CHANGELOG.md). Please include the first "
            "line with the release version and date.\n"
            + '* Find the link below "Attach binaries by dropping them here or selecting them." '
            + 'Click on "selecting them" and upload checker-framework-'
            + new_cf_version
            + ".zip from your machine.\n"
            + '* Click on the green "Publish release" button.\n'
            # NO-AFU: Until the Annotation File Utilities are part of the Checker Framework release.
            + "\n"
            + "To post the Annotation File Utilities release on GitHub:\n"
            + "\n"
            + "* Browse to https://github.com/eisop/annotation-tools/releases/new?tag="
            + new_cf_version
            + "\n"
            + "* For the release title, enter: Annotation File Utilities "
            + new_cf_version
            + "\n"
            + "* For the description, insert the latest Annotation File Utilities changelog entry "
            + "(available at https://eisop.github.io/afu/changelog.html). Please include the first "
            + "line with the release version and date. For bullet points, use the * Markdown "
            + "character.\n"
            + '* Find the link below "Attach binaries by dropping them here or selecting them." '
            + 'Click on "selecting them" and upload annotation-tools-'
            + new_cf_version
            + ".zip from your machine.\n"
            + '* Click on the green "Publish release" button.\n'
        )

        continue_or_exit(msg)

        print_step("Push Step 10. Announce the release.")  # MANUAL
        continue_or_exit(
            "Please announce the release using the email structure below.\n"
            + get_announcement_email(new_cf_version)
        )

        print_step("Push Step 11. Prep for next Checker Framework release.")  # MANUAL
        continue_or_exit(
            "Change releaseVersion in release.gradle to the version of the next release,\n"
            "for example by incrementing the -eisopN suffix.  Builds without -Prelease=true\n"
            "then produce -SNAPSHOT versions of that release.\n"
        )

        print_step("Push Step 12. Update the Checker Framework Gradle plugin.")  # MANUAL
        print("You might have to wait for Maven Central to propagate changes.\n")
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

    delete_if_exists(RELEASE_BUILD_COMPLETED_FLAG_FILE)

    print("Done with release_push.py.\n")


if __name__ == "__main__":
    main(sys.argv)
