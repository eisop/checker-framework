# EISOP Development Notes

## Updating from a different fork

To update EISOP with changes in a different Checker Framework fork, follow these steps:

1. Pull in eisop/master and make sure you don't have any uncommitted files.

1. Create a new branch, named e.g. `typetools-3.18.0-fixes`, and push to eisop, without any changes.

1. Create a new branch, named e.g. `typetools-3.18.0-merge`.

1. If necessary, change to consistent formatting:
    - Remove `.aosp()` from `build.gradle`.
    - Run `./gradlew spotlessApply` and commit results as e.g. `Change to typetools formatting`.

1. Look up the commit IDs for the range you want to include, e.g. previous and current releases.

1. Fetch the new release into a different branch `git fetch typetools toID:typetools-3.18.0-release`.

1. Do `git cherry-pick fromID..toID`.

1. If there are conflicts, resolve and do `git cherry-pick --continue`.

1. If necessary, undo formatting changes and commit `Change back to AOSP formatting`.

1. Open a pull request (against eisop) merging `typetools-3.18.0-merge` into `typetools-3.18.0-fixes`.
  Once this looks OK, squash and merge titled `typetools/checker-framework x.y.z release`, making
  sure to keep all authors.

1. Go through all changes in more detail and clean up any problems.
  This two-step process gives us one commit with the external changes and separate commits with
  eisop-specific changes and enhancements.

1. Open a pull request (against eisop) merging `typetools-3.18.0-fixes` into `master` and
  merge without squashing.

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

A release also updates the version in files other than `build.gradle` -- two of
the examples under `docs/examples/`, and several places in the manual -- so that
what readers are told to depend on is the version just released.  That is done
by the Ant target `update-checker-framework-versions` in
`docs/developer/release/release.xml`, which lists the files it rewrites and
fails if one of them has moved.  The remaining `docs/examples/` versions are
bumped by Renovate once the release is on Maven Central.

If there are problems with the configuration cache, pass `--no-configuration-cache`.

You may need to run `gpg-agent` first and enter the GPG password when prompted.

Use `--warning-mode all` to see gradle deprecation warnings.
