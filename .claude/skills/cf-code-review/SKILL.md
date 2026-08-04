---
name: cf-code-review
description: Use when reviewing changes to the EISOP Checker Framework — a branch, a PR, an agent's or contributor's commits, or your own work before handing it back. Triggers on "review this branch/PR", "look for improvements", "make suggestions", "polish everything", "is this worthwhile". Codifies review scope, the generalize-the-finding rule, evidence standards, and the multi-agent review workflow.
---

# Reviewing changes in the EISOP Checker Framework

This skill exists because the same corrections recurred across many review
sessions. Each rule below is a mistake that was actually made and had to be
called out. Read it before starting a review, not after.

See also [`cf-patch-style`](../cf-patch-style/SKILL.md) for commit/push
discipline (which applies in full to review fixes) and
[`cf-performance`](../cf-performance/SKILL.md) for any perf claim.

## Never fix only the instance — generalize the finding

**This is the single most-repeated correction.** A defect found in one place
is a hypothesis about a *class* of defect. Before reporting or fixing it,
grep for every sibling occurrence and report the whole set.

Real examples of the maintainer having to ask for this after a review:

- "Ensure no similar issues exist for other annotations."
- "Look for other places where we should be using
  `ElementUtils.isElementFromByteCode` instead of checking for `tree == null`.
  Let's clean this all up together."
- "Go through all changes in this branch and ensure bytes are consistently
  read and written as signed or unsigned data."
- "Do we need `generateBinaryStubFiles` in `framework/build.gradle` *and*
  `checker/build.gradle`? … Look through other duplication throughout this
  branch."
- "Fix all issues, make text and binary parsers consistent."

Parity rule: this repo has **paired implementations** (text vs. binary stub
parsers, framework vs. checker build files, `addAnnotations` vs.
`addMissingAnnotations` call sites). A change to one side is incomplete until
you have checked the other. "Binary and text parsing should behave
equivalently" is a standing requirement, not a nice-to-have.

## Review scope includes the already-committed code, not just the diff

Requests to review "this branch" mean the branch's whole contribution,
including commits that landed earlier — "look through other duplication
throughout this branch, **in your changes and already committed changes**."
Do not scope a review to `git diff HEAD~1` when asked about a branch. Use
`git diff master...HEAD` and read the resulting state, not only the patch.

## A hypothesis is not a finding

The maintainer quotes this back approvingly; hold the line yourself. If you
have not measured or traced it, say so explicitly and **file an issue instead
of acting**:

> "I did **not** measure this and I am not proposing it blind — per
> `performance-notes.md`, a hypothesis is not a finding. File an issue, and
> profile before acting."

- Label every unverified claim as a hypothesis in the review output.
- Verify claims about behavior before asserting them. "That must be wrong!
  The `checker/tests/nullness` tests clearly must use the annotated JDK.
  Verify this." — the assertion was wrong and cost a cycle.
- Any performance claim needs the `cf-performance` A/B protocol behind it.
  Never repeat a prior session's A/B number; re-measure against today's
  baseline.
- **Root-cause an "only fails here" claim before touching infrastructure,
  too — not just code.** A `gh pr edit --body-file` failure was blamed on this
  repo's "Projects (classic)" setting, and a repo-level setting was changed on
  that hypothesis; it did not fix the failure. The real cause was a
  two-year-stale local `gh` CLI still querying a GraphQL field GitHub had
  deprecated — upgrading `gh` fixed it, and the no-op setting change was
  reverted. Changing a shared setting to test a guess is acting on an
  unverified root cause; reproduce and isolate first.

## A partial signal is not the whole set — verify completeness

A source that *looks* exhaustive often isn't, by construction. Two review
deliverables in one session shipped incomplete because they trusted such a
signal instead of enumerating the whole set from an authoritative source.

- **A `-Werror` CI log stops at the first warning.** `build.gradle` says it
  outright: Error Prone "stops as soon as it issues one warning, rather than
  outputting them all." So fixing what one truncated CI log shows and
  re-pushing can hit a *second*, previously-invisible warning on the next
  run — one round-trip per warning. To see every warning in one pass, run the
  affected build locally with `-Werror` temporarily removed (the file
  documents the loop: "Temporarily comment out `-Werror` … Repeatedly run
  `./gradlew clean compileJava` and fix all errors … Uncomment `-Werror`").
  Correcting `-AwarnUnneededSuppressions` (#1908) surfaced **nine** now-unneeded
  suppressions across five files; they landed in one commit because the local
  `-Werror`-off pass showed all of them at once, not one CI cycle at a time.
- **A merged-PR keyword scan misses manually-closed issues.** Backfilling a
  CHANGELOG "Closed issues:" list (#1910) by scanning PR bodies for a GitHub
  auto-close keyword (`closes #N`) caught only issues closed *by* a PR's own
  keyword. It missed four closed manually — with an explanatory comment, or a
  cross-reference to the motivating PR without the literal keyword. The list
  was complete only after a second pass querying the issue tracker directly:
  "Pass 1 only catches issues closed by a PR's own auto-close keyword. It
  misses an issue closed without one."

The shared shape: a CI log, a keyword-matched grep, one truncated report is a
*sample*, not the population. When the deliverable is "every X," enumerate X
from the authoritative source, then cross-check the convenient signal against
it — never the other way around. (This does not contradict "don't reproduce
the whole CI matrix" below: that is about not re-running many *different* jobs;
this is about seeing all of *one* build's warnings in a single pass.)

## Never revert or weaken an encoded policy without asking

> "Revert A. In the future, **always ask before reverting an explicit policy
> we encoded.** This is just stupid waste of time."

When a strict check, a stricter fallback policy, or a deliberate constraint is
already in the code, a test failure against it is evidence about the *test* or
the *environment* first. Do not relax the policy to make something pass.
Related: "No, I do not want a fallback to text parsing." Suggest, don't
unilaterally undo.

## Do not introduce dead or unused artifacts

> "Sonnet proposed a solution that adds a `BinaryStubToolClasses` file that is
> unused. Find a nicer solution that does not add an unused file."

A review fix that leaves an unused file, unused parameter, or dead branch is
not done. `DefaultApplierElementImpl extends AnnotatedTypeScanner<Void,
AnnotationMirror>` with an always-unused parameter is the canonical smell —
flag these rather than preserving them.

## Javadoc is a CI gate — treat it as part of the change

See [`cf-patch-style`](../cf-patch-style/SKILL.md)'s "Javadoc on every method
you touch (and its neighbors)" — `requireJavadoc`/`javadocDoclintAll` fail
the misc CI job on a missing `@param`, and this has bitten review fixes
specifically: making a parameter `@Nullable` means updating that method's
Javadoc too ("In `SourceChecker`, make sure to update the javadoc for all
methods where you make a parameter `@Nullable`.").

## Don't reproduce the whole CI matrix locally

> "Don't attempt to do all CI tasks locally, there are too many of them and
> that's why we have the CI setup."

Run the targeted task for the code you touched (`:framework:test`, the
specific `--tests` class, `checknullness`). Push and let CI cover the matrix.
Known local red herring, before you debug a "failure": `Issue1438*` jtreg
tests time out under `alltests` parallelism — **master fails identically**.
Compare against master before chasing it. (See `cf-patch-style`'s "Test
requirements" for the `JAVA_HOME`/jtreg red herring.)

But the inverse trap is real too: **a companion-repo CI job can go red for a
legitimate reason, not a flake.** The `plume-lib`, `daikon-part1`, and
`daikon-part2` jobs clone maintainer-controlled forks (`eisop-plume-lib/*`,
`eisop-codespecs/daikon`) that mirror upstream projects for compatibility
testing. When one goes red after a *behavior* change, do not reflexively file
it under "infra flake" or "not my code" — a correctness fix can legitimately
break an assumption the mirrored source relied on. Correcting
`-AwarnUnneededSuppressions` to actually fire (#1908) is the case in point: it
surfaced real, pre-existing unneeded suppressions in the companion repos' own
source. The fix was working as intended; the follow-up belonged in the
companion repo (the forks are maintainer-owned and pushable), not in a
workaround here. Read the failing job's diagnostic and decide whether it is
your change doing its job before dismissing it.

## Writing up a review

- **Markdown, in a file, when asked to hand it over.** "Write all your
  findings into a markdown file in the repo to allow me to go through them."
  For issue text: "Give me markdown text for the follow-up issue so that I can
  file it."
- **Rank by severity**, correctness above cleanup.
- **Neutral tone.** "Why the harsh words?" — describe the defect, not the
  author's judgment. No marketing language in the other direction either.
- **Ask which branch/PR a fix belongs in** rather than assuming; the repo
  squash-merges, which changes the calculus: "Should these open changes go
  into the current branch or should they be a separate PR? (Remember we squash
  merge PRs.)"

## Keeping `performance-notes.md` and docs honest

Documentation review has its own recurring failure: notes drift from the code
and narrate history instead of describing the result.

- **Describe the current code, not the branch's story.** "The
  `performance-notes.md` changes are too verbose. It does not need to describe
  the whole history of this branch. Focus on the final result and the
  performance impacts, don't focus on every single commit."
- **Re-verify notes against the implementation.** A Copilot comment caught a
  bullet claiming a mirror was built in a constructor when the code built it
  lazily. "Go through all your performance notes and ensure they correspond to
  the current version of the code."
- Changelog entries are for **end users** and should be minimal: "This doesn't
  need a changelog entry. Add the timing details to the commit message."

## Multi-agent review (the fan-out that worked)

For a large branch the maintainer cannot hand-review, this pattern was used
repeatedly and successfully:

1. **Fan out finder agents by angle**, one agent each: line-by-line
   correctness, removed-behavior audit, cross-file tracing, reuse,
   simplification, efficiency, conventions, altitude. Each returns **only** a
   JSON array of ≤6 candidates (`{file, line, summary, failure_scenario}`).
2. **Dedup** near-duplicate candidates into a single list.
3. **Fan out one verifier agent per unique candidate** — verdict
   `CONFIRMED` / `PLAUSIBLE` / `REFUTED`, recall-biased.
4. **Drop `REFUTED`**, rank the rest most-severe-first, report ≤10 via
   `ReportFindings` with `verdict` set, then give a prose summary that also
   lists verified cleanup items that didn't fit the cap.

Operational notes learned the hard way:

- **Dispatch sequentially, not concurrently — one agent at a time.** "Run the
  agents sequentially to avoid overloading the machine." The fan-out above is a
  list of angles to cover in turn, not a license to launch them all at once;
  parallel dispatch was explicitly corrected and held as a hard constraint for
  the rest of a long session.
- **Wait for completion notifications; do not poll agent transcripts with
  `TaskOutput`.**
- **Rate limits interrupt agents mid-task.** Resume with `SendMessage` telling
  the agent where it left off and to finish — do not respawn (it restarts cold).
- **Require a terminal artifact:** "Do not end your turn without a commit sha
  or a revert-and-report."
- **Verify agent claims against git, not the agent's report.** "git log
  `<base>..<branch>` and git status are the ground truth."
- Background waits stall; re-run verification in the **foreground** before
  believing a pass.

## Model choice — the maintainer pays for this

Credits are a live constraint: "You are burning through my credits too
quickly, be concise and efficient, please, please, please."

- **Sonnet for mechanical/scoped work** — finder and verifier agents, focused
  fixes with a clear spec. Opus only for genuinely hard design questions.
- Ask before launching a large fan-out; say what it will cost in agents.
- Keep replies terse. The maintainer will ask for depth if wanted.
