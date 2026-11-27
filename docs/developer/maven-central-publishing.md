# Publishing to Maven Central

How publishing is wired, how a release gets from the build to Maven Central, and
what it would take to publish nightly snapshots.

## What the build does today

Publishing is plain `maven-publish` + `signing`, configured in
[`gradle-mvn-push.gradle`](../../gradle-mvn-push.gradle) — no third-party
publishing plugin. The namespace is `io.github.eisop`.

| | destination | signed |
| --- | --- | --- |
| `-SNAPSHOT` version | `https://central.sonatype.com/repository/maven-snapshots/` | no |
| release version | `https://ossrh-staging-api.central.sonatype.com/service/local/staging/deploy/maven2/` | yes |

`-Prelease=true` selects the release version: without it, the version is
`releaseVersion` (in [`release.gradle`](../../release.gradle)) followed by
`-SNAPSHOT`.

A release does not use `./gradlew publish`. `release_build.py` runs
`./gradlew publishToMavenLocal -Prelease=true`, and checks the signed artifacts
in the local Maven repository; `release_push.py` uploads exactly those files to
the Central Portal. See below.

Snapshots need no publishing configuration of their own: the snapshot URL is
already the Central Portal snapshot repository, and snapshots are deliberately
unsigned (`tasks.withType(Sign) { onlyIf { !isSnapshot && ... } }`), so a
snapshot job needs no GPG key.

## Signing: any maintainer can sign a release

Maven Central does not pin a signing key to a namespace. It checks that each
artifact's detached signature verifies against a public key it can fetch from a
public keyserver — so several maintainers can each sign with their own key, and
nothing needs to change in this repository when a new one starts releasing.

What each maintainer needs, once:

1. A GPG key, with the public half uploaded to a keyserver Central queries
   (`keys.openpgp.org` or `keyserver.ubuntu.com`), and not expired.
2. Publish rights on the `io.github.eisop` namespace in the Central Portal.
3. Their own Portal user token in `~/.gradle/gradle.properties`, as
   `SONATYPE_NEXUS_USERNAME` / `SONATYPE_NEXUS_PASSWORD`.

The key belongs in that same per-user file, not on the command line and not in
this repository:

```properties
signing.gnupg.keyName=<your key id or email>
```

The release command then names no key at all. `gradle-mvn-push.gradle` calls
`useGpgCmd()`, so signing goes through the local `gpg` (and `gpg-agent`, so the
passphrase is entered once rather than once per artifact). A release publish
fails with an explicit message if `signing.gnupg.keyName` is unset: without it
`gpg` would quietly sign with whichever secret key happens to be its default,
which may not be one Central can verify.

If signed CI publishing is ever wanted, the way to do it is an in-memory
ASCII-armored key (`signing.key` / `signing.password`) from a dedicated project
key held in repository secrets — not a maintainer's personal key.

## From the local Maven repository to Maven Central

`release_build.py` deletes the release version's artifacts from the local
Maven repository, runs `./gradlew publishToMavenLocal -Prelease=true`, and
checks what it published, with
[`release/maven_central.py`](release/maven_central.py):

- every artifact in its `EXPECTED_ARTIFACTS` is there, and no other, each with
  its POM, jar, sources jar, and Javadoc jar, and a signature for every file;
- each POM has the name, description, URL, license, developer, and SCM URL that
  Maven Central requires, and depends on no snapshot, on its own artifacts of
  the same version, and on none of their `org.checkerframework` counterparts;
- every file in the `checker`, `checker-qual`, `checker-util`, and
  `annotation-file-utilities` jars is in the same jar in the release zip
  files, with the same contents;
- every signature verifies with the signer's public key from
  `keyserver.ubuntu.com`, as users and Central see it, not only the local key.

A problem stops the build, before anything is pushed or uploaded.

`release_push.py` (step 5, before anything is pushed) checks the files again and puts exactly those files
into a bundle in the layout of a Maven repository, with an MD5 and SHA-1
checksum of each, which it uploads with the Portal's publisher API:

1. `POST https://central.sonatype.com/api/v1/publisher/upload?publishingType=USER_MANAGED`
   (multipart, field `bundle`) returns the deployment ID.
2. `POST …/api/v1/publisher/status?id=<deployment id>` until `deploymentState`
   is `VALIDATED` (or `FAILED`, with `errors`). The response lists the
   components as `purls`, which must be exactly the expected artifacts at the
   release version.
3. After the push to GitHub (step 6), and after the releaser confirms (step 7),
   `POST …/api/v1/publisher/deployment/<deployment id>` publishes the deployment.
   This cannot be undone. Step 11 waits until its state is `PUBLISHED`.

A rerun of release_push.py resumes with the deployment that an earlier run uploaded. A rerun of
release_build.py rebuilds the artifacts, so it drops that deployment with
`DELETE …/api/v1/publisher/deployment/<deployment id>`.

All requests carry `Authorization: Bearer <base64 of user-token-name:user-token-password>`.

By hand, the bundle (`/tmp/$USER/cf-release/central-bundle-VERSION.zip`) can be
uploaded at <https://central.sonatype.com/publishing/deployments> with "Publish
Component", and the deployment published there with Publish; Drop discards an
unpublished deployment.

`./gradlew publish -Prelease=true` still works, too, through Sonatype's OSSRH
staging API, a compatibility layer for older build plugins. It keeps the upload
in a staging repository that does **not** show up in the Portal until a request
from the uploading machine (the repository is tied to its IP) moves it there:

````text
POST https://ossrh-staging-api.central.sonatype.com/manual/upload/defaultRepository/io.github.eisop?publishing_type=user_managed
Authorization: Bearer <base64 of user-token-name:user-token-password>
````

That path uploads what the build produces at that moment, unchecked, so the
release scripts do not use it.

## Nightly snapshots

Nothing in the build needs to change; what is missing is a scheduled workflow
and credentials. A starting point is in
[`.github/workflows/publish-snapshot.yml`](../../.github/workflows/publish-snapshot.yml),
which is deliberately inert until the secrets exist: it checks for
`SONATYPE_NEXUS_USERNAME` and exits early if it is absent, so merging it
publishes nothing by itself.

Prerequisites, in order:

1. **Enable `-SNAPSHOT` publishing for the `io.github.eisop` namespace** in the
   Central Portal. This is a per-namespace setting, separate from the ability
   to publish releases.
2. Add repository secrets `SONATYPE_NEXUS_USERNAME` and
   `SONATYPE_NEXUS_PASSWORD` — the Portal **user token**, not the account
   password.
3. Run the workflow once with `workflow_dispatch` before trusting the schedule.

Worth knowing: Central Portal snapshots are **deleted after about 90 days**, and
no validation is performed on them. They are for consumers who want to track
master, not an archive.

Consumers then add:

````groovy
repositories {
    maven { url = 'https://central.sonatype.com/repository/maven-snapshots/' }
}
````

which is the same URL this repository already lists in its own
`repositories { ... }` blocks for resolving snapshot dependencies.

## Why no publishing plugin

**Do not adopt an opinionated publishing plugin.** The plugins that speak the
Portal API natively configure publications for you. This build has 18
publications and hand-tuned ones at that: `checker` publishes
`components.shadow` rather than `components.java`, with a comment explaining
that using the latter would ship the skinny jar under the fat jar's name, and
with a Gradle attribute copied onto the shadow configuration by hand.
Re-expressing that inside another plugin's model is real risk for no gain, since
the upload itself already works.

What was considered:

- **`io.github.gradle-nexus.publish-plugin`** — automates the OSSRH
  create/close/release cycle. Built for the OSSRH world that the Portal
  replaced; it would be new machinery pointed at a compatibility layer.
- **`com.vanniktech.maven.publish`** — the most widely used option, supports the
  Portal directly and can poll a deployment to completion. Rejected only because
  of the custom publications above; it would be the natural choice for a project
  whose publications are stock, and is the one to revisit if the publications
  are ever simplified.
- **JReleaser** — Sonatype's own suggestion for Gradle users. Same objection,
  plus it brings a release-orchestration model much larger than the one step
  actually missing here.

## Known gaps

**The published artifacts are only smoke-tested locally.**
`docs/examples/publish-smoketest/` is thorough about the artifacts themselves:
it resolves every published coordinate through its published Gradle module
metadata, asserts that each resolves to its own jar with exactly one
`checker-qual` on the classpath, pins `io.github.eisop` to `mavenLocal()` with a
repository content filter so a previously released artifact of the same version
cannot mask a broken local publish, and type-checks a source set with the Value
Checker loaded out of the published `framework-all` jar. That last part is what
catches a POM missing a dependency the artifact needs at run time, which no
in-repo test can see. It runs against `publishToMavenLocal` output, via
`:checker:exampleTests` in `test-cftests-nonjunit.sh`.  For a release, that is
what reaches Central: `release_push.py` uploads exactly the files that
`release_build.py` published locally and checked.  Snapshots are uploaded by
`./gradlew publish`, so pointing the same smoke test at the published snapshot
repository would still catch a snapshot publication that breaks.

The `annotation-file-utilities` artifact has a check of its own in
`checker/bin-devel/test-release.sh` (the `release` CI job): its POM and jar
contents, and a run of the annotator from the locally published artifact by the
consumer project `docs/examples/afu-consumer/`.

**The website is generated from the GitHub releases.** The "Publish website"
workflow of the `eisop.github.io` repository runs `EisopSiteGenerator` daily;
`release_push.py` asks to run it right after the GitHub release is posted, and
waits until the site shows the release.

## Open questions

- Should the nightly job run only when master has moved since the last snapshot?
  Publishing an identical snapshot daily costs little, but it does churn the
  90-day window.
