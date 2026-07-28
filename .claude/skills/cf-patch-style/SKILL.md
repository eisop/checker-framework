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
  only in obscure checkers.
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

## CI checks that `assemble` and `alltests` do NOT run

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
