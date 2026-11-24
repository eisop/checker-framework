#!/usr/bin/env python3
"""Release utilities.

Python Utils for releasing the Checker Framework
This contains no main method only utility functions
Created by Jonathan Burke 11/21/2012

Copyright (c) 2012 University of Washington
"""

from __future__ import annotations

import json
import pathlib
import re
import shlex
import shutil
import subprocess
import time
import urllib.request
from pathlib import Path

from release_errors import ReleaseError
from release_vars import execute, execute_output, execute_status

# =========================================================================================
# Parse Args Utils # TODO: Perhaps use argparse module


def has_command_line_option(argv: list[str], argument: str) -> bool:
    """Return True if the given command line arguments contain the specified argument.

    Returns:
        True if the given command line arguments contain the specified argument.
    """
    return any(argv[index] == argument for index in range(1, len(argv)))


# =========================================================================================
# Command utils


def execute_write_to_file(
    command_args: str,
    output_file_path: Path,
    halt_if_fail: bool = True,
    working_dir: Path | None = None,
) -> None:
    """Execute the given command, capturing the output to the given file.

    Raises:
        ReleaseError: If the command fails.
    """
    print(f"Executing: {command_args}")

    args = shlex.split(command_args) if isinstance(command_args, str) else command_args

    with Path.open(output_file_path, "w+") as output_file:
        process = subprocess.Popen(args, stdout=output_file, stderr=output_file, cwd=working_dir)
        process.communicate()
        process.wait()

    if process.returncode != 0 and halt_if_fail:
        msg = f"Error {process.returncode} while executing {command_args}"
        raise ReleaseError(msg)


def check_command(command: str) -> None:
    """Throw an exception if the command is not on the PATH."""
    p = execute_status(["which", command])
    if p:
        raise AssertionError("command not found: " + command)
    print()


def prompt_yes_no(msg: str, default: bool = False) -> bool:
    """Print the given message and continually prompt the user until they answer yes or no.

    Returns:
        true if the answer was yes, false otherwise.
    """
    default_str = "no"
    if default:
        default_str = "yes"

    result = prompt_w_default(msg, default_str, "^(Yes|yes|No|no)$")

    return result in {"yes", "Yes"}


def prompt_yn(msg: str) -> bool:
    """Print the given message and continually prompts the user until they answer y or n.

    Returns:
        true if the answer was y, false otherwise.
    """
    y_or_n = "z"
    while y_or_n not in {"y", "n"}:
        print(msg + " [y|n]")
        y_or_n = input().lower()

    return y_or_n == "y"


def prompt_w_default(msg: str, default: str, valid_regex: str | None = None) -> str:
    """Only accept answers that match valid_regex.  If default is None, require an answer.

    Returns:
        the user-provided input.
    """
    answer = None
    while answer is None:
        answer = input(msg + f" ({default}): ")

        if answer is None or answer == "":
            answer = default
        else:
            answer = answer.strip()

            if valid_regex is not None:
                m = re.match(valid_regex, answer)
                if m is None:
                    answer = None
                    print("Invalid answer.  Validating regex: " + valid_regex)
            else:
                answer = default

    return answer


def check_tools(tools: list[str]) -> None:
    """Verify that each tools is on the PATH."""
    print("\nChecking to make sure the following programs are installed:")
    print(", ".join(tools))
    list(map(check_command, tools))
    print()


def continue_or_exit(msg: str) -> None:
    """Prompts the user whether to continue executing the script."""
    continue_script = prompt_w_default(
        msg + " Continue ('no' will exit the script)?", "yes", "^(Yes|yes|No|no)$"
    )
    if continue_script == "no" or continue_script == "No":
        raise ReleaseError("User elected NOT to continue at prompt: " + msg)


# =========================================================================================
# Version Utils


# From https://stackoverflow.com/a/1714190/173852, but doesn't strip trailing zeroes
def version_number_to_array(version_num: str) -> list[int]:
    """Given a version number, return an array of the elements, as integers.

    An EISOP version number ends in "-eisopN", such as "1.2.3-eisop1".  The array has five
    elements: up to four numeric parts, padded with zeroes, followed by the "-eisopN" number, or 0
    if there is none.  Arrays for different versions therefore compare in release order.

    Returns:
        an array of the version number elements.

    Raises:
        ReleaseError: If the argument is not a version number.
    """
    match = re.fullmatch(r"(\d+(?:\.\d+){0,3})(?:-eisop(\d+))?", version_num)
    if match is None:
        msg = f"Not a version number: {version_num}"
        raise ReleaseError(msg)
    numbers = [int(x) for x in match.group(1).split(".")]
    numbers += [0] * (4 - len(numbers))
    return [*numbers, int(match.group(2) or 0)]


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


def current_release_version() -> str:
    """Return the version of the latest release of the Checker Framework on GitHub.

    The web site is generated from the GitHub releases, so this is also the version it shows.

    Returns:
        the version of the latest release, such as 3.49.5-eisop1

    Raises:
        ReleaseError: if the latest release's tag is not checker-framework-VERSION
    """
    url = "https://api.github.com/repos/eisop/checker-framework/releases/latest"
    print(f"Looking up the latest release at {url}\n")
    with urllib.request.urlopen(url=url) as response:
        tag = json.load(response)["tag_name"]
    prefix = "checker-framework-"
    if not tag.startswith(prefix):
        msg = f"The latest release's tag is not {prefix}VERSION: {tag}"
        raise ReleaseError(msg)
    return tag.removeprefix(prefix)


# =========================================================================================
# Git Utils


def git_repo_exists_at_path(repo_root: Path) -> bool:
    """Return true if a git repository exists at the given filesystem path.

    Returns:
        true if a git repository exists at the given filesystem path.
    """
    return (repo_root / ".git").is_dir()


def commit_and_tag(version: str, path: Path, tag: str) -> None:
    """Commit the changes made for this release and add a tag.

    release_push.py pushes the commit and the tag; see push_release.
    """
    # Remove the pre-commit hook because it can cause errors.
    execute("rm -f .git/hooks/pre-commit", working_dir=path)

    # Do nothing (instead of erring) if there is nothing to commit.
    if execute_status("git diff-index --quiet HEAD", working_dir=path) != 0:
        execute(f'git commit -a -m "new release {version}"', working_dir=path)
    execute(f"git tag {tag}", working_dir=path)


def push_release(repo_root: Path, tag: str) -> None:
    """Push the release commit on master and its tag to GitHub.

    The push is atomic: either both are pushed or neither is.  In case of failure, ask the user
    whether to try again.

    Raises:
        ReleaseError: If the push fails and the user chooses not to try again.
    """
    cmd = ["git", "push", "--atomic", "origin", "master", tag]
    while True:
        result = subprocess.run(cmd, cwd=repo_root, check=False)
        if result.returncode == 0:
            return
        print(f"Could not push from {repo_root}; result={result.returncode} for {' '.join(cmd)}")
        if not prompt_yn("Try again (responding 'n' will exit the script)?"):
            raise ReleaseError("Could not push the release.")


def execute_with_retries(command: list[str], working_dir: Path | None = None) -> None:
    """Execute the given command, which downloads something, trying it up to three times.

    Raises:
        ReleaseError: If the command fails three times.
    """
    for attempt in range(1, 4):
        if execute_status(command, working_dir) == 0:
            return
        if attempt < 3:
            print("Trying again in 1 minute, in case the failure was a network problem.")
            time.sleep(60)
    msg = f"{' '.join(command)} failed 3 times."
    raise ReleaseError(msg)


def clone_or_reset(src_repo: str, dst_repo_dir: Path, shallow: bool = False) -> None:
    """Make dst_repo_dir a clone of the master branch of src_repo, as if it had just been cloned.

    An existing clone is reset, which discards whatever an earlier run of the release scripts
    left in it, such as an unpushed release commit and tag, modified files, and build outputs.  If
    there is no clone, or the reset fails, the repository is cloned from scratch.

    Args:
        src_repo: the URL of the repository
        dst_repo_dir: the directory of the clone
        shallow: whether to fetch only the latest commit of the master branch
    """
    if git_repo_exists_at_path(dst_repo_dir):
        # A shallow clone fetches only master; a full clone also fetches the tags, and
        # --prune-tags deletes the local tags that are not on GitHub, such as an unpushed release
        # tag.
        fetch = ["--depth", "1"] if shallow else ["--prune", "--prune-tags"]
        commands = [
            ["git", "fetch", "--quiet", *fetch, "origin"],
            ["git", "checkout", "--quiet", "--force", "master"],
            ["git", "reset", "--quiet", "--hard", "origin/master"],
            ["git", "clean", "--quiet", "-ffdx"],
        ]
        if all(execute_status(command, dst_repo_dir) == 0 for command in commands):
            return
        print(f"Could not reset {dst_repo_dir}, so it is cloned from scratch.")
    delete_directory_if_exists(dst_repo_dir)
    depth = ["--depth", "1"] if shallow else []
    execute_with_retries(["git", "clone", "--quiet", *depth, src_repo, str(dst_repo_dir)])


def is_repo_cleaned_and_updated(repo_dir: Path) -> bool:
    """Return true if the repository at the given filesystem path is clean and up-to-date.

    Clean means there are no committed changes and no untracked files in the working tree.

    IMPORTANT: this function is not known to be fully reliable in ensuring
    that a repo is fully clean of all changes, such as committed tags. To be
    certain of success throughout the release_build and release_push process,
    the best option is to clone repositories from scratch.

    Returns:
        true if the repository at the given filesystem path is clean and up-to-date.
    """
    # The idiom "not execute_output(...)" evaluates to True when the captured output is empty.
    # Could add "--untracked-files=no" to this command
    is_clean = not execute_output("git status --porcelain", working_dir=repo_dir)
    execute("git fetch origin", working_dir=repo_dir)
    is_updated = not execute_output("git diff origin/master..master", working_dir=repo_dir)
    return is_clean and is_updated


def check_repo(repo: Path) -> None:
    """Fail if the repository is not clean and up to date.

    Raises:
        ReleaseError: If the repository is not clean and up to date.
    """
    if not git_repo_exists_at_path(repo):
        msg = f"There is no repository at {repo}.  Delete it and run release_build.py again."
        raise ReleaseError(msg)
    if not is_repo_cleaned_and_updated(repo):
        msg = f"repo {repo} is not cleaned and updated!  Delete it and run release_build.py again."
        raise ReleaseError(msg)


# =========================================================================================
# File Utils


def download_binary(source_url: str, destination: Path) -> None:
    """Download a file from the given URL and save its contents to the destination filename."""
    http_response = urllib.request.urlopen(url=source_url)
    content_length = http_response.headers["content-length"]

    if content_length is None:
        raise ReleaseError("No content-length when downloading: " + source_url)

    with Path.open(destination, "wb") as dest_file:
        dest_file.write(http_response.read())


def ensure_writeable(path: Path) -> None:
    """Permit user and group to read/write everything under the specified path."""
    # Errs for any file not owned by this user, so ignore the exit status.
    # But, the point is to set group writeability of any *new* files.
    execute_status(f"chmod -f -R ug+rw {path}")


def delete(file_to_delete: Path) -> None:
    """Delete the specified file."""
    pathlib.Path(file_to_delete).unlink()


def delete_if_exists(file_to_delete: Path) -> None:
    """Check if the specified file exists, and if so, delete it."""
    if pathlib.Path(file_to_delete).exists():
        delete(file_to_delete)


def delete_directory(path: Path) -> None:
    """Delete all files and directories under the specified path."""
    ensure_writeable(path)
    shutil.rmtree(path)


def delete_directory_if_exists(path: Path) -> None:
    """Delete all files and directories under the specified path, if it exists."""
    if pathlib.Path(path).exists():
        delete_directory(path)


def create_empty_file(file_path: Path) -> None:
    """Create an empty file with the given filename."""
    dest_file = Path.open(file_path, "wb")
    dest_file.close()


# =========================================================================================
# Misc. Utils


def print_step(step: str) -> None:
    """Print a step in the release_build or release_push script."""
    print("\n")
    print(step)

    dash_str = ""
    for _dummy in range(len(step)):
        dash_str += "-"
    print(dash_str)


def get_announcement_email(version: str) -> str:
    """Return the template for the e-mail announcing a new release of the Checker Framework.

    Returns:
        the template for the e-mail announcing a new release of the Checker Framework.
    """
    # These are variables only to keep the source lines short.
    description = (
        "The Checker Framework lets you create and/or run pluggable type checkers,"
        " in order to detect and prevent bugs in your code."
    )
    changelog = "https://github.com/eisop/checker-framework/blob/master/docs/CHANGELOG.md"
    return f"""
To:  checker-framework-discuss@googlegroups.com
Subject: Release {version} of the Checker Framework

We have released a new version of the Checker Framework.
{description}

You can find documentation and download links at:
https://eisop.github.io/

Changes for Checker Framework version {version}:

<<Insert latest Checker Framework changelog entry from {changelog}, preserving its formatting.>>
"""


# =========================================================================================
# Testing


def test_version_number_to_array() -> None:
    """Run test cases to ensure that version_number_to_array orders versions correctly."""
    assert version_number_to_array("1.2.3") == [1, 2, 3, 0, 0]
    assert version_number_to_array("1.2.3.1") == [1, 2, 3, 1, 0]
    assert version_number_to_array("1.2.3-eisop1") == [1, 2, 3, 0, 1]
    assert version_number_to_array("1.2.3-eisop1") < version_number_to_array("1.2.3-eisop2")
    assert version_number_to_array("1.2.3-eisop2") < version_number_to_array("1.3.0-eisop1")
    assert version_number_to_array("1.2.3-eisop10") > version_number_to_array("1.2.3-eisop9")


def test_release_utils() -> None:
    """Test that critical methods in this file work as expected."""
    test_version_number_to_array()


# Tests run every time this file is loaded
test_release_utils()
