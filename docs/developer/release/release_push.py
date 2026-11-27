#!/usr/bin/env python3
"""Push the release."""

# See README-release-process.html for more information

import filecmp
import re
import subprocess
import sys
import tempfile
import time
import urllib.request
from pathlib import Path

import maven_central
import release_utils
from maven_central import (
    DEPLOYMENTS_URL,
    on_maven_central,
    publish_deployment,
    saved_deployment,
    upload_to_maven_central,
    verify_local_artifacts,
    wait_until_published,
)
from release_errors import ReleaseError
from release_utils import (
    check_release_dir,
    check_release_version,
    continue_or_exit,
    current_release_version,
    delete_if_exists,
    has_command_line_option,
    print_step,
    prompt_w_default,
    prompt_yes_no,
    push_release,
    version_number_to_array,
)
from release_vars import (
    CF_VERSION,
    CHANGELOG,
    CHECKER_FRAMEWORK,
    GITHUB_REPO,
    RELEASE_BUILD_COMPLETED_FLAG_FILE,
    RELEASE_FILES_DIR,
    TMP_DIR,
    execute,
    execute_output,
    execute_status,
    gradle_release,
)
from sanity_checks import (
    javac_sanity_check,
    maven_sanity_check,
)
from website_check import GENERATED_SITE, check_website


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
        with urllib.request.urlopen(url, timeout=300) as response:
            destination.write_bytes(response.read())
    except OSError as e:
        return f"Could not download {url}: {e}"
    return None


def github_release_files_problem(cf_version: str) -> str | None:
    """Compare the files attached to the GitHub release with the ones that were built.

    Returns:
        why the files attached to the GitHub release are not the built ones, or None if they are.
    """
    download_url = (
        f"https://github.com/{GITHUB_REPO}/releases/download/checker-framework-{cf_version}"
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


def changelog_section(cf_version: str) -> str:
    """Return the section of the changelog for the given version, including its heading line.

    Returns:
        the section of docs/CHANGELOG.md for the given version
    """
    changelog = CHANGELOG.read_text(encoding="utf-8")
    start = changelog.index(f"## Version {cf_version} (")
    end = changelog.find("\n## Version ", start)
    return changelog[start:] if end == -1 else changelog[start : end + 1]


def announcement_email(cf_version: str) -> str:
    """Return the e-mail that announces the given release, with its changelog section.

    Returns:
        the e-mail, with its recipient and subject
    """
    return (
        "To: checker-framework-discuss@googlegroups.com\n"
        f"Subject: Release {cf_version} of the Checker Framework\n"
        "\n"
        "We have released a new version of the Checker Framework.  The Checker Framework lets you"
        " create and/or run pluggable type checkers, in order to detect and prevent bugs in your"
        " code.\n"
        "\n"
        "You can find documentation and download links at:\n"
        "https://eisop.github.io/\n"
        "\n"
        f"Changes for Checker Framework version {cf_version}:\n"
        "\n" + changelog_section(cf_version)
    )


def create_github_release(tag: str, cf_version: str) -> bool:
    """Create the GitHub release for the given tag with gh, with both release files attached.

    Its description is the changelog section of the release.

    Returns:
        true if gh created the release
    """
    with tempfile.TemporaryDirectory() as notes_dir:
        notes = Path(notes_dir) / "notes.md"
        notes.write_text(changelog_section(cf_version), encoding="utf-8")
        command = [
            "gh",
            "release",
            "create",
            tag,
            "--repo",
            GITHUB_REPO,
            # Fail, rather than create the tag on the head of master, if the tag is not on GitHub.
            "--verify-tag",
            "--title",
            f"Checker Framework {cf_version}",
            "--notes-file",
            str(notes),
            *(str(RELEASE_FILES_DIR / name) for name in release_file_names(cf_version)),
        ]
        return execute_status(command) == 0


def tests_passed_in_ci() -> bool:
    """Return true if CI passed on the commit that the release commit is based on.

    The release commit then needs no tests of its own if it changes only files in docs/, such as
    the version numbers and the changelog date.

    Returns:
        true if CI passed on the parent of the release commit, and the release commit changes only
        files in docs/
    """
    changed = execute_output(["git", "diff", "--name-only", "HEAD~1", "HEAD"], CHECKER_FRAMEWORK)
    not_docs = [f for f in changed.split() if not f.startswith("docs/")]
    if not_docs:
        print(f"The release commit changes files outside docs/: {not_docs}")
        return False
    parent = execute_output(["git", "rev-parse", "HEAD~1"], CHECKER_FRAMEWORK).strip()
    conclusions = execute_output(
        [
            "gh",
            "run",
            "list",
            "--repo",
            GITHUB_REPO,
            "--workflow",
            "ci.yml",
            "--commit",
            parent,
            "--json",
            "conclusion",
            "--jq",
            ".[].conclusion",
        ]
    ).split()
    if "success" not in conclusions:
        print(f"CI did not pass on {parent}: {conclusions or 'no CI run'}")
        return False
    print(f"CI passed on {parent}, and the release commit changes only files in docs/.")
    return True


def next_release_version(version: str) -> str:
    """Return the likely version of the release after the given one: its -eisopN suffix incremented.

    A version without the suffix, a typetools version, is followed by its -eisop1.

    Returns:
        the version of the next release
    """
    match = re.fullmatch(r"(.*-eisop)(\d+)", version)
    return f"{match.group(1)}{int(match.group(2)) + 1}" if match else f"{version}-eisop1"


def open_next_release_pr(released_version: str, next_version: str) -> bool:
    """Open a pull request that prepares the next release.

    It sets releaseVersion in release.gradle to the next version and adds the next version's section
    to the changelog.  release_build.py sets the section's date when the version is released.

    Returns:
        true if the pull request was opened
    """
    release_gradle = CHECKER_FRAMEWORK / "release.gradle"
    gradle_text = release_gradle.read_text(encoding="utf-8")
    old_setting = f'releaseVersion = "{released_version}"'
    changelog_text = CHANGELOG.read_text(encoding="utf-8")
    first_section = changelog_text.find("\n## Version ")
    if old_setting not in gradle_text or first_section == -1:
        print(f"Could not find {old_setting} in {release_gradle}, or a section in {CHANGELOG}.")
        return False

    branch = f"prepare-{next_version}"
    title = f"Prepare {next_version}"
    if execute_status(["git", "switch", "--quiet", "-c", branch], CHECKER_FRAMEWORK) != 0:
        return False
    try:
        release_gradle.write_text(
            gradle_text.replace(old_setting, f'releaseVersion = "{next_version}"'),
            encoding="utf-8",
        )
        section = (
            f"\n## Version {next_version} (TBD)\n\n### User-visible changes\n\n"
            "### Implementation details\n\n### Closed issues\n"
        )
        CHANGELOG.write_text(
            changelog_text[:first_section] + section + changelog_text[first_section:],
            encoding="utf-8",
        )
        body = (
            f"Sets releaseVersion to {next_version} and adds its changelog section, after the"
            f" release of {released_version}.  Made by release_push.py."
        )
        commands = [
            ["git", "commit", "--quiet", "-a", "-m", title],
            ["git", "push", "--quiet", "origin", branch],
            [
                "gh",
                "pr",
                "create",
                "--repo",
                GITHUB_REPO,
                "--base",
                "master",
                "--head",
                branch,
                "--title",
                title,
                "--body",
                body,
            ],
        ]
        return all(execute_status(command, CHECKER_FRAMEWORK) == 0 for command in commands)
    finally:
        # Leave the clone at the release commit.
        execute_status(["git", "switch", "--quiet", "--force", "master"], CHECKER_FRAMEWORK)


WEBSITE = "https://eisop.github.io/cf/"


def tag_on_github(tag: str) -> str | None:
    """Return the commit that the given tag names on GitHub.

    Returns:
        the commit, or None if the tag is not on GitHub
    """
    lines = execute_output(
        ["git", "ls-remote", "--tags", "origin", f"refs/tags/{tag}", f"refs/tags/{tag}^{{}}"],
        CHECKER_FRAMEWORK,
    ).split("\n")
    commits = {ref: sha for sha, _, ref in (line.partition("\t") for line in lines) if ref}
    return commits.get(f"refs/tags/{tag}^{{}}") or commits.get(f"refs/tags/{tag}")


def check_release_commit(tag: str, pushed: bool) -> None:
    """Check that the build clone is at the release commit, and that it can be pushed.

    Raises:
        ReleaseError: If the clone is not at the release commit, if the tag on GitHub names
            another commit, or if master on GitHub has moved since release_build.py, so that the
            release commit cannot be pushed.
    """
    head = execute_output(["git", "rev-parse", "HEAD"], CHECKER_FRAMEWORK).strip()
    # git rev-parse fails if the tag does not exist.
    local_tag = execute_output(
        ["git", "rev-parse", f"{tag}^{{commit}}"], CHECKER_FRAMEWORK, check=False
    ).strip()
    if local_tag != head:
        msg = f"{CHECKER_FRAMEWORK} is not at {tag}; run release_build.py."
        raise ReleaseError(msg)
    if pushed:
        if tag_on_github(tag) != head:
            msg = f"{tag} on GitHub is not the release commit {head} in {CHECKER_FRAMEWORK}."
            raise ReleaseError(msg)
        return
    execute(["git", "fetch", "--quiet", "origin", "master"], CHECKER_FRAMEWORK)
    parent = execute_output(["git", "rev-parse", "HEAD~1"], CHECKER_FRAMEWORK).strip()
    master = execute_output(["git", "rev-parse", "origin/master"], CHECKER_FRAMEWORK).strip()
    if master != parent:
        msg = (
            "master on GitHub has moved since release_build.py, so the release commit cannot be"
            " pushed.  Run release_build.py again, which builds the release from the new master."
        )
        raise ReleaseError(msg)


def github_release_exists(tag: str) -> bool:
    """Return true if the GitHub release of the given tag exists, as a draft or published.

    Returns:
        true if the GitHub release exists
    """
    view = ["gh", "release", "view", tag, "--repo", GITHUB_REPO, "--json", "tagName"]
    return subprocess.run(view, capture_output=True, check=False).returncode == 0


def website_shows(version: str) -> bool:
    """Return true if the web site shows the given version.

    Returns:
        true if the front page of the web site names the given version
    """
    try:
        with urllib.request.urlopen(WEBSITE, timeout=60) as response:
            return version in response.read().decode(errors="replace")
    except OSError:
        return False


def wait_for_website(version: str) -> bool:
    """Wait, for up to half an hour, until the web site shows the given version.

    Returns:
        true if the web site shows the version
    """
    print(f"Waiting until {WEBSITE} shows {version}.")
    deadline = time.monotonic() + 30 * 60
    while not website_shows(version):
        if time.monotonic() > deadline:
            return False
        time.sleep(60)
    return True


def validate_args(argv: list[str]) -> None:
    """Validate the command-line arguments.

    Raises:
        ReleaseError: If the command-line arguments are not valid.
    """
    if len(argv) > 3:
        print_usage()
        raise ReleaseError("Invalid arguments. " + ",".join(argv))
    for i in range(1, len(argv)):
        if argv[i] not in {"release", "--dry-run"}:
            print_usage()
            raise ReleaseError("Invalid arguments. " + ",".join(argv))


def print_usage() -> None:
    """Print instructions on how to use this script, including how to set test or release mode."""
    print(
        "Usage: python3 release_push.py [release | --dry-run]\n"
        'If the "release" argument is '
        "NOT specified then the script will execute all steps that checking and prompting "
        "steps but will NOT actually perform a release.  This is for testing the script.\n"
        "--dry-run is a test run that answers every question with its default, and skips the"
        " checks that need gh and the tests; CI runs it after release_build.py --dry-run."
    )


def main(argv: list[str]) -> None:
    """Test the release, push it to GitHub, and guide the user through the manual steps.

    It asks before each public or irreversible step: the push, the GitHub release, from which the
    web site is generated, the publish on Maven Central, the web site, and the pull request that
    prepares the next release.  The announcement e-mail is sent by hand.
    """
    # MANUAL Indicates a manual step
    # AUTO Indicates the step is fully automated.

    check_release_dir(TMP_DIR)
    current_cf_version = current_release_version()
    new_cf_version = CF_VERSION
    dry_run = has_command_line_option(argv, "--dry-run")
    test_mode = dry_run or not has_command_line_option(argv, "release")
    validate_args(argv)
    if dry_run:
        release_utils.answer_defaults = True
        maven_central.verify_with_local_keyring = True

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
            "been run.",
            False,
        )

    # A rerun continues a release that an earlier run left unfinished: each step that changes
    # something public first checks whether it is done already.
    tag = f"checker-framework-{new_cf_version}"
    pushed = tag_on_github(tag) is not None
    if pushed:
        print(f"{tag} is on GitHub already; the steps that are done are skipped.")

    print_step("Push Step 1: Check the release versions and that master has not moved")  # AUTO
    # Steps 8 and 10 use gh.
    if not dry_run and execute_status(["gh", "auth", "status", "--hostname", "github.com"]) != 0:
        raise ReleaseError("gh is not logged in; run gh auth login.")
    if not (pushed and current_cf_version == new_cf_version):
        try:
            check_release_version(current_cf_version, new_cf_version)
        except ReleaseError as e:
            if not dry_run:
                raise
            print(f"Dry run: {e}")
    print(f"Latest release: {current_cf_version}    this release: {new_cf_version}")
    check_release_commit(tag, pushed)

    # Runs sanity tests on the release files that release_build.py built.  release_build.py
    # already checked the links of the pages in the Checker Framework zip file.

    print_step("Push Step 2: Run sanity tests")  # SEMIAUTO
    if prompt_yes_no("Perform this step?", not pushed):
        print_step("2a: Run javac sanity test on the release zip file.")
        if prompt_yes_no("Run javac sanity test on the release zip file?", True):
            javac_sanity_check(
                RELEASE_FILES_DIR / f"checker-framework-{new_cf_version}.zip", new_cf_version
            )

        print_step("2b: Run Maven sanity test on the local Maven repository.")
        if prompt_yes_no("Run Maven sanity test on the local Maven repository?", True):
            maven_sanity_check("maven-dev")

    # Runs all tests on the release build, with -Prelease=true, which leaves the release's jars
    # as they are.

    print_step("Push Step 3: Run all tests (takes a long time)")
    if not dry_run and prompt_yes_no(
        "Run all tests?  With few cores, this takes hours.", not (pushed or tests_passed_in_ci())
    ):
        execute(gradle_release("allTests"), CHECKER_FRAMEWORK)

    # The web site is generated from the GitHub release.  Running the generator on the release zip
    # file shows problems with the generated pages before anything is published: every internal
    # link of the generated pages must work.

    release_zip = RELEASE_FILES_DIR / f"checker-framework-{new_cf_version}.zip"
    print_step("Push Step 4: Check the web site that will be generated")  # AUTO
    website_problems = check_website(release_zip)
    for problem in website_problems:
        print("  " + problem)
    if website_problems:
        continue_or_exit(
            f"The web site generated from {release_zip.name} has the problems above.  It is in"
            f" {GENERATED_SITE}; serve that directory with python3 -m http.server to look at it.\n",
            False,
        )
    else:
        print(f"The links of the web site generated in {GENERATED_SITE} work.")

    # Maven Central validates the release before anything is public: a deployment that is not
    # published can be dropped.  Everything after this step is public or irreversible.

    print_step("Push Step 5: Upload the Maven artifacts to Maven Central, for validation")
    deployment_id, deployment_state = saved_deployment(new_cf_version)
    central_done = deployment_state in {"PUBLISHING", "PUBLISHED"} or on_maven_central(
        new_cf_version
    )
    if test_mode:
        problems = verify_local_artifacts(new_cf_version)
        for problem in problems:
            print("  " + problem)
        if problems:
            raise ReleaseError("The artifacts are not ready for Maven Central.")
        print("Test mode: the artifacts are ready for Maven Central; nothing is uploaded.")
        print("Test complete")
        return
    if central_done:
        print(f"{new_cf_version} is on Maven Central already.")
    elif deployment_state == "VALIDATED":
        print(f"Deployment {deployment_id}, uploaded earlier, is validated.")
    else:
        deployment_id = None
        if prompt_yes_no(
            "Upload the Maven artifacts from the local Maven repository to Maven Central, for"
            " validation?  They are not published until you confirm again.",
            True,
        ):
            deployment_id = upload_to_maven_central(new_cf_version)
        if deployment_id is None:
            raise ReleaseError(
                "The Maven artifacts are not validated, so nothing is pushed or published.  Fix"
                " the problem and run release_push.py again, or publish by hand; see the release"
                " process README."
            )

    print_step("Push Step 6. Push the release to GitHub")  # SEMIAUTO
    if pushed:
        print(f"{tag} is on GitHub already.")
    else:
        if not prompt_yes_no(f"Push master and {tag} to GitHub?  This is irreversible.", False):
            # Posting a GitHub release for a tag that is not on GitHub would create the tag on
            # the head of master, which is not the release commit.
            raise ReleaseError("The release was not pushed.  Run release_push.py again to push it.")
        push_release(CHECKER_FRAMEWORK, tag)
        print("Pushed to GitHub")

    print_step("Push Step 7. Publish the Maven artifacts on Maven Central.")  # SEMIAUTO
    # Before the GitHub release, from which the web site is generated, so that the web site does
    # not offer a version that is not on Maven Central.
    if not central_done and not (
        deployment_id is not None
        and prompt_yes_no(
            f"Publish {new_cf_version} on Maven Central?  This is irreversible.", False
        )
        and publish_deployment(deployment_id)
    ):
        continue_or_exit(
            f"Publish deployment {deployment_id} by hand at {DEPLOYMENTS_URL}, or run"
            " release_push.py again.\n",
            False,
        )

    print_step(
        "Push Step 8. Post the Checker Framework release, with the Annotation File Utilities,"
        " on GitHub."
    )  # SEMIAUTO
    # The web site is generated from this release.  If you decline, or gh fails, the release is
    # posted, or fixed, by hand.
    if github_release_exists(tag):
        print(f"The GitHub release {tag} exists already.")
    elif not (
        prompt_yes_no(
            f"Create the GitHub release for {tag} with gh, with the changelog section as its"
            " description and both zip files attached?  It is public, and the web site shows it"
            " by its next daily update.",
            False,
        )
        and create_github_release(tag, new_cf_version)
    ):
        release_files = "".join(
            f"  {RELEASE_FILES_DIR / name}\n" for name in release_file_names(new_cf_version)
        )
        msg = (
            "\n"
            "The web site is generated from this GitHub release.  Its files are in "
            f"{RELEASE_FILES_DIR}; if this script runs on another machine, copy them to"
            " yours:\n" + release_files + "\n"
            "To post the Checker Framework release on GitHub:\n"
            "\n"
            f"* Browse to https://github.com/{GITHUB_REPO}/releases/new?tag={tag}, or, if a"
            f" draft exists, edit it: gh release view {tag} --repo {GITHUB_REPO}\n"
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
        continue_or_exit("Create the release, or fix it, by hand." + msg)

    print_step("Push Step 9. Check the files attached to the GitHub release.")  # AUTO
    check_github_release_files(new_cf_version)

    print_step("Push Step 10. Publish the web site.")  # SEMIAUTO
    # The web site is also published daily at 20:00 UTC.
    publish_website = [
        "gh",
        "workflow",
        "run",
        "publish-website.yml",
        "--repo",
        "eisop/eisop.github.io",
        "--field",
        "dry_run=false",
    ]
    if website_shows(new_cf_version):
        print(f"{WEBSITE} shows {new_cf_version} already.")
    elif (
        prompt_yes_no(
            f'Publish the web site for {new_cf_version} now, with the "Publish website" workflow?',
            True,
        )
        and execute_status(publish_website) == 0
        and wait_for_website(new_cf_version)
    ):
        print(f"{WEBSITE} shows {new_cf_version}.")
    else:
        continue_or_exit(
            f"{WEBSITE} does not show {new_cf_version} yet.  Run the workflow with dry_run"
            " unchecked, and check the site:\n"
            "  https://github.com/eisop/eisop.github.io/actions/workflows/publish-website.yml\n"
        )

    print_step("Push Step 11. Announce the release.")  # MANUAL
    # Announce only what users can download.
    if not on_maven_central(new_cf_version):
        print(f"Waiting until Maven Central has published {new_cf_version}.")
        if deployment_id is None or not wait_until_published(deployment_id):
            continue_or_exit(f"Check that Maven Central has published {new_cf_version}.", False)
    email = TMP_DIR / f"announcement-{new_cf_version}.txt"
    email.write_text(announcement_email(new_cf_version), encoding="utf-8")
    continue_or_exit(
        f"Send the announcement e-mail in {email}; its first lines are the recipient and the"
        " subject.\n"
    )

    print_step("Push Step 12. Prepare the next Checker Framework release.")  # SEMIAUTO
    # Until releaseVersion is the next version, snapshots have the version just released.
    # Renovate updates the version in docs/examples once the release is on Maven Central.
    next_version = new_cf_version
    while version_number_to_array(next_version) <= version_number_to_array(new_cf_version):
        next_version = prompt_w_default(
            f"Version of the next release, after {new_cf_version}",
            next_release_version(new_cf_version),
            r"^\d+\.\d+\.\d+(-eisop\d+)?$",
        )
    if not (
        prompt_yes_no(
            f"Open a pull request that sets releaseVersion to {next_version} and adds its"
            " changelog section?",
            True,
        )
        and open_next_release_pr(new_cf_version, next_version)
    ):
        continue_or_exit(
            f"Make a pull request that changes releaseVersion in release.gradle to"
            f" {next_version}, and adds its section to docs/CHANGELOG.md.\n"
        )

    delete_if_exists(RELEASE_BUILD_COMPLETED_FLAG_FILE)
    print("Done with release_push.py.\n")


if __name__ == "__main__":
    try:
        main(sys.argv)
    except ReleaseError as e:
        sys.exit(f"\nrelease_push.py stopped: {e}")
