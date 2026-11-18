#!/usr/bin/env python
"""Release utilities.

This contains no main method only utility functions to
run sanity checks on the Checker Framework.
Created by Jonathan Burke 11/21/2012

Copyright (c) 2012 University of Washington
"""

import os
import pathlib
from pathlib import Path

from release_utils import (
    delete_directory,
    delete_directory_if_exists,
    execute_write_to_file,
)
from release_vars import (
    CHECKER_FRAMEWORK,
    SANITY_DIR,
    execute,
)


def javac_sanity_check(release_zip: Path, release_version: str) -> None:
    """Run the Nullness Checker of the given Checker Framework release zip file.

    Run it on the NullnessExampleWithWarnings.java that the zip file contains, and verify the
    output.  Fails if the expected errors are not found in the output.
    """
    javac_sanity_dir = Path(SANITY_DIR) / "javac"
    delete_directory_if_exists(javac_sanity_dir)
    execute(f"mkdir -p {javac_sanity_dir}")

    # unzip, unlike zipfile, keeps the permissions, so checker/bin/javac stays executable.
    execute(f"unzip -q {release_zip} -d {javac_sanity_dir}")

    deploy_dir = Path(javac_sanity_dir) / f"checker-framework-{release_version}"
    nullness_example = deploy_dir / "docs" / "examples" / "NullnessExampleWithWarnings.java"

    sanity_script = Path(__file__).resolve().parent / "javac-sanity-check.sh"
    execute(f"{sanity_script} {deploy_dir} {nullness_example}")


def maven_sanity_check(sub_sanity_dir_name: str) -> None:
    """Run the Maven sanity check.

    It uses the artifacts that release_build.py deployed to the local Maven repository.
    """
    maven_sanity_dir = Path(SANITY_DIR) / sub_sanity_dir_name
    if pathlib.Path(maven_sanity_dir).is_dir():
        delete_directory(maven_sanity_dir)

    execute(f"mkdir -p {maven_sanity_dir}")

    maven_example_dir = Path(maven_sanity_dir) / "MavenExample"
    output_log = Path(maven_example_dir) / "output.log"

    # -Prelease=true makes the example use the release version rather than a -SNAPSHOT version.
    get_example_dir_cmd = (
        f"./gradlew updateCopyMavenExample -Prelease=true -PdestDir={maven_sanity_dir}"
    )

    execute(get_example_dir_cmd, CHECKER_FRAMEWORK)

    os.environ["JAVA_HOME"] = os.environ["JAVA_21_HOME"]
    execute_write_to_file("mvn compile", output_log, False, maven_example_dir)
