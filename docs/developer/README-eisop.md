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

### Naming divergences from typetools

Where this fork names or defines something differently from typetools, the table below records it, so
that a cherry-picked line that uses typetools' name is recognized as needing adaptation.  The source
is `docs/developer/typetools-divergences.tsv`; regenerate the table with
`checker/bin-devel/typetools-import-check.py --markdown`.  While importing, run
`checker/bin-devel/typetools-import-check.py --range FROM..TO` to list the added lines that use a
name from the table, and `--api-diff TAG` to find typetools-only method names nobody has written down.
Rows of kind `file` record a typetools file whose code is in other files here, for example because
this fork split a class, so that a hunk for that file is applied to the right files; `--hunks`
(below) uses them.

<!-- BEGIN typetools-divergences (generated by typetools-import-check.py) -->

| Kind | Scope | Typetools | EISOP | Since | Why |
| --- | --- | --- | --- | --- | --- |
| collision | AnnotatedTypeMirror | `getAnnotation(` | `getEffectiveAnnotation(` | 3.55.0 | typetools 3.55.0 (#7594) renamed getEffectiveAnnotation*, hasEffectiveAnnotation*, getEffectiveAnnotations to getAnnotation*, hasAnnotation*, getAnnotations. Here getAnnotation*, hasAnnotation*, and getAnnotations() keep meaning the annotations on that type itself, as in javac. |
| collision | AnnotatedTypeMirror | `getAnnotations()` | `getEffectiveAnnotations()` | 3.55.0 | See getAnnotation(: the plain name means the annotations on the type itself, as in javac. |
| collision | AnnotatedTypeMirror | `hasAnnotation(` | `hasEffectiveAnnotation(` | 3.55.0 | See getAnnotation(. |
| rename | AnnotatedTypeMirror | `getPrimaryAnnotation(` | `getAnnotation(` | 3.36.0 | Typetools named the annotations on the type itself getPrimaryAnnotation*; this fork undid that renaming in October 2023 to stay close to javac. |
| rename | AnnotatedTypeMirror | `getPrimaryAnnotations(` | `getAnnotations(` | 3.36.0 | See getPrimaryAnnotation(. |
| rename | AnnotatedTypeMirror | `hasPrimaryAnnotation(` | `hasAnnotation(` | 3.36.0 | See getPrimaryAnnotation(. |
| rename | AnnotatedTypeMirror | `getPrimaryAnnotationInHierarchy(` | `getAnnotationInHierarchy(` | 3.36.0 | See getPrimaryAnnotation(. |
| api | AnnotatedTypeFactory | `canonicalAnnotation(` | `canonicalAnnotation(` | 3.54.0 | Both return their argument when it is not an alias (this fork adopted typetools 3.54.0 semantics and removed its own canonicalAnnotationOrWritten, which was never released). |
| api | TestDiagnostic | `error key in [brackets]` | `error key in (parentheses) or [brackets]` | 3.54.0 | Typetools 3.54.0 writes expected error keys as [key]; the test harness here accepts both forms, so tests need not be converted. Typetools' rework of TestDiagnostic (message to key, -Aonelinemsg instead of -Anomsgtext) is not imported. |
| api | build.gradle | `CloneOrUpdateRelatedTask` | `CloneOrUpdateRelatedTask` | 3.54.0 | Adopted, with eisop as the default organization and the root directory as a task property for the configuration cache. |
| rename | AnnotatedTypeMirror | `*Primary*` | `the same name without "Primary"` | 3.36.0 | The whole family of typetools methods with Primary in the name; see getPrimaryAnnotation(. |
| api | javacutil, dataflow, framework | `@Deprecated(since = "date")` | `@Deprecated // date` | 3.55.0 | The modules are compiled for Java 8, where the since element does not exist; typetools' #7572 converted the comments to it. Write the date as a comment. |
| api | Node | `setIsLoopCondition(boolean), getIsLoopCondition()` | `same` | 3.55.0 | Adopted with typetools' Value Checker widening. The flag costs no memory (it fits in the padding of the object header) and nothing at run time; computing it from tree paths in ValueTransfer would cost more. |
| api | ValueTransfer | `try/catch (Exception) around values for -Acfgviz=...,verbose` | `-` | 3.55.0 | Not adopted: typetools' workarounds catch every exception; the crash does not occur here (112 Value and 70 Index test files run with verbose cfgviz). |
| api | docs | `docs/.htaccess` | `(removed)` | 3.55.1 | Apache redirects for checkerframework.org on the CSE web server; eisop.github.io is on GitHub Pages and ignores .htaccess. |
| api | checker/tests/nullness-extra | `issue502 on all JDKs` | `issue502 on JDK 17 and later` | 3.55.1 | Its expected javap output holds a constant-pool index that differs between JDK 8, 11, and 17+. |
| file | Initialization Checker | `checker/src/main/java/org/checkerframework/checker/initialization/InitializationAnnotatedTypeFactory.java` | `InitializationParentAnnotatedTypeFactory.java, InitializationAnnotatedTypeFactory.java, InitializationFieldAccessTreeAnnotator.java` | 3.39.0-eisop1 | This fork split the class when it separated the Initialization Checker from the Nullness Checker. The abstract superclass InitializationParentAnnotatedTypeFactory, shared with the field-access subchecker, holds the type and tree annotators, the qualifier hierarchy, and the type-frame helpers; the subclass InitializationAnnotatedTypeFactory holds the parts that use the flow analysis; InitializationFieldAccessTreeAnnotator types field accesses. Typetools' field-invariant methods have no counterpart. The import of typetools 3.51.1 dropped the source hunk of fc3a55d3521b because of this split. |
| file | Nullness Checker | `checker/src/main/java/org/checkerframework/checker/nullness/NullnessAnalysis.java` | `NullnessNoInitAnalysis.java` | 3.39.0-eisop1 | Renamed when the Initialization Checker became a separate checker: the Nullness Checker runs NullnessNoInitSubchecker, which checks nullness without initialization. |
| file | Nullness Checker | `checker/src/main/java/org/checkerframework/checker/nullness/NullnessAnnotatedTypeFactory.java` | `NullnessNoInitAnnotatedTypeFactory.java` | 3.39.0-eisop1 | See NullnessAnalysis.java. |
| file | Nullness Checker | `checker/src/main/java/org/checkerframework/checker/nullness/NullnessStore.java` | `NullnessNoInitStore.java` | 3.39.0-eisop1 | See NullnessAnalysis.java. |
| file | Nullness Checker | `checker/src/main/java/org/checkerframework/checker/nullness/NullnessTransfer.java` | `NullnessNoInitTransfer.java` | 3.39.0-eisop1 | See NullnessAnalysis.java. |
| file | Nullness Checker | `checker/src/main/java/org/checkerframework/checker/nullness/NullnessValue.java` | `NullnessNoInitValue.java` | 3.39.0-eisop1 | See NullnessAnalysis.java. |
| file | Nullness Checker | `checker/src/main/java/org/checkerframework/checker/nullness/NullnessVisitor.java` | `NullnessNoInitVisitor.java` | 3.39.0-eisop1 | See NullnessAnalysis.java. |
| file | Nullness Checker | `checker/src/main/java/org/checkerframework/checker/nullness/NullnessAnnotatedTypeFormatter.java` | `-` | 3.49.5-eisop1 | Removed here when the type of the null literal became NullType; this fork prints null literals with their annotations. |
| file | Report Checker | `framework/src/main/java/org/checkerframework/common/util/count/report/` | `framework/src/main/java/org/checkerframework/common/util/report/` | 3.42.0-eisop1 | Typetools moved the Report Checker to common.util.count.report; this fork keeps it in common.util.report. |
| file | Report Checker | `checker-qual/src/main/java/org/checkerframework/common/util/count/report/qual/` | `checker-qual/src/main/java/org/checkerframework/common/util/report/qual/` | 3.42.0-eisop1 | See the Report Checker row above. |
| file | TypeInformationPresenter | `framework/src/main/java/org/checkerframework/framework/util/TypeInformationPresenter.java` | `framework/src/main/java/org/checkerframework/framework/util/visualize/` | 3.42.0-eisop3 | This fork made the class reusable: an interface TypeInformationPresenter, the implementation in AbstractTypeInformationPresenter, and the message kind and range as TypeOccurrenceKind and TypeOccurrenceRange. |
| file | AnnotationFormatter | `javacutil/src/main/java/org/checkerframework/javacutil/AnnotationFormatter.java` | `framework/src/main/java/org/checkerframework/framework/util/AnnotationFormatter.java` | 3.21.2-eisop1 | This fork moved the annotation formatters from javacutil to framework/util; typetools keeps them in javacutil. |
| file | AnnotationFormatter | `javacutil/src/main/java/org/checkerframework/javacutil/DefaultAnnotationFormatter.java` | `framework/src/main/java/org/checkerframework/framework/util/DefaultAnnotationFormatter.java` | 3.21.2-eisop1 | See AnnotationFormatter.java. |

<!-- END typetools-divergences -->

#### Comparing the hunks of an import

`--range` sees only names on added lines, so it misses a typetools hunk that an import drops
silently.  That happens when the code of the hunk's file is elsewhere here: git applies the hunk to
a file of the same path, or to none, and the import shows no conflict.  Typetools commit
fc3a55d3521b of 3.51.1 deleted a `shouldCache = false; ... shouldCache = oldShouldCache;` toggle in
`InitializationAnnotatedTypeFactory`; here that code is in `InitializationParentAnnotatedTypeFactory`,
which this fork split off, so the import of 3.51.1 brought the test change of that commit and not
the source change.

Before merging a typetools import, run on the import branch, with the previous and the imported
release,

```sh
checker/bin-devel/typetools-import-check.py --hunks checker-framework-3.51.0..checker-framework-3.51.1
```

For each commit of the range and each Java, stub, or `messages.properties` file it touches, the
script finds the files here that hold the file's code (by a `file` row of the table, by the same
path, or by the same file name) and lists, with the typetools commit:

- a file with no counterpart here and no `file` row;
- a hunk none of whose context or removed lines occur here, so its code may have moved;
- a removed line that typetools no longer has at the end of the range and that is still here;
- an added line that typetools still has at the end of the range and that is not here.

For fc3a55d3521b it reports the three toggle lines as removed in typetools and still in
`InitializationParentAnnotatedTypeFactory.java`; without the `file` row, it reports the hunk as not
found in `InitializationAnnotatedTypeFactory.java`.

Lines are compared after collapsing whitespace (so a line wrapped differently still matches),
mapping the method names of the `collision` and `rename` rows, and writing `[key]` and
`@Deprecated(since = ...)` as this fork does.  Blank lines, braces, imports, comments, and very
short lines are ignored.  Build files, CI configuration, scripts, and documentation are not
compared: they differ on purpose in too many places, and the import procedure above reviews them.

The findings are prompts for review, not errors; the script exits with 0 unless the arguments are
invalid.  Put its output into the description of the import pull request and mark each finding as
handled (with the commit that handles it) or deliberately skipped (with the reason, or a new row of
the table).

What it cannot detect: a hunk whose lines this fork has rewritten, so that neither the old nor the
new lines occur here; a semantic change that needs code elsewhere to be adapted; a method renamed
inside code that moved to another file (add a `file` row once the move is known); and changes to
the files it does not compare.  It compares with the working tree, so run it on the import branch:
run on a later `master`, it also reports what later commits changed.

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
