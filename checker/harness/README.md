## Summary

Adds a reusable test harness for A/B performance testing of annotation processors.

Provides pluggable `Driver` and `CodeGenerator` interfaces with three execution modes (in-process, external-process, jtreg), two test protocols (SINGLE, CROSS), and automatic Markdown reports.

## Motivation

The Checker Framework's type-checking is performance-sensitive. Optimizations can significantly impact analysis time, but lacked systematic benchmarking infrastructure.

### Previous limitations

- **No A/B testing framework**: CF Requires manual timing and spreadsheet analysis
- **Ad-hoc test scripts**: Existing perf tests (e.g., `NewClassPerf.java`) duplicated timing logic and couldn't produce comparable reports
- **Order bias**: Simple sequential runs ("A 10x, then B 10x") suffer from JIT warmup drift

### What this enables

- Test fast-path optimizations with one command → get median/average/min/max + % delta
- CROSS protocol (ABBA) mitigates order effects for reliable comparisons
- Unified markdown reports with diagnostics, reproduction commands, and environment snapshot
- Reusable across checkers (Nullness, Initialization, etc.) and workload generators

Developers can now validate optimizations with `./gradlew :harness-driver-cli:run --args="..."` instead of writing custom test harnesses.

## Components

### Core APIs

- **`CodeGenerator`** - interface: Writes test source files. Includes `NewAndArrayGenerator` for nullness checking tests (deterministic output via seed).
- **`Driver`** - interface: Runs one test cycle (generate sources → compile → collect results). Three implementations:
  - `InProcessJavacDriver`: Calls javac via `ToolProvider` (fast, shared JVM)
  - `ExternalProcessJavacDriver`: Runs javac as separate process (clean isolation)
  - `JtregDriver`: Wraps existing jtreg tests

### CLI Tool

**`Main`**: Single command-line entry point with flexible flag handling:

- **Protocols**:
  - **SINGLE**: Run baseline N times → run update N times → compare medians
  - **CROSS (ABBA)**: Alternate AB/BA to cancel out JIT/cache order effects

- **Flag system**: Isolate the exact change being tested
  - `--baseline-flags`: Options for baseline configuration
  - `--update-flags`: Options for update configuration

  **Example**: Testing a fast-path optimization in the nullness checker

  Baseline code (always adds annotation):
  ```java
  // NullnessNoInitAnnotatedTypeFactory:750
  public Void visitNewClass(NewClassTree tree, AnnotatedTypeMirror type) {
      type.addMissingAnnotation(NONNULL);
      return null;
  }
  ```

  Optimized code (skip if already present):
  ```java
  private static final boolean SKIP_NONNULL_FASTPATH =
            Boolean.getBoolean("cf.skipNonnullFastPath");

  public Void visitNewClass(NewClassTree tree, AnnotatedTypeMirror type) {
      if (!SKIP_NONNULL_FASTPATH && type.hasEffectiveAnnotation(NONNULL)) {
          return null;  // Early exit saves redundant work
      }
      type.addMissingAnnotation(NONNULL);
      return null;
  }
  ```

  Benchmark command:
  ```bash
  --baseline-flags -Dcf.skipNonnullFastPath=true   # Disable fast-path (slow)
  --update-flags -Dcf.skipNonnullFastPath=false    # Enable fast-path (fast)
  ```

  The harness compiles identical sources under both configs and reports speedup.

- **Flag forwarding** (handles three execution contexts):

  Annotation processors read runtime configuration from `System.getProperty("cf.*")`. Each engine forwards flags differently based on its JVM boundary:

  - **In-process** (`InProcessJavacDriver`):
    - Javac runs in the same JVM as the harness
    - `-Dcf.skipNonnullFastPath=false` → `System.setProperty("cf.skipNonnullFastPath", "false")`
    - Properties are set before compilation, restored after to prevent cross-run pollution
    - Accepts both `-D` and `-J-D` forms (strips `-J` prefix)

  - **External** (`ExternalProcessJavacDriver`):
    - Javac runs in a separate JVM subprocess
    - `-J-Dcf.skipNonnullFastPath=false` → passed as JVM argument to `ProcessBuilder`
    - The `-J` prefix tells javac to forward the flag to its internal JVM

  - **Jtreg** (`JtregDriver`):
    - Test runs in jtreg's test VM, which then spawns javac
    - `-Dcf.skipNonnullFastPath=false` → forwarded via jtreg's `-vmoption` flag
    - Jtreg passes this to the test VM; `JtregPerfHarness` reads it and constructs javac args

- **Auto-warmup**: Each variant runs once (untimed) before measurements to stabilize JIT

## How to Use

Run from the repository root. Build artifacts first (needed by --processor-path):
- checker/dist/checker.jar
- checker-qual/build/libs/checker-qual-*.jar

```bash
./gradlew :checker:shadowJar -x test --no-daemon --console=plain
./gradlew :checker:assembleForJavac -x test --no-daemon --console=plain
./gradlew :checker-qual:jar -x test --no-daemon --console=plain
```

**Run these whenever code under `checker/` or `checker-qual/` changes (or after a clean clone) to keep artifacts up to date.**

### 1) Navigate to the harness subproject

The harness is a standalone Gradle subproject with its own `settings.gradle`. You must run commands from within its directory:

```bash
cd checker/harness
```

All subsequent commands assume you are in `checker/harness/`. The harness uses the root project's `gradlew` wrapper via `../../gradlew`.

### 2) Run harness commands

**In-process (fast dev loop):**
```bash
../../gradlew :harness-driver-cli:run --no-daemon --console=plain --args="\
  --generator NewAndArray \
  --sampleCount 3 \
  --seed 42 \
  --processor org.checkerframework.checker.nullness.NullnessChecker \
  --processor-path ../../../checker/dist/checker.jar:../../../checker-qual/build/libs/checker-qual-*.jar \
  --release 17 \
  --protocol SINGLE \
  --runs 3 \
  --engine inproc \
  --extra.groupsPerFile 10 \
  --baseline-flags -Dcf.skipNonnullFastPath=false \
  --update-flags -Dcf.skipNonnullFastPath=true"
```

**External process (clean isolation):**
```bash
../../gradlew :harness-driver-cli:run --no-daemon --console=plain --args="\
  --generator NewAndArray \
  --sampleCount 3 \
  --seed 42 \
  --processor org.checkerframework.checker.nullness.NullnessChecker \
  --processor-path ../../../checker/dist/checker.jar:../../../checker-qual/build/libs/checker-qual-*.jar \
  --release 17 \
  --protocol SINGLE \
  --runs 3 \
  --engine external \
  --extra.groupsPerFile 10 \
  --baseline-flags -J-Dcf.skipNonnullFastPath=false \
  --update-flags -J-Dcf.skipNonnullFastPath=true"
```
Note: Use `-J-D` prefix for system properties with external engine.

**JTReg (Compatibility with Existing Tests)**

Before running, verify that **JTReg** is available under `checker/harness`:

```bash
# Check JTReg path and version
ls -la ../../../jtreg/bin/jtreg
../../../jtreg/bin/jtreg -version || ../../../jtreg/bin/jtreg --version
```

If JTReg is **not installed**, install it using one of the following methods (run from `checker/harness`):

**Automated (Recommended)**

```bash
bash setup-jtreg.sh
```

**Manual (Fallback)**

```bash
cd ../../..
curl -L -o jtreg.tar.gz https://builds.shipilev.net/jtreg/jtreg-7.5+b01.tar.gz
tar -xzf jtreg.tar.gz && mv jtreg-* jtreg && rm jtreg.tar.gz
chmod +x jtreg/bin/jtreg jtreg/bin/jtdiff
```

**Verification Steps (run from `checker/harness`)**

```bash
# 1) Confirm files exist and are executable
ls -la ../../../jtreg/bin/jtreg
test -x ../../../jtreg/bin/jtreg && echo "ok: executable"

# 2) Print version to confirm it runs
../../../jtreg/bin/jtreg -version 2>/dev/null || ../../../jtreg/bin/jtreg --version

# 3) (macOS only) Clear quarantine attributes if needed
xattr -d com.apple.quarantine ../../../jtreg/bin/jtreg 2>/dev/null || true
```
Once JTReg is installed, run the following command from `checker/harness`:

```bash
../../gradlew :harness-driver-cli:run --no-daemon --console=plain --args="\
  --generator NewAndArray \
  --sampleCount 3 \
  --seed 42 \
  --processor org.checkerframework.checker.nullness.NullnessChecker \
  --processor-path ../../../checker/dist/checker.jar:../../../checker-qual/build/libs/checker-qual-*.jar \
  --release 17 \
  --protocol SINGLE \
  --runs 3 \
  --engine jtreg \
  --jtreg ../../jtreg/bin \
  --jtreg-test checker/harness/jtreg/JtregPerfHarness.java \
  --extra.groupsPerFile 10 \
  --baseline-flags -Dharness.release=17 -Dcf.skipNonnullFastPath=false \
  --update-flags -Dharness.release=17 -Dcf.skipNonnullFastPath=true"
```

Note: Jtreg requires `-Dharness.release` to match `--release`, and uses `-D` (not `-J-D`).

Output: `checker/harness/result/report.md` (config, env, diagnostics, timing stats, comparison, reproduction commands).

### 3) What each flag means (quick reference)

| Flag | Purpose |
| --- | --- |
| `--generator <Name>` | Pick a source generator that creates the .java workload. Today: `NewAndArray`. |
| `--sampleCount N`, `--seed S` | Control how many files to generate and make them reproducible. Same seed → same sources. |
| `--processor`, `--processor-path` | Which annotation processor to run, and where to find it + its deps. Include `checker/dist/checker.jar` AND `checker-qual/build/libs/checker-qual-*.jar`. |
| `--release <8\|11\|17\|21>` | Compile target platform (language level and stdlib). Must match your test plan. |
| `--protocol SINGLE\|CROSS` | SINGLE: run A then B (each N times). CROSS: do AB then BA per iteration to reduce order bias; we report per-iteration averages `Aᵢ`, `Bᵢ`. |
| `--runs N` | Number of timed iterations. SINGLE → N samples for A and N for B. CROSS → N iterations; each iteration yields one `A_i` and one `B_i`. First occurrence of A and B is auto‑warmed (not timed). |
| `--engine inproc\|external\|jtreg` | Where compilation runs: in the same JVM (fast), in a forked `javac` process (clean isolation), or via jtreg (compatible with existing jtreg setups). |
| `--baseline-flags ...` | Flags for the “A” variant (e.g., optimization OFF). Typically controls processor behavior via system properties or `-A...` options. |
| `--update-flags ...` | Flags for the “B” variant (e.g., optimization ON). Keep everything else identical to baseline except what you intentionally change. |

Extra generator knobs:
- `--extra.groupsPerFile <N>`: increases per‑file work (default 400). More groups → longer compile → less noise.

Engine‑specific system property forwarding (for `cf.*` and other `-D` keys)

- inproc: pass `-Dkey=value` (also accepts `-J-Dkey=value`; we strip `-J` automatically).
  - value: the exact string assigned to the JVM system property; processors read it via
    `System.getProperty("key")` (or `Boolean.getBoolean("key")` for booleans).
    - Booleans: `true` / `false` (lowercase recommended).
    - Numbers: plain decimal like `123`.
    - Strings: use quotes if value contains spaces, e.g. `-Dfoo="a b"`.
    - Parsing rule: only the first `=` splits key/value; everything after it is the value.
    - You may repeat `-D` to set multiple properties.

- external: pass `-J-Dkey=value` (because `-J` forwards to the forked JVM that hosts `javac`).
  - Same value rules as above; quoting is handled by your shell then by `javac`’s JVM.

- jtreg: pass `-Dkey=value` (we forward via `-vmoption` to the test JVM).
  - Additionally set `-Dharness.release=<ver>` so the jtreg test uses the same language
    level as `--release`.

Examples
- Toggle fast‑path:
  - baseline: `-Dcf.skipNonnullFastPath=false`
  - update:   `-Dcf.skipNonnullFastPath=true`
- jtreg with matching release:
  - `-Dharness.release=17 -Dcf.skipNonnullFastPath=true`

### 3) Pick the right engine

| Engine   | Use when              | Pros                                  | Notes                                 |
| -------- | --------------------- | ------------------------------------- | ------------------------------------- |
| inproc   | Fast local iteration  | Lowest overhead; direct diagnostics   | Same JVM as CLI; best for development |
| external | Release/CI validation | Real javac process; clean environment | Slightly slower startup               |
| jtreg    | Reuse jtreg infra     | Works with existing jtreg tests       | Highest overhead; requires jtreg      |

## Reporting

- Unified `report.md` including:
  - Test configuration: protocol, runs, engine, generator parameters
  - Environment: JDK version/home, OS, CPU cores
  - Diagnostics: ERROR/WARNING counts with details and match status
  - Timing statistics: median, average, min, max, sample count, success rate
  - Baseline vs. update comparison with absolute and percentage deltas
  - Copy-paste reproduction commands for both variants
  - Full compiler flags (source opts, processor path/classpath, processors)

For jtreg engine, samples count always equals to 1 is correct because Main.java executes only one driver.runOnce() call per variant, while the --runs x (x > 1) parameter is handled internally by JtregPerfHarness which performs multiple compilations and returns aggregated statistics.

## Future Work

- Expand test signals: capture GC events, heap usage, and compare across multiple JDKs.
- Broaden coverage: add more generators to exercise diverse code shapes and workloads.
- Improve UX: detect malformed CLI inputs and surface actionable warnings/errors.

Fixes #1411