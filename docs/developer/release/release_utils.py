#!/usr/bin/env python3
"""Release utilities.

Python Utils for releasing the Checker Framework
This contains no main method only utility functions
Created by Jonathan Burke 11/21/2012

Copyright (c) 2012 University of Washington
"""

from __future__ import annotations

import grp
import json
import os
import pwd
import re
import shutil
import stat
import subprocess
import time
import urllib.request
from typing import TYPE_CHECKING

from release_errors import ReleaseError
from release_vars import GITHUB_REPO, execute, execute_output, execute_status

if TYPE_CHECKING:
    from pathlib import Path

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


# Set by --dry-run: every prompt takes its default answer, so that the scripts run unattended.
answer_defaults: bool = False


def prompt_w_default(msg: str, default: str, valid_regex: str | None = None) -> str:
    """Only accept answers that match valid_regex.  If default is None, require an answer.

    Returns:
        the user-provided input.
    """
    if answer_defaults:
        print(f"{msg} ({default}): {default}")
        return default
    answer = None
    while answer is None:
        answer = input(msg + f" ({default}): ")

        if answer is None or answer == "":
            answer = default
        else:
            answer = answer.strip()

            if valid_regex is not None and re.match(valid_regex, answer) is None:
                answer = None
                print("Invalid answer.  Validating regex: " + valid_regex)

    return answer


def check_tools(tools: list[str]) -> None:
    """Verify that each tool is on the PATH.

    Raises:
        ReleaseError: If a tool is not on the PATH.
    """
    missing = [tool for tool in tools if shutil.which(tool) is None]
    if missing:
        msg = "These programs are not installed, or not on the PATH: " + ", ".join(missing)
        raise ReleaseError(msg)


def continue_or_exit(msg: str) -> None:
    """Prompts the user whether to continue executing the script."""
    if not prompt_yes_no(msg + " Continue ('no' will exit the script)?", True):
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
    url = f"https://api.github.com/repos/{GITHUB_REPO}/releases/latest"
    print(f"Looking up the latest release at {url}\n")
    request = urllib.request.Request(url)
    # GitHub limits requests without a token, which CI has.
    if os.environ.get("GITHUB_TOKEN"):
        request.add_unredirected_header("Authorization", "Bearer " + os.environ["GITHUB_TOKEN"])
    with urllib.request.urlopen(request, timeout=60) as response:
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


def commit_and_tag(
    version: str, path: Path, tag: str, message: str, signing_key: str | None
) -> None:
    """Commit the changes made for this release and add an annotated tag with the given message.

    The tag is signed with the given key, if there is one.  release_push.py pushes the commit and
    the tag; see push_release.
    """
    # Remove the pre-commit hook because it can cause errors.
    execute("rm -f .git/hooks/pre-commit", working_dir=path)

    # Do nothing (instead of erring) if there is nothing to commit.  git status, unlike git
    # diff-index, ignores a file that was rewritten with the same contents.
    if execute_output(["git", "status", "--porcelain", "--untracked-files=no"], path):
        execute(f'git commit -a -m "new release {version}"', working_dir=path)
    sign = ["--local-user", signing_key] if signing_key else []
    execute(["git", "tag", "--annotate", *sign, "--message", message, tag], working_dir=path)


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
        if not prompt_yes_no("Try again ('no' will exit the script)?", True):
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


def check_release_dir(release_dir: Path) -> None:
    """Check that no other user can change the release directory.

    The release is built and signed in this directory.  It, and each directory that contains it,
    must belong to you or root, and must not be writable by other users, unless it is a sticky
    directory such as /tmp, in which they cannot rename or delete your files.  A directory that
    only its group can write is fine if that is your own group, with no other members, as with
    umask 002.

    Raises:
        ReleaseError: If another user can change the release directory.
    """
    user = pwd.getpwuid(os.geteuid()).pw_name
    for path in [release_dir, *release_dir.parents]:
        if not path.exists():
            continue
        status = path.stat()
        if status.st_uid not in (os.geteuid(), 0):
            msg = f"{path} belongs to another user, who could change the release built in it."
            raise ReleaseError(msg)
        own_group = status.st_gid == os.getegid() and set(grp.getgrgid(status.st_gid).gr_mem) <= {
            user
        }
        writable_by_others = status.st_mode & 0o002 or (status.st_mode & 0o020 and not own_group)
        if writable_by_others and not status.st_mode & stat.S_ISVTX:
            msg = f"Other users can write {path}, so they could change the release built in it."
            raise ReleaseError(msg)


def discardable_work(repo_dir: Path) -> list[str]:
    """Return the work in the given directory that resetting it to a fresh clone would discard.

    That is modified and untracked files, stashes, and commits on no remote branch, other than a
    release commit of an earlier run of the release scripts.  Ignored files, such as build outputs,
    are not work.  A directory that is not a git repository is all work, unless it is empty.

    Returns:
        a description of each piece of work, or an empty list if there is none
    """
    if not any(repo_dir.iterdir()):
        return []
    status = subprocess.run(
        ["git", "status", "--porcelain"], cwd=repo_dir, capture_output=True, check=False
    )
    if not git_repo_exists_at_path(repo_dir) or status.returncode != 0:
        return [f"all of {repo_dir}, which is not a usable git repository"]
    work = [f"file {line}" for line in status.stdout.decode().splitlines()]
    work += [
        f"stash {line}" for line in execute_output(["git", "stash", "list"], repo_dir).splitlines()
    ]
    commits = execute_output(
        ["git", "log", "--branches", "--not", "--remotes", "--format=%h %s"], repo_dir
    )
    work += [
        f"commit {line}"
        for line in commits.splitlines()
        if not line.split(" ", 1)[-1].startswith("new release ")
    ]
    return work


def clone_or_reset(src_repo: str, dst_repo_dir: Path, shallow: bool = False) -> None:
    """Make dst_repo_dir a clone of the master branch of src_repo, as if it had just been cloned.

    An existing clone is reset, which discards what an earlier run of the release scripts left in
    it, such as an unpushed release commit and tag, and build outputs.  If it also holds other work,
    such as a fix made by hand, you are asked first.  If there is no clone, or the reset fails, the
    repository is cloned from scratch.

    Args:
        src_repo: the URL of the repository
        dst_repo_dir: the directory of the clone
        shallow: whether to fetch only the latest commit of the master branch

    Raises:
        ReleaseError: If the directory has other work, which you decide not to discard.
    """
    if dst_repo_dir.exists():
        work = discardable_work(dst_repo_dir)
        if work:
            print(f"Resetting {dst_repo_dir} would discard:")
            for item in work:
                print("  " + item)
            if not prompt_yes_no("Discard it?", False):
                msg = "Keep that work elsewhere, then run release_build.py again."
                raise ReleaseError(msg)
    if git_repo_exists_at_path(dst_repo_dir):
        # A shallow clone fetches only master; a full clone also fetches the tags, and
        # --prune-tags deletes the local tags that are not on GitHub, such as an unpushed release
        # tag.
        # --force updates a tag that moved on GitHub, such as nightly; git refuses otherwise.
        fetch = ["--depth", "1"] if shallow else ["--force", "--prune", "--prune-tags"]
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


# =========================================================================================
# File Utils


def delete_if_exists(file_to_delete: Path) -> None:
    """Delete the given file, if it exists."""
    file_to_delete.unlink(missing_ok=True)


def delete_directory_if_exists(path: Path) -> None:
    """Delete the given directory and everything in it, if it exists."""
    if path.exists():
        # Make read-only files, such as those of a Gradle or Go cache, deletable.  This fails for a
        # file of another user, so the exit status is ignored.
        execute_status(["chmod", "-f", "-R", "u+rw", str(path)])
        shutil.rmtree(path)


# =========================================================================================
# Misc. Utils


def print_step(step: str) -> None:
    """Print a step in the release_build or release_push script."""
    print("\n")
    print(step)
    print("-" * len(step))


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
