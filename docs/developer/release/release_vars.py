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
GITHUB_REPO = "eisop/checker-framework"
LIVE_CHECKER_REPO = f"git@github.com:{GITHUB_REPO}.git"

# The annotated JDK, which the build expects next to the Checker Framework, in BUILD_DIR.
ANNOTATED_JDK_REPO = "https://github.com/eisop/jdk.git"

# The clone in which release_build.py builds, commits, and tags the release, and from which
# release_push.py pushes it to LIVE_CHECKER_REPO.
BUILD_DIR = TMP_DIR / "build"
CHECKER_FRAMEWORK = BUILD_DIR / "checker-framework"
CHANGELOG = CHECKER_FRAMEWORK / "docs" / "CHANGELOG.md"

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

os.environ["CHECKERFRAMEWORK"] = str(CHECKER_FRAMEWORK)
os.environ["TEXINPUTS"] = ".:..:"
# The JDK of the release build, also for the Gradle command below; the environment variable
# JAVA_21_HOME overrides the default.
os.environ.setdefault("JAVA_21_HOME", "/usr/lib/jvm/java-21-openjdk/")
os.environ["JAVA_HOME"] = os.environ["JAVA_21_HOME"]
os.environ["PATH"] = os.environ["JAVA_HOME"] + "/bin:" + os.environ["PATH"]

execute("./gradlew version -q -Prelease=true", SCRIPTS_CLONE)
CF_VERSION = execute_output("./gradlew version -q -Prelease=true", SCRIPTS_CLONE).strip()

# The release files, checker-framework-VERSION.zip and annotation-file-utilities-VERSION.zip,
# which are attached to the GitHub release.  The web site is generated from them.
RELEASE_FILES_DIR = TMP_DIR / "release-files"

# The repositories that the build clones or updates, besides the Checker Framework, with the
# Gradle task that does it.  Build Steps 1 and 3 get them once; the release tag records their
# commits.
HELPER_CLONES = {
    BUILD_DIR / "jdk": ":framework:cloneAnnotatedJdk",
    CHECKER_FRAMEWORK / "checker" / "bin-devel" / ".plume-scripts": ":getPlumeScripts",
    CHECKER_FRAMEWORK / "checker" / "bin-devel" / ".html-tools": ":getHtmlTools",
}

# The options of the Gradle commands that build, test, and publish the release.  -Prelease=true
# makes the version releaseVersion rather than a -SNAPSHOT version.  Without the -x options, every
# Gradle command would update the repositories in HELPER_CLONES again, and a change to one of them
# between two commands would change the release.
GRADLE_RELEASE_OPTIONS = ["-Prelease=true", "--console=plain"]
for task in HELPER_CLONES.values():
    GRADLE_RELEASE_OPTIONS += ["-x", task]

# The local Maven repository to which release_build.py publishes, if not the user's; the
# environment variable CF_RELEASE_MAVEN_REPO sets it.  CI sets it, so that a test run does not
# leave a release version in the user's repository.
LOCAL_MAVEN_REPO = os.environ.get("CF_RELEASE_MAVEN_REPO")
MAVEN_REPO_OPTIONS = [f"-Dmaven.repo.local={LOCAL_MAVEN_REPO}"] if LOCAL_MAVEN_REPO else []
GRADLE_RELEASE_OPTIONS += MAVEN_REPO_OPTIONS

# The signing key, if not signing.gnupg.keyName in the user's gradle.properties; the environment
# variable CF_RELEASE_SIGNING_KEY sets it.  (Gradle ignores an ORG_GRADLE_PROJECT_ environment
# variable whose name contains a dot, such as one for signing.gnupg.keyName.)
SIGNING_KEY = os.environ.get("CF_RELEASE_SIGNING_KEY")
if SIGNING_KEY:
    GRADLE_RELEASE_OPTIONS.append(f"-Psigning.gnupg.keyName={SIGNING_KEY}")


def gradle_release(*tasks: str) -> list[str]:
    """Return the Gradle command that runs the given tasks for the release.

    Returns:
        the Gradle command
    """
    return ["./gradlew", *tasks, *GRADLE_RELEASE_OPTIONS]


# Tools that must be on your PATH (besides common Unix ones like grep)
TOOLS = [
    "hevea",
    "java",
    "latex",
    "mvn",
    "git",
    "dot",
    "gh",
    "make",
    "unzip",
]
