# Publishing to Maven Central

Notes on how this project publishes, what is still manual, and what it would
take to automate. Written up in response to two asks: nightly snapshots, and
not having to finish every release by hand on the Central Portal website.

## What the build does today

Publishing is plain `maven-publish` + `signing`, configured in
[`gradle-mvn-push.gradle`](../../gradle-mvn-push.gradle) — 37 lines, no
third-party publishing plugin. The namespace is `io.github.eisop`.

| | destination | signed |
| --- | --- | --- |
| `-SNAPSHOT` version | `https://central.sonatype.com/repository/maven-snapshots/` | no |
| release version | `https://ossrh-staging-api.central.sonatype.com/service/local/staging/deploy/maven2/` | yes |

A release is made with

````bash
./gradlew publish -Prelease=true --no-parallel -Psigning.gnupg.keyName=...
````

which uploads to a staging repository, and then **a human opens
central.sonatype.com and clicks Publish**. That click is the annoyance.

Two things already work in our favour:

- The snapshot URL is already the Central Portal snapshot repository, so
  snapshot publishing needs no new publishing configuration at all.
- Snapshots are deliberately unsigned (`tasks.withType(Sign) { onlyIf { !isSnapshot && ... } }`),
  so a snapshot job needs no GPG key in CI.

## Why the manual click exists

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

## Recommendation: keep the build, automate the last step

**Do not adopt an opinionated publishing plugin.** The plugins that speak the
Portal API natively (`com.vanniktech.maven.publish`, the various
`sonatype-central-portal-publisher` forks, JReleaser) configure publications
for you. This build has 18 publications and hand-tuned ones at that: `checker`
publishes `components.shadow` rather than `components.java`, with a comment
explaining that using the latter would ship the skinny jar under the fat jar's
name, and with a Gradle attribute copied onto the shadow configuration by hand.
Re-expressing that inside another plugin's model is real risk for no gain,
since the upload itself already works.

The smaller change gets the same result:

1. Keep `./gradlew publish -Prelease=true` exactly as it is.
2. Follow it with the one `POST` above.

That is the whole difference between today's release and a hands-off one.

## Nightly snapshots

Nothing in the build needs to change. What is missing is a scheduled workflow
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

Worth knowing: Central Portal snapshots are **deleted after about 90 days**,
and no validation is performed on them. They are for consumers who want to
track master, not an archive.

Consumers then add:

````groovy
repositories {
    maven { url = 'https://central.sonatype.com/repository/maven-snapshots/' }
}
````

which is the same URL this repository already lists in its own
`repositories { ... }` blocks for resolving snapshot dependencies.

## What was not adopted, and why

- **`io.github.gradle-nexus.publish-plugin`** — automates the OSSRH
  create/close/release cycle. Built for the OSSRH world that the Portal
  replaced; it would be new machinery pointed at a compatibility layer.
- **`com.vanniktech.maven.publish`** — the most widely used option, supports
  the Portal directly and can poll a deployment to completion. Rejected only
  because of the custom publications above; it would be the natural choice for
  a project whose publications are stock.
- **JReleaser** — Sonatype's own suggestion for Gradle users. Same objection,
  plus it brings a release-orchestration model much larger than the one step
  actually missing here.

If the publications are ever simplified, `com.vanniktech.maven.publish` is the
one to revisit.

## Open questions

- Should the nightly job run only when master has moved since the last
  snapshot? Publishing an identical snapshot daily costs little, but it does
  churn the 90-day window.
- Should a release use `publishing_type=automatic` or `portal_api` plus a
  status poll? Automatic is one call; a poll gives a build log that says
  whether validation passed, rather than an email later.
