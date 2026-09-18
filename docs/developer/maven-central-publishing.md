# Publishing to Maven Central

How publishing is wired, what a releaser still does by hand, and what it would
take to automate the rest.

## What the build does today

Publishing is plain `maven-publish` + `signing`, configured in
[`gradle-mvn-push.gradle`](../../gradle-mvn-push.gradle) — no third-party
publishing plugin. The namespace is `io.github.eisop`.

| | destination | signed |
| --- | --- | --- |
| `-SNAPSHOT` version | `https://central.sonatype.com/repository/maven-snapshots/` | no |
| release version | `https://ossrh-staging-api.central.sonatype.com/service/local/staging/deploy/maven2/` | yes |

A release is made with

````bash
./gradlew publish -Prelease=true --no-parallel
````

which uploads to a staging repository. The release is **not** live until a human
opens <https://central.sonatype.com/publishing/deployments> and clicks Publish.

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

## Why the manual click exists, and how to remove it

`ossrh-staging-api.central.sonatype.com` is Sonatype's **compatibility layer**
for publishers whose build plugins predate the Central Portal API. It accepts
the old OSSRH staging upload, and then leaves the deployment sitting in the
Portal for a human to release.

The compatibility service has its own API for that last step, so the click is
avoidable without changing how the build uploads anything:

````
POST https://ossrh-staging-api.central.sonatype.com/manual/upload/defaultRepository/io.github.eisop?publishing_type=automatic
Authorization: Bearer <base64 of user-token-name:user-token-password>
````

`publishing_type=automatic` uploads the staging repository to the Portal and
releases it to Maven Central if validation passes. `publishing_type=portal_api`
does the upload but leaves it for a status poll, which is the better choice for
a large deployment.

There is also a native Portal API — `POST /api/v1/publisher/upload` with
`publishingType=AUTOMATIC` — but it takes a pre-built bundle, which is a
different upload path than the build currently uses.

So the recommended change is small: keep `./gradlew publish -Prelease=true`
exactly as it is, and follow it with the one `POST` above. That is the whole
difference between today's release and a hands-off one.

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
in-repo test can see. But it runs against `publishToMavenLocal` output, via
`:checker:exampleTests` in `test-cftests-nonjunit.sh` — so it verifies the
artifacts the build *would* publish, not that what reached Central is what the
build produced. Once nightly snapshots exist, pointing the same smoke test at
the published snapshot repository would close that gap, and would fail on the
day a publication breaks rather than at the next release.

**Nothing publishes the website automatically.** `EisopSiteGenerator` is run by
hand against a `gh-pages` checkout, as its README in the `eisop.github.io`
repository describes, and the release scripts here do not mention it, so
"re-run the website generator" belongs in the release checklist.

## Open questions

- Should the nightly job run only when master has moved since the last snapshot?
  Publishing an identical snapshot daily costs little, but it does churn the
  90-day window.
- Should a release use `publishing_type=automatic` or `portal_api` plus a status
  poll? Automatic is one call; a poll gives a build log that says whether
  validation passed, rather than an email later.
