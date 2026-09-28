---
name: cf-patch-style
description: Use when authoring commits, patches, or pull requests for the EISOP Checker Framework. Triggers on requests to write a commit message, produce a git format-patch, prepare a PR, or stage changes for review. Codifies the project's commit conventions, branch naming, changelog protocol, and what not to touch.
---

# Patch and commit style for the EISOP Checker Framework

The maintainer's strong preference is for small, focused, commit-ready
patches with informative commit messages and minimal prose around them.
Follow this skill when producing any change for review.

## NEVER push without an explicit, separate OK to push

`git push` is outward-facing and irreversible-ish (it updates a shared
branch / open PR and re-triggers CI). **Always check in with the maintainer
immediately before pushing, every single time** — no exceptions, no
standing authorization, no "they'll obviously want this pushed."

- **"Commit" never implies "push."** Approval to commit, to "add it to
  branch X", or to land a fix is approval to *commit only*. Stop and ask
  before `git push`.
- A request to "fix the CI failure" is **not** a license to push the fix.
  Commit it, then ask before pushing.
- Approval to push one commit does not carry over to the next commit or
  the next turn. Re-ask each time.
- When a change is committed and you believe it should go up, *say so and
  ask* ("Committed as <sha> — push to update PR #N?") rather than pushing.

## Check what is already pushed before amending or rebasing — and avoid force-pushes

Before `git commit --amend`, `git rebase`, or any history rewrite, **check
whether the target commits are already on the remote** — a branch can be pushed
(and CI running) even with no local upstream set. Confirm with
`git ls-remote --heads origin <branch>` (or compare against `origin/<branch>`),
not by assuming from local state.

- **Never rewrite a commit that has already been pushed.** Amending or rebasing a
  pushed commit makes local diverge from the remote, so the *only* way to update
  origin is `git push --force` — which rewrites shared history and re-triggers CI.
- **Prefer a new follow-up commit on top of the pushed history** (e.g. a separate
  "Format …" or "Fix …" commit). It pushes as a plain fast-forward, needs no
  force, and a squash-merge tidies the series at merge time. Verify it is a
  fast-forward with `git merge-base --is-ancestor <pushed-sha> HEAD`.
- Amend/rebase freely only while the commit is **local-only** (never pushed).
- A force-push needs its **own** explicit OK, separate from a normal-push OK, and
  even then default to avoiding it unless the maintainer asks for it.

## Setting an experiment aside without losing work

Reverting a change to confirm something — that a test really fails without the
fix, that a guard is load-bearing — is routine. Three ways to lose the work you
are standing on while doing it:

- **`git checkout -- <file>` reverts to HEAD.** On a file that also holds
  uncommitted work, it throws that away too, with no warning and no reflog
  entry. This silently undid a finished change twice in one session.
- **`git commit` after `git add -A` sweeps in more than you described.** Stage
  the specific paths for the commit you are writing. A commit whose message
  describes one change and whose diff holds two has to be split afterwards,
  which is only safe while it is still unpushed.
- **`git stash` shares one stack across every worktree of the repository**, and
  this project uses worktrees. Verified: stash in one worktree, and
  `git stash list` in another shows the same entry; a bare `git stash pop`
  there consumes and drops it, leaving the first worktree's changes in the
  second worktree's files. Another session working in parallel loses its
  experiment with no error.

What to do instead, cheapest first:

- **Copy the file aside**: `cp Foo.java /tmp/Foo.bak`, experiment, `cp` back.
  Adequate for the one- or two-file reverts that most experiments need, and it
  cannot be disturbed by anything else in the repository.
- **Commit first, then experiment.** A throwaway commit is recoverable from the
  reflog even if the working tree is later clobbered, and `git reset --soft
  HEAD~1` unwinds it. Prefer this when the experiment spans several files.
- **If you do stash**, never bare `git stash` / `git stash pop`. Tag the entry
  and restore it by identity:

  ```bash
  git stash push -u -m "cf-2079-experiment"
  git stash list --format='%H %gs'          # note YOUR entry's SHA
  git stash apply <sha>                     # apply, not pop
  git stash drop <its current stash@{n}>    # find it again by tag first
  ```

  `apply` leaves the entry in place, so a concurrent `pop` elsewhere cannot
  strand you; drop it yourself when finished.

## Deleting merged branches: `is-ancestor` lies here

This repository **squash-merges**, so a merged branch's tip is never an ancestor
of `master`. `git branch -d` refuses it and
`git merge-base --is-ancestor <branch> origin/master` reports it unmerged —
for every merged branch, so neither is usable as the test.

`git diff origin/master..<branch>` is no better: a branch that is merely
*behind* master shows master's newer commits as removals, which reads as
unmerged work.

Ask GitHub what happened, then compare the branch against the squash commit
itself:

```bash
mc=$(gh pr view <N> --json mergeCommit --jq '.mergeCommit.oid')
git diff --stat "<branch>" "$mc"        # empty  =>  content is in master
```

Empty means the branch's content is in master and `git branch -D` is safe.

## One logical change per commit

Series of three or four narrow commits are preferred over a single
sprawling diff. Each commit should be reviewable on its own and revertable
without disturbing the others. Refactoring, performance, and behavioral
fixes belong in separate commits even when they touch the same file.

## Commit subject line

Imperative mood, scope first when it adds clarity, concise. Examples
copied verbatim from real history:

- `Avoid Integer boxing and lambda for ATM hashCode`
- `Clear SubtypeVisitHistory to avoid huge caches that slow down checking`
- `Cache the hashCode for dataflow expressions`
- `Increase default cache size from 300 to 2000`
- `Use cached value of Class::getCanonicalName`
- `Optimize CFAbstractValue#validateSet and avoid allocation`
- `Review of common/basetype package` (for systematic audits)

Avoid: "Improve performance", "Fix bug", "Various changes", "WIP".

## Commit body

Two short paragraphs is usually enough. Structure:

1. **Problem.** What was wrong, and how was it observed. Cite the JFR
   self-time percentage or test failure if applicable.
2. **Fix.** What the change does. Mention any non-obvious invariant
   preserved or trade-off accepted.

Example:

```
AnnotatedTypeScanner.reset: clear visitedNodes instead of reallocating

Every call to AnnotatedTypeMirror.hashCode()/equals() goes through the
static HASHCODE_VISITOR / EQUALITY_COMPARER, whose reset() allocated a
fresh IdentityHashMap. JFR alltests traces attribute roughly 8.6% of
main-thread self-time to this allocation path.

IdentityHashMap.clear() nulls the table in place with no rehash or
realloc, so for a steady-state visitor it is strictly cheaper. Update
the comment that cited an older measurement favoring reallocation.
```

Do not include marketing adjectives ("blazingly", "dramatically",
"massively"). Numbers are more persuasive than prose.

## Commit trailers

- Keep the `Co-Authored-By:` trailer.
- **Never add a `Claude-Session:` trailer.** Strip it from any commit
  message before committing, even if the harness boilerplate suggests
  it. If it slipped into commits that have not been pushed, rewrite
  them to remove it (e.g. `git filter-branch --msg-filter
  "grep -v '^Claude-Session:'"` over the range).

## Refer to a typetools issue in plain text, never as a link

Write **`typetools issue 2816`**. Not `typetools/checker-framework#2816`, not a
markdown link, not a bare URL to github.com/typetools.

GitHub turns the `owner/repo#number` form, and a pasted issue URL, into a
cross-reference: it posts a backlink onto the typetools issue saying this
repository referenced it. Done from commit messages, PR bodies and issue
comments, that puts eisop's traffic into an upstream project's issue tracker,
where it is noise for people who do not work on this fork. The last several
hundred commits here contain no such reference, and that is deliberate.

The plain-text form carries the same information to a human, who can find the
issue in one search, and creates nothing upstream.

```
    Fixes typetools issue 2816.

 * @ignore This fails for Java 11. See typetools issue 2816.
```

Two established exceptions:

- The **`docs/CHANGELOG.md` "Closed issues:" list** uses the compact
  `typetools#NNNN` form alongside `eisop#NNNN`. It is an index of numbers, and
  with no custom autolink configured on this repository it is plain text too,
  so it creates no cross-reference either.
- Some **older files inherited from typetools** contain full URLs. Leave them;
  a URL sitting in a tracked file creates no cross-reference. The rule is about
  what you write, and above all about commit messages, PR and issue bodies, and
  review comments, which GitHub does scan.

The same care applies to an eisop issue in a *typetools* context, and to any
other repository this project does not own.

## Branch naming

- Performance/correctness audits of a package: `review-<package>` or
  `perf-<package>`, e.g., `review-common-basetype`.
- Bug fix: `fix-<short-description>`.
- Feature: `feature-<short-description>`.
- Infrastructure: descriptive, e.g., `gradle-9.4.1`.

## Changelog

Every user-visible or perf-relevant change adds a bullet to the next
release section in [`docs/CHANGELOG.md`](../../../docs/CHANGELOG.md).
Match the existing style: one line, ends with the PR number once it's
opened.

**The "Closed issues:" list is one issue per line** while a release section is
unreleased:

```
eisop#2089,
eisop#2095,
typetools#399,
typetools#3203.
```

A PR that adds a number then touches only its own line, so two PRs in flight
merge cleanly unless they insert at the very same point. The old filled-paragraph
form conflicted on *every* concurrent pair, because adding one number reflows
the whole paragraph. Markdown joins the lines, so the rendered changelog is
identical either way -- the difference is only in what git sees.

Measured, adding two different numbers on two branches and merging:

| form | different points | same point |
| --- | --- | --- |
| filled paragraph | conflict | conflict |
| one per line | clean | conflict |

The convention is documented in
[`docs/developer/README-eisop.md`](../../../docs/developer/README-eisop.md),
not as a comment inside `docs/CHANGELOG.md`: "Prep for next release" creates
each section with an empty list, so a comment there would have to be re-added
every prep and stripped every release, and a note that needs restoring on every
cycle is one that eventually contradicts the file. None is needed — once the
list is one per line, the next entry is added the same way by imitation.

**Reflowing to filled lines when a release is finalized is optional**, and
purely cosmetic: Markdown joins the lines either way, so a released section
left one-per-line renders correctly and is not a defect to fix. Leave already
released sections as they are.

When the same-point conflict does happen, it is two lines: keep both, in
ascending order. Check the whole list afterwards -- ascending, no duplicates,
both PRs' numbers present -- since a conflict that starts mid-list shows only
part of it.

## What not to touch in a perf patch

- **`checker-qual/`** is public API. Signature changes break downstream.
- **Default values in public classes** without a release note.
- **`AnnotatedTypeMirror` equality/hash contract.** Cache it, don't
  redefine it.
- **`@Pure`/`@Deterministic`/`@SideEffectFree` annotations on methods**
  unless you understand the dataflow consequences.

## When producing a patch series via `git format-patch`

Generate to a clean directory:

```
git format-patch origin/master -o /tmp/cf-patches/
```

Each file is standalone and applies with `git am`. Verify round-trip
before submitting:

```
git checkout -b verify origin/master
git am /tmp/cf-patches/*.patch
./gradlew assemble
git checkout - && git branch -D verify
```

## Test requirements

- `./gradlew assemble` — must succeed.
- The relevant focused test(s) — e.g., `:checker:NullnessTest`.
- `./gradlew alltests` — strongly preferred for any framework or
  javacutil change. Subtle visitor and dataflow semantics often fail
  only in obscure checkers. If it fails *only* on `:checker:jtregTests` /
  `:checker:jtregJdk11Tests` with `No java executable at java`, that is an
  environment issue, **not a regression**: `JAVA_HOME` is unset. Set it
  (`JAVA_HOME=$(dirname $(dirname $(readlink -f $(which java))))`) and
  re-run those two tasks; the JUnit suites are unaffected.
- Run `./gradlew spotlessApply` before committing to fix **Java and Gradle**
  formatting — Spotless covers `*.java` *and* `*.gradle` files, so a build-script
  edit needs it too.
  Spotless does **not** touch shell or Python: if your change adds or edits a
  `*.sh` or `*.py` file, also run the shell/Python formatters **and linters**
  — shfmt + `shellcheck` for shell, ruff format + ruff check for Python (see
  the `misc` CI bullet below) — or CI's
  `shell-style-check`/`python-style-check` will fail.

If `alltests` is impractical (e.g., no local JDK matrix), say so
explicitly in the PR description rather than implying it passed.

## A green run on one JDK says nothing about the others

CI runs the matrix; a development machine usually runs one JDK. Twice in one
session a change verified as green locally failed CI on a JDK that was never
tried:

- A jtreg test used an `instanceof` pattern whose expression type is already a
  subtype of the pattern type. javac accepted it on 21 and rejected it as an
  unconditional pattern on 17 and 20, which the test's
  `@below-java17-jdk-skip-test` marker did not exclude.
- `AbstractTypeProcessor` was keyed on the synthetic type that javac's ANALYZE
  event carries for a `package-info.java`. javac 21 reports
  `<package>.package-info`; **javac 11 and 17 report an anonymous type in the
  unnamed package**, from which neither the package nor its annotations can be
  reached. The dispatch silently did nothing on those versions.

Both were invisible to `./gradlew test` on JDK 21.

**Run the other JDKs when the change touches anything version-sensitive** —
`javacutil`, anything reading `com.sun.*` or `javax.lang.model`, the language
features a test source uses, or a `@requires`/skip marker:

```
ORG_GRADLE_PROJECT_useJdkVersion=11 ./gradlew :checker:jtregTests
ORG_GRADLE_PROJECT_useJdkVersion=25 ./gradlew :checker:jtregTests
```

Two traps in that command:

- **Gradle itself needs JDK 17+.** Keep `JAVA_HOME` on a modern JDK and let
  `useJdkVersion` select the target; setting `JAVA_HOME` to 11 fails with
  "Gradle requires JVM 17 or later to run".
- **`useJdkVersion` does not always change jtreg's `testJDK`.** Check what
  actually ran: `grep -h '^testJDK' checker/build/jtreg/all/work/**/*.jtr`. A
  `.jtr` file also records the `@requires` expression it evaluated, so it tells
  you whether a test ran, was filtered, or was never selected.
- The jtreg **work directory accumulates across runs**, so `report/text/summary.txt`
  can show two mutually exclusive suites both "Passed" — they passed on
  different JDKs. `rm -rf checker/build/jtreg` before a run whose results you
  intend to read.

When a JDK cannot be run locally (no toolchain for it), say so rather than
implying the matrix was covered.

## Do not declare a lint gate clean from truncated output

`requireJavadoc` and `javadocDoclintAll` emit hundreds of pre-existing findings,
so the only question is whether any falls on a line **this diff added**. Piping
the log through `head` and seeing nothing relevant is not an answer: a real
`no @param for isError` finding sat below a `head -6` cutoff, was reported as
clean, and failed the `misc` job.

Intersect the *complete* finding list against the *complete* added-line set:

```bash
./gradlew requireJavadoc javadocDoclintAll spotlessCheck --continue > /tmp/gate.log 2>&1
for f in $(git diff --name-only origin/master...HEAD -- '*.java'); do
  base=$(basename $f)
  added=$(git diff -U0 origin/master...HEAD -- "$f" \
      | awk '/^@@/{split($3,a,","); s=substr(a[1],2); n=(a[2]==""?1:a[2]);
                   for(i=0;i<n;i++) print s+i}')
  while IFS= read -r line; do
    ln=$(echo "$line" | grep -oE "$base:[0-9]+" | head -1 | cut -d: -f2)
    [ -z "$ln" ] && continue
    echo "$added" | grep -qx "$ln" && echo "HIT: $line"
  done < <(grep "/$base:" /tmp/gate.log)
done
```

No `HIT` lines means clean. This is the same rule as the `-Werror` and
closed-issues lessons in [`cf-code-review`](../cf-code-review/SKILL.md): one
truncated report is a sample, not the population.

## CI checks that `assemble` and `alltests` do NOT run

**Run the whole `misc` gate before proposing a push, not the part you
suspect.** Checking one item off the list below and stopping is how a
green local build still turns CI red. The checks are independent, so a
clean result from one says nothing about the others:

```
# the import rule -- the only check here that hard-exits
grep -n -r --exclude-dir=build --exclude-dir=examples --exclude-dir=jtreg \
  --exclude-dir=tests --exclude="*.astub" --exclude="*.tex" \
  '^\(import static \|import .*\*;$\)'
./gradlew requireJavadoc javadocDoclintAll spotlessCheck --continue
make style-check          # shell + Python only; skip if no .sh/.py changed
```

**`--continue` is not optional.** `requireJavadoc` fails on this repo
unconditionally -- there are well over a thousand pre-existing violations --
so without it Gradle stops there and `javadocDoclintAll` and `spotlessCheck`
never run, which is the exact trap this section is about.

**Read the result the way CI does: by line, not by exit code.** Both Javadoc
tasks are wrapped in `|| true` in `test-misc.sh` and their output is piped
through `ci-lint-diff`, which reports only findings on lines the diff
*changed*. So a red `requireJavadoc` means nothing on its own; what matters is
whether any finding falls inside your own added lines. Get those from
`git diff -U0 origin/master...HEAD` and intersect. A finding in a file you
touched, on a line you did not, is pre-existing -- do not chase it.

Run the grep from the repo root over tracked files only (`git ls-files -z
'*.java' | xargs -0 grep -n ...`); run plainly it also matches untracked
checkouts parked under `.claude/worktrees/`, which CI never sees.

Ordering matters when reading a failed CI log: `requireJavadoc` and
`javadocDoclintAll` run first and only accumulate a status, while the
import grep does `exit 1` immediately. So a log that ends at the import
error proves the Javadoc checks ran, but a log that ends *anywhere*
proves nothing about the checks below it -- `make style-check` and
`htmlValidate` never ran at all. Same shape as the `-Werror` trap
elsewhere in this file: one truncated report is a sample, not the
population.

The `misc` CI job (`checker/bin-devel/test-misc.sh`) runs lint that a
normal build skips. Two of its checks catch things that compile and test
green but still fail CI:

- **`./gradlew javadocDoclintAll`** runs `-Xdoclint:all` at the **PRIVATE**
  member level — stricter than `:framework:javadoc` (PUBLIC), so it
  validates javadoc on private methods/fields. The recurring footgun: a
  `{@link Foo#bar}` to a class in a **sibling package that the code does not
  import** (the code reaches it only through a return type, so no `import`
  is needed) resolves fine for the compiler but doclint reports
  `reference not found`. Fix: use the **fully-qualified** name in the
  `{@link}`. Run `./gradlew javadocDoclintAll` locally before pushing any
  javadoc-touching change to a private member.
- **`ci-lint-diff`** only flags warnings on lines the PR **changed**.
  Pre-existing warnings on untouched lines in the same file are fine — do
  not chase them, and do not let them make you think your change is at
  fault.
- A javadoc/comment-only edit can still shift line wrapping enough to fail
  `spotlessJavaCheck` (a pre-commit hook then blocks the commit). Re-run
  `./gradlew spotlessApply` after **any** edit, including comment-only ones,
  not just code edits.
- **`spotlessApply` on `//` comments is non-idempotent: it splits over-long
  ones but never rejoins short ones.** google-java-format wraps a `//` line
  comment that exceeds the column limit onto a second line, but it leaves an
  already-short comment alone — so a hand-wrapped or incrementally-edited `//`
  comment can keep a bad mid-sentence break indefinitely and `spotlessCheck`
  will still pass (verified: two short `//` lines survived both
  `spotlessApply` and `spotlessCheck` untouched). Collapse a `//` comment to a
  single line yourself and let spotless re-wrap it; never hand-wrap `//`
  comments.
- **`shell-style-check` / `python-style-check` cover shell and Python, which
  Spotless does NOT.** `shell-style-check` runs **three** tools on every
  bash/`*.sh` script — `shfmt -i 2 -ci -bn -sr` (format), `shellcheck -x`
  (lint), and `checkbashisms` (POSIX-`sh` scripts only) — and
  `python-style-check` runs `ruff format --check` + `ruff check` on every
  `*.py`. A patch that adds or edits such a file passes `assemble`/`alltests`
  but fails this CI job if any of those flag it (real example: new `.sh`/`.py`
  skill scripts).
- **shfmt is NOT enough — run shellcheck too.** `shfmt`/`make shell-style-fix`
  only *reformat*; they do not catch `shellcheck` lint such as **SC2034**
  (unused variable — e.g. a `for rep in 1 2` counter the body never reads; fix
  by renaming the variable to `_`, which shellcheck ignores). These findings
  need a manual code change, not a formatter pass. If `shellcheck` is not on
  the PATH, install it before claiming a shell script is clean — do not skip
  the lint step and report only the formatter result.
- Fix shell with `make shell-style-fix` (or per file
  `shfmt -w -i 2 -ci -bn -sr <f.sh>`), then **verify both steps** per file:
  `shfmt -d -i 2 -ci -bn -sr <f.sh>` *and* `shellcheck -x <f.sh>`. Fix Python
  with `ruff format <f.py> && ruff check --fix <f.py>`, verify with
  `ruff format --check <f.py>` + `ruff check <f.py>`.
- Caveats on the local `make shell-style-check`: it finds scripts by shebang,
  so it also flags stray untracked `*.orig` backups that a clean CI checkout
  never sees (trust the per-file checks on your own file, not just the `make`
  exit code), and when `checkbashisms` is not on the PATH the Makefile
  downloads it to `./.checkbashisms` — a build artifact that is gitignored, not
  committed.

## Javadoc on every method you touch (and its neighbors)

`require-javadoc` + `ci-lint-diff` flag any **changed** line lacking
documentation, so adding or moving a line next to an undocumented
declaration makes a previously-silent warning fail the build. Pre-empt it
rather than waiting for CI:

- **Any method, constructor, field, or class whose lines your diff
  touches must carry a complete Javadoc comment** — a summary sentence
  plus a `@param` for every parameter and a `@return` for every non-void
  method (and `@throws` for documented checked exceptions). This holds
  for test classes too (e.g. JUnit `getTestDirs`/`@Parameters` methods),
  which `require-javadoc` checks just like production code.
- **Also document anything directly adjacent** to your change — the
  declaration immediately above or below an inserted line can land in the
  diff hunk and get flagged even though you did not mean to touch it.
- A brand-new file has *every* line counted as changed, so document all
  of its members, not just the one you care about.
- Match the surrounding wording convention (e.g. existing `getTestDirs`
  Javadoc reads "the directories containing test code"); do not invent a
  new phrasing. Run `./gradlew requireJavadoc` locally to confirm before
  committing.

## Verify what you committed, not what's in the working tree

After a commit — especially a file **move** plus an in-place edit, or any
`git add -A <pathspec>` where a path was already renamed — confirm the
committed content with `git show HEAD:<path>`, not by reading the working
tree. A stale pathspec can make `git add` silently drop a file from the
commit while the working tree still shows your edit, so "it's already
fixed" reads true from the tree but false from `HEAD`. One `git show HEAD:`
check before claiming a change landed avoids a wrong status report.

## What good output to a human reviewer looks like

- A branch with N small commits, each compiling.
- A PR description that summarizes the series in two or three sentences.
- A `docs/CHANGELOG.md` entry per user-visible change.
- No drive-by formatting churn, no `import` reordering on untouched
  files, no IDE-config commits.
