"""Publish the release to Maven Central.

The build uploads the artifacts to Sonatype's OSSRH staging API, which keeps them in a staging
repository.  A request to that API moves the repository to the Central Portal as a deployment,
which the Portal validates.  A validated deployment is published, irreversibly, by a request to the
Portal's publisher API.  See docs/developer/maven-central-publishing.md.
"""

from __future__ import annotations

import base64
import json
import os
import re
import time
import urllib.error
import urllib.request
from pathlib import Path

from release_vars import CF_VERSION, CHECKER_FRAMEWORK, execute_status

NAMESPACE = "io.github.eisop"
STAGING_API = "https://ossrh-staging-api.central.sonatype.com"
PORTAL_API = "https://central.sonatype.com/api/v1/publisher"
DEPLOYMENTS_URL = "https://central.sonatype.com/publishing/deployments"

# How long to wait for the Portal to validate a deployment, and how often to ask.
VALIDATION_TIMEOUT_SECONDS = 30 * 60
POLL_SECONDS = 30


def gradle_user_properties() -> dict[str, str]:
    """Return the Gradle properties of the user, which the release build reads.

    They are the properties in gradle.properties in the Gradle user home, and the properties set by
    ORG_GRADLE_PROJECT_ environment variables, which take precedence.

    Returns:
        the Gradle properties of the user
    """
    properties = {}
    gradle_user_home = Path(os.environ.get("GRADLE_USER_HOME", Path.home() / ".gradle"))
    properties_file = gradle_user_home / "gradle.properties"
    if properties_file.exists():
        for line in properties_file.read_text(encoding="utf-8").splitlines():
            match = re.match(r"\s*([^#!\s=:][^\s=:]*)\s*[=:\s]\s*(.*)", line)
            if match:
                properties[match.group(1)] = match.group(2).strip()
    prefix = "ORG_GRADLE_PROJECT_"
    for name, value in os.environ.items():
        if name.startswith(prefix):
            properties[name[len(prefix) :]] = value
    return properties


def authorization(username: str, password: str) -> dict[str, str]:
    """Return the header that authorizes a request to Maven Central with the given user token.

    Returns:
        the Authorization header
    """
    token = base64.b64encode(f"{username}:{password}".encode()).decode()
    return {"Authorization": "Bearer " + token}


def user_authorization() -> dict[str, str]:
    """Return the header that authorizes a request with the user token from the Gradle properties.

    Returns:
        the Authorization header
    """
    properties = gradle_user_properties()
    return authorization(
        properties.get("SONATYPE_NEXUS_USERNAME", ""), properties.get("SONATYPE_NEXUS_PASSWORD", "")
    )


def request(method: str, url: str, headers: dict[str, str]) -> tuple[int, str]:
    """Send a request to Maven Central.

    An error status raises urllib.error.HTTPError, and a failure to reach Maven Central raises
    another OSError.

    Returns:
        the HTTP status and the body of the response
    """
    req = urllib.request.Request(url, method=method, headers=headers)
    with urllib.request.urlopen(req, timeout=300) as response:
        return response.status, response.read().decode()


def check_central_token(username: str, password: str) -> str | None:
    """Check that Maven Central accepts the given user token.

    The Central Portal answers a query of the publisher API only for a valid token.

    Returns:
        a description of the problem, or None if Maven Central accepts the token
    """
    url = f"{PORTAL_API}/published?namespace={NAMESPACE}&name=checker&version={CF_VERSION}"
    try:
        request("GET", url, authorization(username, password))
    except urllib.error.HTTPError as e:
        return (
            f"Maven Central rejects the user token in SONATYPE_NEXUS_USERNAME and"
            f" SONATYPE_NEXUS_PASSWORD (HTTP {e.code}); generate a new one at"
            " https://central.sonatype.com/usertoken."
        )
    except OSError as e:
        return f"Could not reach Maven Central to check the user token: {e}"
    return None


def deployment_ids(headers: dict[str, str]) -> set[str]:
    """Return the Portal deployment IDs of this machine's staging repositories in the namespace.

    Returns:
        the deployment IDs
    """
    _, body = request(
        "GET", f"{STAGING_API}/manual/search/repositories?profile_id={NAMESPACE}", headers
    )
    repositories = json.loads(body).get("repositories", [])
    return {r["portal_deployment_id"] for r in repositories if r.get("portal_deployment_id")}


def deployment_status(deployment_id: str, headers: dict[str, str]) -> dict:
    """Return the Portal's status of the given deployment.

    Returns:
        the status, with the deploymentState, the purls of its components, and any errors
    """
    _, body = request("POST", f"{PORTAL_API}/status?id={deployment_id}", headers)
    return json.loads(body)


def upload_to_maven_central(version: str) -> str | None:
    """Upload the release to Maven Central and wait until the Portal has validated it.

    Nothing is published.  The deployment waits in the Portal for publish_deployment, or for a
    click on Publish.

    Returns:
        the ID of the validated deployment, or None if there is none; the reason is printed
    """
    # The upload must come from this machine: the staging API ties it to the uploader's IP.
    publish = "./gradlew publish -Prelease=true --no-parallel --console=plain"
    if execute_status(publish, CHECKER_FRAMEWORK) != 0:
        print(f"{publish} failed.")
        return None

    headers = user_authorization()
    try:
        before = deployment_ids(headers)
        request(
            "POST",
            f"{STAGING_API}/manual/upload/defaultRepository/{NAMESPACE}"
            "?publishing_type=user_managed",
            headers,
        )
        new = deployment_ids(headers) - before
        if len(new) != 1:
            print(f"Expected one new deployment in the Portal, found {len(new)}.")
            return None
        deployment_id = new.pop()
        print(f"Deployment {deployment_id} is in the Portal; waiting for its validation.")
        deadline = time.monotonic() + VALIDATION_TIMEOUT_SECONDS
        while True:
            status = deployment_status(deployment_id, headers)
            state = status.get("deploymentState")
            print(f"  {state}")
            if state == "VALIDATED":
                break
            if state == "FAILED" or time.monotonic() > deadline:
                print(json.dumps(status.get("errors", status), indent=2))
                return None
            time.sleep(POLL_SECONDS)
    except (OSError, ValueError, KeyError) as e:
        print(f"The request to Maven Central failed: {e}")
        return None

    purls = status.get("purls", [])
    wrong_version = [p for p in purls if not p.endswith("@" + version)]
    if not purls or wrong_version:
        print(f"The deployment does not consist of version {version}: {purls}")
        return None
    print(f"The deployment is valid.  It contains {len(purls)} components:")
    for purl in purls:
        print("  " + purl)
    return deployment_id


def publish_deployment(deployment_id: str) -> bool:
    """Publish the given validated deployment to Maven Central.  This is irreversible.

    Returns:
        True if the Portal is publishing the deployment; otherwise the reason is printed
    """
    try:
        status, _ = request(
            "POST", f"{PORTAL_API}/deployment/{deployment_id}", user_authorization()
        )
    except OSError as e:
        print(f"The request to publish the deployment failed: {e}")
        return False
    print(f"Maven Central is publishing the deployment (HTTP {status}).  It is on Maven Central")
    print(f"within about half an hour; see {DEPLOYMENTS_URL}.")
    return True
