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
./gradlew publish -Prelease=true --no-parallel
````

which uploads to a staging repository, and then **a human opens
central.sonatype.com and clicks Publish**. That click is the annoyance.

Two things already work in our favour:

- The snapshot URL is already the Central Portal snapshot repository, so
  snapshot publishing needs no new publishing configuration at all.
- Snapshots are deliberately unsigned (`tasks.withType(Sign) { onlyIf { !isSnapshot && ... } }`),
  so a snapshot job needs no GPG key in CI.

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

The key belongs in the same per-user file, not on the command line and not in
this repository:

```properties
signing.gnupg.keyName=<your key id or email>
```

The release command then names no key at all:

```bash
./gradlew publish -Prelease=true --no-parallel
```

`gradle-mvn-push.gradle` calls `useGpgCmd()`, so signing goes through the local
`gpg` (and `gpg-agent`, so the passphrase is entered once rather than once per
artifact). It now also fails with an explicit message if
`signing.gnupg.keyName` is unset on a release publish: without it `gpg` would
quietly sign with whichever secret key happens to be its default, which may not
be one Central can verify.

Snapshots are unsigned here, so the nightly workflow needs no key. If signed
CI publishing is ever wanted, the way to do it is an in-memory ASCII-armored
key (`signing.key` / `signing.password`) from a dedicated project key held in
repository secrets — not a maintainer's personal key.

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

## Other things worth fixing in the release process

Found while tracing the above; none is caused by the Portal migration alone.

### `release_push.py` described the retired Nexus UI (fixed here)

Steps 5b and 5c told the releaser to

> click on iogithubeisop-XXXX … Click "close" at the top … Copy the URL of the
> closed artifacts (in the bottom pane)

and then paste that URL back into the script. That is the OSSRH Nexus staging
UI, with its top and bottom panes and its close-then-release two-step. The
Central Portal has neither: a deployment is validated and then published, in
one step. Anyone following the script got stuck looking for a button that is
gone.

Worse, the staging step it ran was dead on arrival for eisop: it read the
signing passphrase from `/projects/swlab1/checker-framework/hosting-info/`, a
University of Washington path that does not exist here, and signed with
`checker-framework-dev@googlegroups.com` rather than the releaser's own key.

Since Maven Central publishing is in practice a separate `./gradlew publish`
invocation, this PR removes those steps from `release_push.py` rather than
rewording them, and renumbers the remaining GitHub/website steps. Removing
step 5c also retired the only caller that passed a staging repository URL, so
`maven_sanity_check`'s `repo_url` parameter and `add_repo_information` — which
still edited poms to point at `org/checkerframework` artifacts — went with it.

### The version-bump automation had silently drifted (fixed here)

`build.gradle` holds the authoritative version, and the last released version is
also written into a number of other tracked files. That bump is *not* manual:
the Ant target `update-checker-framework-versions` in
`docs/developer/release/release.xml` does it. But the target had drifted from
the tree, and it failed quietly.

Four of its file references no longer resolved:

| Referenced | Actual |
| --- | --- |
| `docs/manual/checkerframework.gradle` | gone |
| `build-common.properties` | gone |
| `checker/build.properties` | gone |
| `docs/manual/checker-framework-quick-start.html` | the file is at `docs/checker-framework-quick-start.html` |

Ant's `<replaceregexp>` — which the target's `update` macro also wraps — only
prints `The following file is missing: …` for a path that does not exist; the
build still reports `BUILD SUCCESSFUL`. So nothing ever flagged this. The
symptom is in the tree: `docs/checker-framework-quick-start.html` still
advertised `checker-framework-2.1.7.zip`, many releases later, because the
target had been rewriting a path that does not exist. (No reader saw that —
see [Two web pages nothing publishes](#two-web-pages-nothing-publishes) — but
it is an accurate gauge of how long the breakage went unnoticed.)

There is a second way the same target skips silently. Most rewrites key off a
marker comment (`<!-- checker-framework-version -->` and friends, defined in
`release.properties`), and if a page drops or renames a marker the regex simply
matches nothing. Two rules were in that state: the `checker-framework-version`
and `compiler-version` updates to `docs/checker-framework-webpage.html`, neither
of which marker exists on that page any more.

This PR therefore:

- drops the three rules for deleted files and the two rules for absent markers;
- points `checkerQuickStartPage` at the real location, and brings
  `docs/checker-framework-quick-start.html` up to the current release;
- adds `require-file` and `require-marker` macros that assert, before anything
  is rewritten, that every file exists and every marker is present.

Verified against a scratch copy, with the real AFU `build.properties`: on the
happy path all five files are rewritten and the AFU date and zip name update;
removing a file gives `BUILD FAILED ... file to update does not exist`, and
renaming a marker gives `BUILD FAILED ... marker not found`. Both abort before
any file is changed.

The `docs/examples/` builds are no longer touched by the target at all —
see [Who bumps the version in the examples](#who-bumps-the-version-in-the-examples).
(`docs/developer/performance-notes.md` also names a version, but it cites the
release a measurement was taken on, so it should not be bumped. `BazelExample`
is a further exception: its files carry checksums, so it is re-pinned instead.)

### Who bumps the version in the examples

Renovate owns this, and the release scripts now stay out of it. Two things had
to change for that to actually work.

**The two mechanisms were fighting.** Renovate rewrites a pom property in place
and drops any marker comments around it — it did exactly that to
`MavenExample-framework-all/pom.xml` in #2017, turning
`<checkerFrameworkVersion><!-- checker-framework-version -->…` into a plain
value. The Ant target keyed off those same markers, both to bump
`MavenExample/pom.xml` at release time and, in `update-and-copy-maven-example`,
to set the version on the copy used by the Maven sanity check. Had Renovate
reached `MavenExample/pom.xml` first, that sanity check would have gone on
compiling against the *previous* release and still passed. So
`update-and-copy-maven-example` now matches the `<checkerFrameworkVersion>`
element itself rather than marker comments, and the vestigial markers are gone.

**Renovate probably could not see the Gradle examples.** Those builds hold the
version in a variable — an `ext.versions` map entry, or a plain `def` in
`eisop-errorprone`. Every Renovate edit ever made to these files was a *plugin*
version (`id 'net.ltgt.errorprone' version '…'`); the eisop version in them was
last bumped by Dependabot, which was removed in #1997. Rather than guess at
Renovate's Gradle variable resolution, `renovate.json` now carries an explicit
`customManagers` regex for `eisopVersion`, matching the repository's existing
practice for Bazel, the JDK EA build, and ruff. Verified that its file pattern
and both match strings select all five example builds and extract the current
version.

Finally, the repository-wide `minimumReleaseAge: 7 days` made no sense for our
own artifact: it delayed the examples a week behind every release. A package
rule now sets `minimumReleaseAge: 0 days` for `io.github.eisop:*` and gives it
its own PR rather than the grouped `docs/examples` one.

### The release zip shipped a broken example and no README

`checker-includes`, the list of paths that go into `checker-framework-X.Y.Z.zip`,
had drifted the same way, and Ant does not complain about an include pattern
that matches nothing either.

- `README.html` was renamed to `README.txt` in 2015 and is now `README.md`. The
  list still said `README.html`, so **no README shipped in the distribution at
  all.**
- "Make class name and file name the same" (2020) moved the units-extension
  qualifiers into `qual/` and renamed `Demo.java` to `UnitsExtensionDemo.java`.
  The list still named the old paths, so the example shipped with its
  `Makefile`, `README` and `Expected.txt` — and **not one Java source file**.
  The shipped `Makefile` refers to `qual/Frequency.java` and friends, none of
  which are there.

Confirmed on the published site rather than inferred. Note that the site's
newest release is 3.49.3-eisop1, not 3.49.5-eisop1 — see below — and both
renames long predate it. Under
`/cf/checker-framework-3.49.3-eisop1/examples/units-extension/`, `README` is
200 while `UnitsExtensionDemo.java` is 404; `README.md` is 404 at both the
release folder and `/cf/`. Both entries are corrected here.

### Two web pages nothing published

`docs/checker-framework-webpage.html` and `docs/checker-framework-quick-start.html`
are rewritten by `update-checker-framework-versions`, and the webpage is copied
by the `checker-framework-website-docs` target into the interm site directory,
where a symlink makes it the site's `index.html`. Nothing published that
directory for eisop:

- neither file was in `checker-includes`, so neither shipped in the release zip;
- `site-copy-includes` covers only `annotation-file-utilities/**` and the JSR 308
  specification;
- `DEV_SITE_DIR` / `LIVE_SITE_DIR` are local `/tmp/$USER` directories, and
  `https://eisop.github.io/cf/dev` is a 404;
- `https://eisop.github.io/cf/index.html` is generated by `EisopSiteGenerator`
  in the `eisop.github.io` repository from `cf-template.md`, filled in from the
  GitHub releases API — not from either of these files.

Confirmed: `/cf/checker-framework-webpage.html` and
`/cf/checker-framework-quick-start.html` are both 404 on the live site.

**The main page has already been ported.** `cf-template.md` is a Markdown
rendering of `checker-framework-webpage.html`, with the same headline, the same
introductory paragraphs word for word, the same bullet structure and the same
"Support and community" / "Bug reports" / "Mailing lists" sections. Comparing
the two, every difference in link targets is just the hosting layout
(`manual/checker-framework-manual.pdf` versus `manual/manual.pdf`, `api` versus
`api/checker-javadoc/`, `annotation-file-utilities/` versus `../afu/`). One
difference was content, not layout: the port dropped the **Dataflow Framework**
bullet. That is restored in eisop/eisop.github.io#103.

The better resolution is the other direction, and it is being done in a
follow-up PR: **ship this page in the release zip** and let the website use it
as that release's page, retiring `cf-template.md`.

One template shared across every release is a standing mismatch — a link added
for content that arrives in release N is broken on the archived page of every
release before N, permanently, not just until the next release. A page that
travels with the release it describes cannot have that problem, and this file is
already exactly that page: `release.xml` stamps the Checker Framework version
and date *and* the AFU zip name and date into it, so it is self-contained at
release time. It also has the Dataflow Framework bullet that the port to
`cf-template.md` dropped.

What that PR has to do is add the file to `checker-includes` and align its links,
which are still the old typetools site layout (`manual/checker-framework-manual.pdf`,
`api`, `annotation-file-utilities/`), to the layout the zip and the website
share. It has to merge before the next release, since only a release built after
it can carry the page.

**The quick-start guide is now shipped and surfaced.** It had no counterpart on
the site at all. Rather than port it to a second website template, this PR adds
it to `checker-includes`, so it travels in the release zip like the manual, the
tutorial and the CHANGELOG, and stays version-correct through the same Ant
target that already rewrote it. eisop/eisop.github.io#103 lifts it to
`cf/quick-start.html` using the same three-step pattern the generator already
applies to `docs/CHANGELOG.md`, and links it from the front page. Its
`https://eisop.github.io/cf/manual/#anchor` links — which worked only through
the 404 page's JavaScript redirect — are now site-relative, so they resolve
directly both at `cf/quick-start.html` and in an archived
`cf/<release>/quick-start.html`.

### The dataflow manual was built every release and thrown away

`release_build.py` runs `make` in `dataflow/manual`, producing `dataflow.pdf`,
and `checker-framework-website-docs` copies it to the interm site directory as
`checker-framework-dataflow-manual.pdf` — the exact name the Dataflow bullet on
the old webpage linked to. Since that directory is never published, the PDF was
built and discarded at every release, and the link was dead.

It is now added to the release zip, placed at
`docs/manual/checker-framework-dataflow-manual.pdf` via a `fullpath` zipfileset
(the same mechanism already used for `CFLogo.png`). The generator lifts
`docs/manual` wholesale, so it lands at
`cf/manual/checker-framework-dataflow-manual.pdf` with no generator change, and
the restored bullet points at it.

### The website is a release behind

`https://eisop.github.io/cf/` still offers `checker-framework-3.49.3-eisop1.zip`
(released 6 May 2025). 3.49.5-eisop1 was published on 26 Apr 2026 and does not
appear on the site at all: `/cf/checker-framework-3.49.5-eisop1/` is a 404, and
the release archive lists 28 entries against the 29 the API reports.

Nothing publishes the site automatically — `EisopSiteGenerator` is run by hand
against a `gh-pages` checkout, as its README describes, and the release scripts
here do not mention it. Whatever else is decided above, "re-run the website
generator" belongs in the release checklist.

### The published artifacts are only smoke-tested locally

`docs/examples/publish-smoketest/` is thorough about the artifacts themselves.
It resolves every one of the eleven published coordinates — plus
`framework-errorprone` when the build JDK is 21 or newer — through its published
Gradle module metadata, asserts that each resolves to its own jar with exactly
one `checker-qual` on the classpath, pins `io.github.eisop` to `mavenLocal()`
with a repository content filter so a previously released artifact of the same
version cannot mask a broken local publish, and type-checks a source set with
the Value Checker loaded out of the published `framework-all` jar. That last
part is what catches a POM missing a dependency the artifact needs at run time,
which no in-repo test can see.

What it does not check is the *deployment*: it runs against
`publishToMavenLocal` output, via `:checker:exampleTests` in
`test-cftests-nonjunit.sh`. So it verifies the artifacts the build would
publish, not that what reached Central is what the build produced.

Once nightly snapshots exist, pointing the same smoke test at the published
snapshot repository would close that gap, and would fail on the day a
publication breaks rather than at the next release.

## Open questions

- Should the nightly job run only when master has moved since the last
  snapshot? Publishing an identical snapshot daily costs little, but it does
  churn the 90-day window.
- Should a release use `publishing_type=automatic` or `portal_api` plus a
  status poll? Automatic is one call; a poll gives a build log that says
  whether validation passed, rather than an email later.
