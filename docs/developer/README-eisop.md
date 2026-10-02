# EISOP Development Notes

## Updating from a different fork

To update EISOP with the changes of a different Checker Framework fork (usually a typetools release),
use two pull requests: the first contains the external changes and nothing else, the second contains
the eisop-specific fixes.  Each typetools release is imported by itself, in release order.

### Policy

- Import **everything** that typetools changed, including CI configuration, WPI, Markdown lint, and
  the "Prep for release" commits.  A commit that does nothing in our tree, because we are newer or
  already did the same, is still imported, as an empty commit (`git cherry-pick --allow-empty` or
  `git commit --allow-empty -C <commit>`).  A skipped commit is never offered again by the next
  release's range, and every skipped file makes later imports harder.
- Skip only what would break our CI in the first pull request: dependency downgrades where we are
  newer, changes to JDK selection, and removal of our test-parallelism settings.  Name each skip and
  the reason in the description of the pull request.
- Adapt and clean up in the second pull request, not the first.

### Steps

1. Pull in eisop/master and make sure you don't have any uncommitted files.

1. Create a new branch, named e.g. `typetools-3.18.0-fixes`, and push to eisop, without any changes.

1. Create a new branch, named e.g. `typetools-3.18.0-merge`.

1. If necessary, change to consistent formatting:
   - Remove `.aosp()` from `build.gradle`.
   - Run `./gradlew spotlessApply` and commit results as e.g. `Change to typetools formatting`.

1. Look up the commit IDs for the range you want to include, e.g. previous and current releases
   (tags `checker-framework-X.Y.Z`).  Fetch them: `git fetch typetools --tags`.

1. Do `git cherry-pick -Xignore-space-change fromID..toID`.
   Resolve conflicts toward typetools' text, but keep eisop's identity: eisop URLs and
   organizations, `io.github.eisop` coordinates, and our release scripts.  Continue with
   `git cherry-pick --continue`.

1. Check that nothing is missing.  The range is long and a script that treats a commit subject as
   "done" skips a second commit with the same subject, so compare authors and subjects:

   ```bash
   git log --no-merges --format='%an|%s' fromID..toID | LC_ALL=C sort > upstream.txt
   git log --no-merges --format='%an|%s' master..HEAD | LC_ALL=C sort > ours.txt
   LC_ALL=C comm -23 upstream.txt ours.txt
   ```

   Every line printed is a commit that still has to be imported or listed as a skip.  Also
   search the tree for conflict markers.

1. If necessary, undo formatting changes and commit `Change back to AOSP formatting`.

1. Open a pull request (against eisop) merging `typetools-3.18.0-merge` into `typetools-3.18.0-fixes`.
   The description lists the skips and, as the only trailers, one `Co-authored-by:` line for each
   typetools author, computed from `git log` rather than typed from memory.  Once this looks OK,
   squash and merge titled `typetools/checker-framework x.y.z release`, making sure to keep all
   authors.

1. Go through all changes in more detail and clean up any problems.
   This two-step process gives us one commit with the external changes and separate commits with
   eisop-specific changes and enhancements.  Typical clean-up:
   - Files that typetools deleted but our checks still use (for example `.ruff.toml`).
   - Names that typetools changed that we prefer to keep, so that our fork stays closer to its history.
   - References to typetools repositories that must point to eisop repositories (for example
     `eisop/jdk` in `docs/manual`).
   - CHANGELOG sections: take typetools' text if the words are identical apart from formatting, and
     keep sections with eisop edits.
   - Commits skipped in the first pull request, and anything that makes `make style-check`,
     `./gradlew spotlessCheck`, or `./gradlew requireJavadoc javadocDoclintAll` fail.

1. Open a pull request (against eisop) merging `typetools-3.18.0-fixes` into `master` and
   merge **without** squashing, with a merge commit titled
   `typetools/checker-framework x.y.z release (#NNNN)` and an empty body.

### Companion repositories

Some changes need a matching change in another repository.  CI clones the companion repository
with the same branch name as the pull request, falling back to `master`: `eisop/jdk`
(`typetools/jdk` changes), `eisop-codespecs/daikon`, and the `eisop-plume-lib` repositories.
Create the branches under the same name, merge in the order Daikon, then Checker Framework, then JDK,
and expect a failure in a companion job to be real.

`eisop/jdk` `master` stays on JDK 17.  Do not merge `typetools/jdk` `master` into it, because that is
JDK 21; merge it into the `jdk-21` branch instead.

### CI

- With `fail-fast`, one failing job cancels most of the others; look for the single job that
  failed.
- A Maven Central HTTP 429 is a flake; re-run the job.
- Do not force-push, and do not rewrite pushed commits, without an explicit OK.

## Changelog

Each entry goes under the next release section of
[`docs/CHANGELOG.md`](../CHANGELOG.md), in the same PR as the change it
describes. A PR that closes an issue adds its number to that section's
`### Closed issues` list, rather than leaving it for a later backfill.

While a release is unreleased, that list is written **one issue per line**:

````text
eisop#2089,
eisop#2095,
typetools#399,
typetools#3203.
````

Adding a number then touches only the line it adds, so two PRs in flight merge
cleanly unless they happen to insert at the very same point. Written as a
filled paragraph, adding one number reflows the whole paragraph and *every*
concurrent pair conflicts -- which, in one busy week, meant resolving the same
conflict by hand seven times.

Markdown joins lines within a paragraph, so both forms render identically.
Reflowing the list to filled lines when a release is finalized is therefore
optional tidiness, not a required step: a released section left one per line is
correct as it stands.

Entries stay in ascending numeric order, `eisop#NNNN` before `typetools#NNNN`.
Refer to another project's issue in prose as plain text -- "typetools issue
2816" -- never as a link or as `typetools/checker-framework#2816`, both of
which make GitHub post a cross-reference into that project's tracker.

## Release process

See [`maven-central-publishing.md`](maven-central-publishing.md) for how
publishing is wired, why a release currently ends with a manual click on the
Central Portal website, and what it would take to remove that step and to
publish nightly snapshots.

TODO: the release process contains many buffalo-specific paths, which still needs to be cleaned up.
Most of the instructions can be followed, ignoring certain steps.

Without using the release scripts, you can make a Maven Central release using:

````bash
./gradlew publish -Prelease=true --no-parallel
````

The build signs releases and refuses to publish unsigned ones.  Any maintainer
may sign with their own key: put `signing.gnupg.keyName=<your key id or email>`
in `~/.gradle/gradle.properties`, next to your `SONATYPE_NEXUS_USERNAME` and
`SONATYPE_NEXUS_PASSWORD` Portal tokens.  The publish fails with an explicit
message if that property is unset.  See
[`maven-central-publishing.md`](maven-central-publishing.md#signing-any-maintainer-can-sign-a-release)
for what a new releaser has to set up.

That uploads to a staging repository. The release is **not** live until it is
published from the Central Portal, which today means opening
<https://central.sonatype.com/publishing/deployments> and clicking Publish.
[`maven-central-publishing.md`](maven-central-publishing.md) describes the
single API call that would remove that step.

The version is `releaseVersion` in `release.gradle`.  A build's version is
`releaseVersion` followed by `-SNAPSHOT`, unless Gradle is run with
`-Prelease=true` as above; so `./gradlew publish` without it publishes a
snapshot.

A release also updates the version in files other than `release.gradle` --
the front page, the quick-start page, and several places in the manual -- so
that what readers are told to depend on is the version just released.  That is
done by the Gradle task `updateVersionNumbers` in `release.gradle`, which lists
the files it rewrites and fails if one of them has moved.  The
`docs/examples/` versions are bumped by Renovate once the release is on Maven
Central.

If there are problems with the configuration cache, pass `--no-configuration-cache`.

You may need to run `gpg-agent` first and enter the GPG password when prompted.

Use `--warning-mode all` to see gradle deprecation warnings.
