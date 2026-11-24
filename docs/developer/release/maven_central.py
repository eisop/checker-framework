"""Check the release's Maven artifacts, and publish exactly those to Maven Central.

release_build.py publishes the signed artifacts to the local Maven repository and checks them there,
before anything is uploaded: every artifact is present with its POM, sources, Javadoc, and
signatures; the POMs have what Maven Central requires and depend only on released versions; the
signatures verify with the public key that users fetch from a keyserver; and the jars contain what
the release zip files contain.

release_push.py checks them again, and uploads exactly those files, as a bundle, to the Central
Portal.  The Portal validates the deployment, which waits for a request, or a click, to publish it,
irreversibly.  See docs/developer/maven-central-publishing.md.
"""

from __future__ import annotations

import base64
import hashlib
import json
import os
import re
import shutil
import subprocess
import tempfile
import time
import urllib.error
import urllib.request
import uuid
import xml.etree.ElementTree as ET
import zipfile
from pathlib import Path

from release_vars import CF_VERSION, RELEASE_FILES_DIR, TMP_DIR

NAMESPACE = "io.github.eisop"
PORTAL_API = "https://central.sonatype.com/api/v1/publisher"
DEPLOYMENTS_URL = "https://central.sonatype.com/publishing/deployments"
KEYSERVER = "keyserver.ubuntu.com"

# The artifacts of a release, in the namespace NAMESPACE.  Each has a jar, sources, and Javadoc.
EXPECTED_ARTIFACTS = (
    "annotation-file-utilities",
    "checker",
    "checker-qual",
    "checker-qual-android",
    "checker-util",
    "dataflow",
    "dataflow-errorprone",
    "dataflow-nullaway",
    "dataflow-shaded",
    "framework",
    "framework-all",
    "framework-errorprone",
    "framework-test",
    "javacutil",
)

# The artifacts whose jar is also in a release zip file: artifact -> (zip file name, jar in it).
# Every file in the artifact's jar must be in the zip's jar, with the same contents.  The zip's
# checker.jar also contains the qualifiers, which the checker artifact takes from checker-qual.
JARS_IN_ZIP_FILES = {
    "checker": ("checker-framework", "checker/dist/checker.jar"),
    "checker-qual": ("checker-framework", "checker/dist/checker-qual.jar"),
    "checker-util": ("checker-framework", "checker/dist/checker-util.jar"),
    "annotation-file-utilities": (
        "annotation-file-utilities",
        "annotation-file-utilities/dist/annotation-file-utilities.jar",
    ),
}

POM_NAMESPACE = "{http://maven.apache.org/POM/4.0.0}"

# Files that are checksums of other files, which the bundle for the Portal gets fresh.
CHECKSUM_SUFFIXES = {".md5", ".sha1", ".sha256", ".sha512"}

# How long to wait for the Portal to validate, or to publish, a deployment, and how often to ask.
VALIDATION_TIMEOUT_SECONDS = 30 * 60
PUBLICATION_TIMEOUT_SECONDS = 60 * 60
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


def gpg_executable() -> str:
    """Return the gpg executable that signs the release.

    Returns:
        the gpg executable
    """
    return gradle_user_properties().get("signing.gnupg.executable", "gpg")


# =========================================================================================
# Requests to Maven Central


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


def request(
    method: str,
    url: str,
    headers: dict[str, str],
    data: bytes | None = None,
    timeout: int = 300,
) -> tuple[int, str]:
    """Send a request to Maven Central.

    An error status raises urllib.error.HTTPError, and a failure to reach Maven Central raises
    another OSError.

    Returns:
        the HTTP status and the body of the response
    """
    req = urllib.request.Request(url, data=data, method=method)
    # Unredirected headers are not sent on to the target of a redirect, which may be another host.
    for name, value in headers.items():
        req.add_unredirected_header(name, value)
    with urllib.request.urlopen(req, timeout=timeout) as response:
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


# =========================================================================================
# Signatures


def signing_fingerprint(gpg: str, signature: bytes) -> str | None:
    """Return the fingerprint, or else the key ID, of the key that made the given signature.

    Returns:
        the fingerprint or key ID, or None if the signature names neither
    """
    packets = subprocess.run(
        [gpg, "--list-packets"], input=signature, capture_output=True, check=False
    ).stdout.decode(errors="replace")
    match = re.search(r"issuer fpr v\d ([0-9A-F]+)", packets) or re.search(
        r"keyid ([0-9A-F]+)", packets
    )
    return match.group(1) if match else None


def verify_with_published_key(gpg: str, signed_files: list[Path]) -> list[str]:
    """Verify the signature FILE.asc of each given file with the signer's key from a keyserver.

    Users of the release, and Maven Central, fetch the public key from a keyserver.  A copy there
    that has expired, or that lacks the key's user ID, makes the signatures look invalid, even
    though the local key is fine.  The signatures are verified in a temporary keyring.

    Returns:
        a description of each problem, or an empty list if every signature verifies
    """
    if not signed_files:
        return []
    fingerprint = signing_fingerprint(gpg, Path(f"{signed_files[0]}.asc").read_bytes())
    if fingerprint is None:
        return [f"Could not find the key that made the signature {signed_files[0]}.asc."]
    upload = (
        f"Upload your current public key: gpg --keyserver hkps://{KEYSERVER}"
        f" --send-keys {fingerprint}"
    )
    url = f"https://{KEYSERVER}/pks/lookup?op=get&options=mr&search=0x{fingerprint}"
    try:
        with urllib.request.urlopen(url, timeout=60) as response:
            public_key = response.read()
    except urllib.error.HTTPError as e:
        if e.code == 404:
            return [f"{KEYSERVER} does not have your key {fingerprint}.  {upload}"]
        return [f"Could not get your key {fingerprint} from {KEYSERVER}: {e}"]
    except OSError as e:
        return [f"Could not reach {KEYSERVER} to check your public key: {e}"]

    problems = []
    with tempfile.TemporaryDirectory() as home:
        Path(home).chmod(0o700)
        options = [gpg, "--homedir", home, "--batch", "--no-autostart"]
        subprocess.run([*options, "--import"], input=public_key, capture_output=True, check=False)
        for signed in signed_files:
            status = subprocess.run(
                [*options, "--status-fd", "1", "--verify", f"{signed}.asc", str(signed)],
                capture_output=True,
                check=False,
            ).stdout.decode(errors="replace")
            if "[GNUPG:] GOODSIG" in status:
                continue
            if "[GNUPG:] EXPKEYSIG" in status:
                return [f"Your key {fingerprint} on {KEYSERVER} has expired.  {upload}"]
            if "[GNUPG:] BADSIG" in status:
                problems.append(f"The signature {signed}.asc does not match {signed.name}.")
            elif fingerprint != signing_fingerprint(gpg, Path(f"{signed}.asc").read_bytes()):
                problems.append(f"{signed}.asc is signed by another key than {fingerprint}.")
            else:
                return [
                    (
                        f"Your key {fingerprint} on {KEYSERVER} does not verify your signatures;"
                        f" it may lack its user ID.  {upload}"
                    )
                ]
    return problems


# =========================================================================================
# The artifacts in the local Maven repository


def local_repository() -> Path:
    """Return the local Maven repository, to which publishToMavenLocal publishes.

    Returns:
        the local Maven repository
    """
    settings = Path.home() / ".m2" / "settings.xml"
    if settings.exists():
        match = re.search(
            r"<localRepository>\s*(.*?)\s*</localRepository>",
            settings.read_text(encoding="utf-8"),
        )
        if match:
            return Path(match.group(1).replace("${user.home}", str(Path.home())))
    return Path.home() / ".m2" / "repository"


def artifact_dirs(version: str) -> dict[str, Path]:
    """Return the directories of the artifacts of the given version in the local Maven repository.

    Returns:
        a map from artifact ID to its directory, for each artifact of the version
    """
    namespace_dir = local_repository().joinpath(*NAMESPACE.split("."))
    if not namespace_dir.is_dir():
        return {}
    return {
        artifact.name: artifact / version
        for artifact in sorted(namespace_dir.iterdir())
        if (artifact / version).is_dir()
    }


def clear_local_artifacts(version: str) -> None:
    """Delete the artifacts of the given version from the local Maven repository.

    Publishing then leaves exactly the files of this build, and none of an earlier one.
    """
    for directory in artifact_dirs(version).values():
        shutil.rmtree(directory)


def pom_problems(pom: Path, artifact: str, version: str) -> list[str]:
    """Return what is wrong with the given POM of a release artifact.

    Maven Central requires a name, description, URL, license, developer, and SCM URL.  A release
    must depend only on released versions, and on its own artifacts of the same version.

    Returns:
        a description of each problem
    """
    try:
        project = ET.parse(pom).getroot()
    except ET.ParseError as e:
        return [f"{pom} is not valid XML: {e}"]

    def text(path: str) -> str:
        element = project.find("/".join(POM_NAMESPACE + part for part in path.split("/")))
        return (element.text or "").strip() if element is not None else ""

    problems = []
    for path, expected in (("groupId", NAMESPACE), ("artifactId", artifact), ("version", version)):
        if text(path) != expected:
            problems.append(f"{pom}: {path} is {text(path)!r}, not {expected!r}.")
    required = (
        "name",
        "description",
        "url",
        "licenses/license/name",
        "developers/developer/name",
        "scm/url",
    )
    problems.extend(
        f"{pom} has no {path}, which Maven Central requires." for path in required if not text(path)
    )
    for dependency in project.iter(POM_NAMESPACE + "dependency"):
        group = dependency.findtext(POM_NAMESPACE + "groupId", "").strip()
        name = dependency.findtext(POM_NAMESPACE + "artifactId", "").strip()
        dependency_version = dependency.findtext(POM_NAMESPACE + "version", "").strip()
        if "SNAPSHOT" in dependency_version:
            problems.append(f"{pom} depends on {group}:{name}:{dependency_version}, a snapshot.")
        if group == NAMESPACE and dependency_version != version:
            problems.append(f"{pom} depends on {group}:{name}:{dependency_version}, not {version}.")
        if group == "org.checkerframework" and name in EXPECTED_ARTIFACTS:
            problems.append(f"{pom} depends on {group}:{name}, not {NAMESPACE}:{name}.")
    return problems


def jar_problems(jar: Path, kind: str) -> list[str]:
    """Return what is wrong with the given jar of a release artifact.

    Args:
        jar: the jar
        kind: "classes", "sources", or "javadoc"

    Returns:
        a description of each problem
    """
    try:
        with zipfile.ZipFile(jar) as contents:
            names = contents.namelist()
            corrupt = contents.testzip()
    except (OSError, zipfile.BadZipFile) as e:
        return [f"{jar} is not a valid jar: {e}"]
    if corrupt is not None:
        return [f"{jar} is corrupt: {corrupt}"]
    suffix = {"classes": ".class", "sources": ".java", "javadoc": "index.html"}[kind]
    if not any(name.endswith(suffix) for name in names):
        return [f"{jar} contains no {suffix} file."]
    return []


def zip_jar_problems(jar: Path, artifact: str, version: str) -> list[str]:
    """Return how the given jar differs from the same jar in a release zip file.

    Returns:
        a description of each problem
    """
    zip_name, jar_in_zip = JARS_IN_ZIP_FILES[artifact]
    release_zip = RELEASE_FILES_DIR / f"{zip_name}-{version}.zip"
    if not release_zip.exists():
        return [f"{release_zip} does not exist, so {jar.name} cannot be compared with it."]
    with zipfile.ZipFile(release_zip) as outer:
        try:
            zip_jar_bytes = outer.read(f"{zip_name}-{version}/{jar_in_zip}")
        except KeyError:
            return [f"{release_zip} does not contain {jar_in_zip}."]
    with tempfile.TemporaryDirectory() as tmp:
        zip_jar = Path(tmp) / "zip.jar"
        zip_jar.write_bytes(zip_jar_bytes)
        with zipfile.ZipFile(jar) as maven, zipfile.ZipFile(zip_jar) as zipped:
            in_zip = {info.filename: info.CRC for info in zipped.infolist()}
            differing = [
                info.filename
                for info in maven.infolist()
                if not info.is_dir() and in_zip.get(info.filename) != info.CRC
            ]
    if differing:
        return [
            (
                f"{jar.name} has {len(differing)} files that are missing from, or differ from, the"
                f" {jar_in_zip} in {release_zip.name}, such as {differing[0]}."
            )
        ]
    return []


def verify_local_artifacts(version: str) -> list[str]:
    """Check the release's artifacts in the local Maven repository.

    Returns:
        a description of each problem, or an empty list if the artifacts are ready for Maven Central
    """
    dirs = artifact_dirs(version)
    problems = [
        f"{NAMESPACE}:{artifact}:{version} is not in {local_repository()}."
        for artifact in EXPECTED_ARTIFACTS
        if artifact not in dirs
    ]
    problems += [
        f"{NAMESPACE}:{artifact}:{version} is not a known artifact of a release; if it should be"
        " published, add it to EXPECTED_ARTIFACTS in maven_central.py."
        for artifact in dirs
        if artifact not in EXPECTED_ARTIFACTS
    ]
    signed = []
    for artifact, directory in dirs.items():
        base = directory / f"{artifact}-{version}"
        pom = Path(f"{base}.pom")
        jars = {
            "classes": Path(f"{base}.jar"),
            "sources": Path(f"{base}-sources.jar"),
            "javadoc": Path(f"{base}-javadoc.jar"),
        }
        missing = [f.name for f in (pom, *jars.values()) if not f.exists()]
        if missing:
            problems.append(f"{directory} lacks {', '.join(missing)}.")
            continue
        problems += pom_problems(pom, artifact, version)
        for kind, jar in jars.items():
            problems += jar_problems(jar, kind)
        if artifact in JARS_IN_ZIP_FILES:
            problems += zip_jar_problems(jars["classes"], artifact, version)
        for file in sorted(directory.iterdir()):
            if file.suffix in {".asc", *CHECKSUM_SUFFIXES}:
                continue
            if Path(f"{file}.asc").exists():
                signed.append(file)
            else:
                problems.append(f"{file} has no signature {file.name}.asc.")
    if not problems:
        problems += verify_with_published_key(gpg_executable(), signed)
    return problems


# =========================================================================================
# Publishing to Maven Central


def make_bundle(version: str) -> Path:
    """Put the release's artifacts in the local Maven repository into a bundle for the Portal.

    The bundle is a zip file in the layout of a Maven repository, with an MD5 and a SHA-1 checksum
    of each file.

    Returns:
        the bundle
    """
    bundle = TMP_DIR / f"central-bundle-{version}.zip"
    repository = local_repository()
    with zipfile.ZipFile(bundle, "w", zipfile.ZIP_DEFLATED) as contents:
        for directory in artifact_dirs(version).values():
            for file in sorted(directory.iterdir()):
                if file.suffix in CHECKSUM_SUFFIXES:
                    continue
                name = file.relative_to(repository).as_posix()
                data = file.read_bytes()
                contents.writestr(name, data)
                contents.writestr(f"{name}.md5", hashlib.md5(data).hexdigest())
                contents.writestr(f"{name}.sha1", hashlib.sha1(data).hexdigest())
    return bundle


def upload_bundle(bundle: Path, version: str, headers: dict[str, str]) -> str:
    """Upload the given bundle to the Central Portal, to be published by a later request.

    Returns:
        the ID of the deployment
    """
    boundary = uuid.uuid4().hex
    body = b"".join(
        [
            f"--{boundary}\r\n".encode(),
            f'Content-Disposition: form-data; name="bundle"; filename="{bundle.name}"\r\n'.encode(),
            b"Content-Type: application/octet-stream\r\n\r\n",
            bundle.read_bytes(),
            f"\r\n--{boundary}--\r\n".encode(),
        ]
    )
    _, deployment_id = request(
        "POST",
        f"{PORTAL_API}/upload?publishingType=USER_MANAGED&name=checker-framework-{version}",
        {**headers, "Content-Type": f"multipart/form-data; boundary={boundary}"},
        data=body,
        timeout=1800,
    )
    return deployment_id.strip()


def deployment_status(deployment_id: str, headers: dict[str, str]) -> dict:
    """Return the Portal's status of the given deployment.

    Returns:
        the status, with the deploymentState, the purls of its components, and any errors
    """
    _, body = request("POST", f"{PORTAL_API}/status?id={deployment_id}", headers)
    return json.loads(body)


def deployment_file(version: str) -> Path:
    """Return the file that holds the ID of the deployment of the given version.

    A rerun of release_push.py uses it to find the deployment that an earlier run uploaded.

    Returns:
        the file that holds the deployment ID
    """
    return TMP_DIR / f"central-deployment-{version}.txt"


def saved_deployment(version: str) -> tuple[str | None, str | None]:
    """Return the deployment of the given version that an earlier run uploaded, and its state.

    Returns:
        the deployment ID and its deploymentState, or None for each that is unknown
    """
    if not deployment_file(version).exists():
        return None, None
    deployment_id = deployment_file(version).read_text(encoding="utf-8").strip()
    try:
        state = deployment_status(deployment_id, user_authorization()).get("deploymentState")
    except (OSError, ValueError) as e:
        print(f"Could not get the state of deployment {deployment_id}: {e}")
        return deployment_id, None
    return deployment_id, state


def on_maven_central(version: str) -> bool:
    """Return true if Maven Central serves the given version of the checker artifact.

    Returns:
        true if the version's POM of the checker artifact can be downloaded from Maven Central
    """
    path = "/".join([*NAMESPACE.split("."), "checker", version, f"checker-{version}.pom"])
    try:
        with urllib.request.urlopen(f"https://repo1.maven.org/maven2/{path}", timeout=60):
            return True
    except OSError:
        return False


def wait_until_published(deployment_id: str) -> bool:
    """Wait until Maven Central has published the given deployment.

    Returns:
        true if the deployment is published; otherwise the reason is printed
    """
    deadline = time.monotonic() + PUBLICATION_TIMEOUT_SECONDS
    try:
        while True:
            status = deployment_status(deployment_id, user_authorization())
            state = status.get("deploymentState")
            print(f"  {state}")
            if state == "PUBLISHED":
                return True
            if state == "FAILED" or time.monotonic() > deadline:
                print(json.dumps(status.get("errors", status), indent=2))
                return False
            time.sleep(POLL_SECONDS)
    except (OSError, ValueError) as e:
        print(f"Could not get the state of deployment {deployment_id}: {e}")
        return False


def upload_to_maven_central(version: str) -> str | None:
    """Upload the checked release artifacts to Maven Central, and wait until they are validated.

    The artifacts in the local Maven repository are checked again, and exactly those files are
    uploaded.  Nothing is published: the deployment waits in the Portal for publish_deployment, or
    for a click on Publish.

    Returns:
        the ID of the validated deployment, or None if there is none; the reason is printed
    """
    problems = verify_local_artifacts(version)
    if problems:
        print("The artifacts in the local Maven repository are not ready for Maven Central:")
        for problem in problems:
            print("  " + problem)
        return None
    print(f"The artifacts of {version} in {local_repository()} are ready for Maven Central.")

    headers = user_authorization()
    try:
        bundle = make_bundle(version)
        print(f"Uploading {bundle} ({bundle.stat().st_size // 1_000_000} MB).")
        deployment_id = upload_bundle(bundle, version, headers)
        deployment_file(version).write_text(deployment_id + "\n", encoding="utf-8")
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
    prefix = f"pkg:maven/{NAMESPACE}/"
    components = {
        purl.removeprefix(prefix).removesuffix("@" + version)
        for purl in purls
        if purl.startswith(prefix) and purl.endswith("@" + version)
    }
    if len(components) != len(purls) or components != set(EXPECTED_ARTIFACTS):
        print(f"The deployment does not consist of the release's artifacts: {sorted(purls)}")
        return None
    print(f"The deployment is valid.  It contains the {len(purls)} artifacts of {version}.")
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
    print(f"Maven Central is publishing the deployment (HTTP {status}); see {DEPLOYMENTS_URL}.")
    return True
