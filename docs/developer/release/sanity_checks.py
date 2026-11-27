#!/usr/bin/env python3
"""Release utilities.

This contains no main method only utility functions to
run sanity checks on the Checker Framework.
Created by Jonathan Burke 11/21/2012

Copyright (c) 2012 University of Washington
"""

import re
import subprocess
from pathlib import Path

from release_errors import ReleaseError
from release_utils import delete_directory_if_exists
from release_vars import (
    CHECKER_FRAMEWORK,
    MAVEN_REPO_OPTIONS,
    SANITY_DIR,
    execute,
    gradle_release,
)


def javac_sanity_check(release_zip: Path, release_version: str) -> None:
    """Run the Nullness Checker of the given Checker Framework release zip file.

    Run it on the NullnessExampleWithWarnings.java that the zip file contains, and verify the
    output.  Fails if the expected errors are not found in the output.
    """
    javac_sanity_dir = SANITY_DIR / "javac"
    delete_directory_if_exists(javac_sanity_dir)
    javac_sanity_dir.mkdir(parents=True)

    # unzip, unlike zipfile, keeps the permissions, so checker/bin/javac stays executable.
    execute(f"unzip -q {release_zip} -d {javac_sanity_dir}")

    deploy_dir = javac_sanity_dir / f"checker-framework-{release_version}"
    nullness_example = deploy_dir / "docs" / "examples" / "NullnessExampleWithWarnings.java"

    sanity_script = Path(__file__).resolve().parent / "javac-sanity-check.sh"
    execute(f"{sanity_script} {deploy_dir} {nullness_example}")


def maven_sanity_check(sub_sanity_dir_name: str) -> None:
    """Run the Maven sanity check.

    It uses the artifacts that release_build.py deployed to the local Maven repository.
    """
    maven_sanity_dir = SANITY_DIR / sub_sanity_dir_name
    delete_directory_if_exists(maven_sanity_dir)
    maven_sanity_dir.mkdir(parents=True)

    maven_example_dir = maven_sanity_dir / "MavenExample"
    output_log = maven_example_dir / "output.log"

    # -Prelease=true makes the example use the release version rather than a -SNAPSHOT version.
    get_example_dir_cmd = gradle_release("updateCopyMavenExample", f"-PdestDir={maven_sanity_dir}")

    execute(get_example_dir_cmd, CHECKER_FRAMEWORK)

    # The example's build is supposed to fail with the example's expected error.  JAVA_HOME is
    # the release's JDK; see release_vars.py.
    print(f"Executing in {maven_example_dir}: mvn compile > {output_log}")
    with output_log.open("w") as log:
        subprocess.run(
            ["mvn", "-B", *MAVEN_REPO_OPTIONS, "compile"],
            cwd=maven_example_dir,
            stdout=log,
            stderr=log,
            check=False,
        )
    expected_error = re.compile(
        r"MavenExample\.java:\[30,(29|30)\] \[assignment\.type\.incompatible\]"
        r" incompatible types in assignment\."
    )
    if not expected_error.search(output_log.read_text(encoding="utf-8")):
        msg = f"The Maven example did not report its expected error; see {output_log}."
        raise ReleaseError(msg)
    print("Maven sanity check: passed!")
