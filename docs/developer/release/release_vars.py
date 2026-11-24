#!/usr/bin/env python3
"""Release variables."""

# See README-release-process.html for an explanation of how the release process works.

from __future__ import annotations

import os
import pwd
import shlex
import subprocess
from pathlib import Path

from release_errors import ReleaseError

# ---------------------------------------------------------------------------------
# The only methods that should go here are methods that help define global release
# variables.  All other methods that aid in release should go in release_utils.py


def execute_output(
    command: str | list[str],
    working_dir: Path | None = None,
) -> str:
    """Execute the given command.

    Returns:
        the output
    """
    if working_dir is not None:
        print(f"Executing in {working_dir}: {command}")
    else:
        print(f"Executing: {command}")
    command_line = shlex.split(command) if isinstance(command, str) else command

    process = subprocess.Popen(command_line, stdout=subprocess.PIPE, cwd=working_dir)
    out = process.communicate()[0]
    process.wait()
    return out.decode("utf-8")


def execute(
    command: str | list[str],
    working_dir: Path | None = None,
) -> None:
    """Execute the given command.

    Raises:
        ReleaseError: If the the status code is non-zero.
    """
    status = execute_status(command, working_dir)
    if status:
        msg = f"Error status {status} while executing {command} in {working_dir}"
        raise ReleaseError(msg)


def execute_status(
    command: str | list[str],
    working_dir: Path | None = None,
) -> int:
    """Execute the given command.

    Returns:
        The the status code.
    """
    if working_dir is not None:
        print(f"Executing in {working_dir}: {command}")
    else:
        print(f"Executing: {command}")
    command_line = shlex.split(command) if isinstance(command, str) else command

    return subprocess.call(command_line, cwd=working_dir)


# ---------------------------------------------------------------------------------

# Per-user directory for the temporary files created by the release process, in which the
# Checker Framework is cloned; the environment variable CF_RELEASE_DIR overrides it.
# ("USER = os.getlogin()" does not work; see https://github.com/python/cpython/issues/36915.
# Another alternative is: USER = os.getenv('USER').)
TMP_DIR = Path(
    os.environ.get("CF_RELEASE_DIR", Path("/tmp") / pwd.getpwuid(os.geteuid())[0] / "cf-release")
)

# Location in which we will download files to run sanity checks.
SANITY_DIR = TMP_DIR / "sanity"

# The existence of this file indicates that release_build completed.
# It is deleted at the beginning of a release_build run, and at the
# end of a release_push run.
RELEASE_BUILD_COMPLETED_FLAG_FILE = TMP_DIR / "release-build-completed"

# The GitHub repository of the Checker Framework, which includes the AFU.
LIVE_CHECKER_REPO = "git@github.com:eisop/checker-framework.git"

# The annotated JDK, which the build expects next to the Checker Framework, in BUILD_DIR.
ANNOTATED_JDK_REPO = "https://github.com/eisop/jdk.git"

# The clone in which release_build.py builds, commits, and tags the release, and from which
# release_push.py pushes it to LIVE_CHECKER_REPO.
BUILD_DIR = TMP_DIR / "build"
CHECKER_FRAMEWORK = BUILD_DIR / "checker-framework"

# The version of the release.  Without -Prelease=true, the build's version is a -SNAPSHOT version.
# If a new Gradle wrapper was recently installed, the first ./gradlew command may output:
#   Downloading https://services.gradle.org/distributions/gradle-6.6.1-bin.zip
SCRIPTS_CLONE = TMP_DIR / "checker-framework"
if not (SCRIPTS_CLONE / "gradlew").is_file():
    msg = (
        f"Run the release scripts from a clone of the Checker Framework in {SCRIPTS_CLONE}; see"
        " the Step by Step section of docs/developer/release/README-release-process.html."
    )
    raise ReleaseError(msg)
execute("./gradlew version -q -Prelease=true", SCRIPTS_CLONE)
CF_VERSION = execute_output("./gradlew version -q -Prelease=true", SCRIPTS_CLONE).strip()

# The release files, checker-framework-VERSION.zip and annotation-file-utilities-VERSION.zip,
# which are attached to the GitHub release.  The web site is generated from them.
RELEASE_FILES_DIR = TMP_DIR / "release-files"

os.environ["CHECKERFRAMEWORK"] = str(CHECKER_FRAMEWORK)
os.environ["TEXINPUTS"] = ".:..:"
# The JDK of the release build; the environment variable JAVA_21_HOME overrides the default.
os.environ.setdefault("JAVA_21_HOME", "/usr/lib/jvm/java-21-openjdk/")
os.environ["JAVA_HOME"] = os.environ["JAVA_21_HOME"]

PATH = os.environ["JAVA_HOME"] + "/bin:" + os.environ["PATH"]
PATH = PATH + ":/usr/bin"
PATH = PATH + ":."
os.environ["PATH"] = PATH

# Tools that must be on your PATH (besides common Unix ones like grep)
TOOLS = [
    "hevea",
    "java",
    "latex",
    "mvn",
    "git",
    "dot",
]
