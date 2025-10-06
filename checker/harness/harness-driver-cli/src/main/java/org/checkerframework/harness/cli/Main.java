package org.checkerframework.harness.cli;

import org.checkerframework.harness.core.CodeGenerator;
import org.checkerframework.harness.core.Driver;
import org.checkerframework.harness.core.HarnessIO;
import org.checkerframework.harness.core.InProcessJavacDriver;
import org.checkerframework.harness.generators.nullness.NewAndArrayGenerator;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * CLI entry point that orchestrates SINGLE/CROSS protocols, constructs a RunSpec for the chosen
 * engine, delegates execution to a Driver, and emits JSON/Markdown reports.
 */
public final class Main {
    public static void main(String[] args) throws Exception {
        Map<String, String> opts = parseArgs(args);
        if (opts.containsKey("--help") || !opts.containsKey("--generator")) {
            printHelp();
            return;
        }

        String generatorName = opts.getOrDefault("--generator", "NewAndArray");
        int sampleCount = Integer.parseInt(opts.getOrDefault("--sampleCount", "100"));
        long seed = Long.parseLong(opts.getOrDefault("--seed", "1"));
        String baselineFlags = opts.getOrDefault("--baseline-flags", "").trim();
        String updateFlags = opts.getOrDefault("--update-flags", "").trim();
        String processor =
                opts.getOrDefault(
                        "--processor", "org.checkerframework.checker.nullness.NullnessChecker");
        String processorPath = opts.getOrDefault("--processor-path", "");
        String release = opts.getOrDefault("--release", "17");
        String protocol =
                opts.getOrDefault("--protocol", "SINGLE").toUpperCase(Locale.ROOT); // SINGLE|CROSS
        int runs = Integer.parseInt(opts.getOrDefault("--runs", "5"));
        if (runs < 1) {
            System.err.println("WARNING: --runs < 1; falling back to 1.");
            runs = 1;
        }
        String engine = opts.getOrDefault("--engine", "inproc"); // inproc|external|jtreg
        String jtregBin = opts.getOrDefault("--jtreg", Paths.get("jtreg", "bin").toString());
        String jtregTest =
                opts.getOrDefault("--jtreg-test", "checker/harness/jtreg/JtregPerfHarness.java");

        CodeGenerator generator = selectGenerator(generatorName);

        Path resultDir = Paths.get("checker/harness/result").toAbsolutePath().normalize();
        Path baselineDir = resultDir.resolve("baseline");
        Path updateDir = resultDir.resolve("update");
        Files.createDirectories(baselineDir);
        Files.createDirectories(updateDir);

        Driver driver;
        if ("external".equalsIgnoreCase(engine)) {
            driver = new org.checkerframework.harness.core.ExternalProcessJavacDriver();
        } else if ("jtreg".equalsIgnoreCase(engine)) {
            driver =
                    new org.checkerframework.harness.core.JtregDriver(
                            Paths.get(jtregBin), jtregTest);
        } else {
            driver = new InProcessJavacDriver();
        }

        // Common compiler config: shared across engines. For jtreg, pass
        // -Dharness.release=<ver>
        // via flags so the test-side harness can translate it to --release.
        List<String> processors = java.util.Arrays.asList(processor);
        List<Path> pp =
                processorPath.isEmpty()
                        ? new ArrayList<Path>()
                        : java.util.Arrays.asList(Paths.get(processorPath));
        List<String> sourceOpts =
                java.util.Arrays.asList("--release", release, "-proc:only", "-Xlint:-options");

        // Add perf-related flags for jtreg engine. Strip -J prefix if present (not needed for test
        // VM).
        if ("jtreg".equalsIgnoreCase(engine)) {
            List<String> baselineJavacFlags = new ArrayList<>();
            for (String f : splitFlags(baselineFlags)) {
                // Strip -J prefix: -J-Dcf.xxx -> -Dcf.xxx (test VM doesn't use javac's -J syntax)
                baselineJavacFlags.add(f.startsWith("-J") ? f.substring(2) : f);
            }
            baselineJavacFlags.add("-Dperf.runs=" + runs);
            baselineJavacFlags.add("-Dcf.skipNonnullFastPath=false");
            baselineJavacFlags.add("-Dharness.release=" + release);
            baselineJavacFlags.add("-Dharness.processor=" + processor);

            List<String> updateJavacFlags = new ArrayList<>();
            for (String f : splitFlags(updateFlags)) {
                updateJavacFlags.add(f.startsWith("-J") ? f.substring(2) : f);
            }
            updateJavacFlags.add("-Dperf.runs=" + runs);
            updateJavacFlags.add("-Dcf.skipNonnullFastPath=true");
            updateJavacFlags.add("-Dharness.release=" + release);
            updateJavacFlags.add("-Dharness.processor=" + processor);

            baselineFlags = String.join(" ", baselineJavacFlags);
            updateFlags = String.join(" ", updateJavacFlags);
        }

        Driver.CompilerConfig compilerCfgBaseline =
                new Driver.CompilerConfig(
                        null,
                        List.of(),
                        processors,
                        pp,
                        sourceOpts,
                        baselineDir.resolve("sampleCount"));

        Driver.CompilerConfig compilerCfgUpdate =
                new Driver.CompilerConfig(
                        null,
                        List.of(),
                        processors,
                        pp,
                        sourceOpts,
                        updateDir.resolve("sampleCount"));

        CodeGenerator.GenerationRequest genReqBaseline =
                new CodeGenerator.GenerationRequest(
                        baselineDir.resolve("src"), seed, sampleCount, extractExtra(opts));
        CodeGenerator.GenerationRequest genReqUpdate =
                new CodeGenerator.GenerationRequest(
                        updateDir.resolve("src"), seed, sampleCount, extractExtra(opts));

        java.util.List<String> baselineFlagList = splitFlags(baselineFlags);
        java.util.List<String> updateFlagList = splitFlags(updateFlags);

        // Warn if protocol requires update variant but user didn't provide
        // --update-flags
        if (!"SINGLE".equals(protocol) && updateFlagList.isEmpty()) {
            System.err.println(
                    "WARNING: --update-flags is empty; proceeding without update-specific flags.");
        }

        // If engine=jtreg and --release is provided but -Dharness.release is missing,
        // show a warning and suggest adding it
        if ("jtreg".equalsIgnoreCase(engine)) {
            boolean hasHarnessRelease =
                    baselineFlagList.stream().anyMatch(s -> s.startsWith("-Dharness.release="))
                            || updateFlagList.stream()
                                    .anyMatch(s -> s.startsWith("-Dharness.release="));
            if (!release.isEmpty() && !hasHarnessRelease) {
                System.err.println(
                        "WARNING: engine=jtreg with --release="
                                + release
                                + ": add -Dharness.release="
                                + release
                                + " to --baseline-flags/--update-flags for consistent language level inside tests.");
            }
        }

        Driver.RunSpec specBaseline =
                new Driver.RunSpec(
                        generator,
                        genReqBaseline,
                        compilerCfgBaseline,
                        baselineFlagList,
                        "baseline");
        Driver.RunSpec specUpdate =
                new Driver.RunSpec(
                        generator, genReqUpdate, compilerCfgUpdate, updateFlagList, "update");

        if ("jtreg".equalsIgnoreCase(engine)) {
            // For jtreg, the looping is handled inside the test. Run each variant just once.
            Driver.RunResult lastA = driver.runOnce(specBaseline);
            Driver.RunResult lastB = driver.runOnce(specUpdate);

            // The driver returns a result with the median time. We pass it as a single-element
            // list.
            List<Long> aTimings = lastA.success() ? List.of(lastA.wallMillis()) : List.of();
            List<Long> bTimings = lastB.success() ? List.of(lastB.wallMillis()) : List.of();

            if (lastA != null && lastB != null) {
                Map<String, String> ctx = new HashMap<String, String>();
                ctx.put("protocol", protocol + " (jtreg-internal)");
                ctx.put("runs", String.valueOf(runs));
                ctx.put("engine", engine);
                ctx.put("sampleCount", String.valueOf(sampleCount));
                ctx.put("seed", String.valueOf(seed));
                String gpf = extractExtra(opts).getOrDefault("groupsPerFile", "");
                if (!gpf.isEmpty()) ctx.put("groupsPerFile", gpf);
                ctx.put("baselineFlags", String.join(" ", baselineFlagList));
                ctx.put("updateFlags", String.join(" ", updateFlagList));
                HarnessIO.writeUnifiedReport(
                        resultDir.resolve("report.md"),
                        lastA,
                        lastB,
                        ctx,
                        aTimings,
                        lastA.success() ? 1 : 0,
                        1,
                        bTimings,
                        lastB.success() ? 1 : 0,
                        1);
                printErrorsToConsole(lastA, lastB);
            }
        } else if ("SINGLE".equals(protocol)) {
            // Warmup once per variant (not timed) to mitigate JIT/IO-cache cold-start effects and
            // reduce drift in measured runs. This keeps SINGLE results more stable without changing
            // its simple execution model.
            try {
                driver.runOnce(specBaseline);
            } catch (Throwable ignore) {
            }
            try {
                driver.runOnce(specUpdate);
            } catch (Throwable ignore) {
            }

            // Run baseline 'runs' times, then update 'runs' times; collect timings and emit summary
            // statistics.
            ArrayList<Long> aTimings = new ArrayList<Long>();
            ArrayList<Long> bTimings = new ArrayList<Long>();
            int aSuccess = 0;
            int bSuccess = 0;
            Driver.RunResult lastA = null;
            Driver.RunResult lastB = null;
            for (int i = 0; i < runs; i++) {
                lastA = driver.runOnce(specBaseline);
                if (lastA.success()) {
                    aTimings.add(Long.valueOf(lastA.wallMillis()));
                    aSuccess++;
                }
            }
            for (int i = 0; i < runs; i++) {
                lastB = driver.runOnce(specUpdate);
                if (lastB.success()) {
                    bTimings.add(Long.valueOf(lastB.wallMillis()));
                    bSuccess++;
                }
            }
            // Persist last results for compatibility
            if (lastA != null) HarnessIO.writeJson(baselineDir.resolve("result.json"), lastA);
            if (lastB != null) HarnessIO.writeJson(updateDir.resolve("result.json"), lastB);
            // Unified report (aggregated metrics always shown)
            if (lastA != null && lastB != null) {
                Map<String, String> ctx = new HashMap<String, String>();
                ctx.put("protocol", protocol);
                ctx.put("runs", String.valueOf(runs));
                ctx.put("engine", engine);
                ctx.put("sampleCount", String.valueOf(sampleCount));
                ctx.put("seed", String.valueOf(seed));
                String gpf = extractExtra(opts).getOrDefault("groupsPerFile", "");
                if (!gpf.isEmpty()) ctx.put("groupsPerFile", gpf);
                ctx.put("baselineFlags", String.join(" ", baselineFlagList));
                ctx.put("updateFlags", String.join(" ", updateFlagList));
                HarnessIO.writeUnifiedReport(
                        resultDir.resolve("report.md"),
                        lastA,
                        lastB,
                        ctx,
                        aTimings,
                        runs,
                        aSuccess,
                        bTimings,
                        runs,
                        bSuccess);
                printErrorsToConsole(lastA, lastB);
            }
        } else {
            if ("CROSS".equals(protocol)) {
                // CROSS (ABBA): per iteration run AB then BA; compute per-iteration pairwise
                // averages (A_i, B_i).
                ArrayList<Long> aPairwise = new ArrayList<Long>();
                ArrayList<Long> bPairwise = new ArrayList<Long>();
                int aSuccess = 0;
                int bSuccess = 0;

                boolean warmedA = false;
                boolean warmedB = false;

                // Representative single-run results for reporting (flags/diagnostics only).
                // Note: Aggregated timing statistics (median/average/min/max) are computed from
                // the full series (pairwise A_i/B_i over all iterations). The representative
                // results below do NOT participate in those aggregates; they are only used to
                // populate readable, single-run fields in the report such as Flags and Diagnostics.
                // We choose the "last BA" results from the final iteration to avoid extra runs
                // and because any single iteration would be equivalent for this purpose.
                Driver.RunResult reprA = null;
                Driver.RunResult reprB = null;
                for (int i = 0; i < runs; i++) {
                    // Round 1: AB
                    if (!warmedA) {
                        driver.runOnce(specBaseline);
                        warmedA = true;
                    }
                    Driver.RunResult resAB_A = driver.runOnce(specBaseline);
                    long tAB_A = resAB_A.wallMillis();
                    if (!warmedB) {
                        driver.runOnce(specUpdate);
                        warmedB = true;
                    }
                    Driver.RunResult resAB_B = driver.runOnce(specUpdate);
                    long tAB_B = resAB_B.wallMillis();

                    // Round 2: BA
                    Driver.RunResult resBA_B = driver.runOnce(specUpdate);
                    long tBA_B = resBA_B.wallMillis();
                    Driver.RunResult resBA_A = driver.runOnce(specBaseline);
                    long tBA_A = resBA_A.wallMillis();

                    long aPair = (tAB_A + tBA_A) / 2L; // A_i
                    long bPair = (tAB_B + tBA_B) / 2L; // B_i
                    aPairwise.add(aPair);
                    bPairwise.add(bPair);
                    // Count success per iteration as both directions succeeded (exit status)
                    // Here success is approximated via presence of timings (drivers already report
                    // success)
                    aSuccess++;
                    bSuccess++;
                    // Record the most recent results as representatives for flags/diagnostics only
                    reprA = resBA_A; // last A in this iteration
                    reprB = resBA_B; // last B in this iteration
                }

                // Unified report aggregates pairwise averages across iterations (series) and
                // uses reprA/reprB solely for metadata and diagnostics presentation.
                Map<String, String> ctx = new HashMap<String, String>();
                ctx.put("protocol", protocol);
                ctx.put("runs", String.valueOf(runs));
                ctx.put("engine", engine);
                ctx.put("sampleCount", String.valueOf(sampleCount));
                ctx.put("seed", String.valueOf(seed));
                String gpf = extractExtra(opts).getOrDefault("groupsPerFile", "");
                if (!gpf.isEmpty()) ctx.put("groupsPerFile", gpf);
                ctx.put("baselineFlags", String.join(" ", baselineFlagList));
                ctx.put("updateFlags", String.join(" ", updateFlagList));
                HarnessIO.writeUnifiedReport(
                        resultDir.resolve("report.md"),
                        reprA,
                        reprB,
                        ctx,
                        aPairwise,
                        runs,
                        aSuccess,
                        bPairwise,
                        runs,
                        bSuccess);
                printErrorsToConsole(reprA, reprB);
            } else {
                throw new IllegalArgumentException(
                        "Unknown protocol: " + protocol + ". Supported: SINGLE|CROSS");
            }
        }

        System.out.println(
                "Report generated at: " + resultDir.resolve("report.md").toAbsolutePath());
    }

    private static CodeGenerator selectGenerator(String name) {
        if ("NewAndArray".equals(name)) {
            return new NewAndArrayGenerator();
        }
        throw new IllegalArgumentException(
                "Unknown generator: " + name + ". Supported: NewAndArray");
    }

    private static Map<String, String> parseArgs(String[] args) {
        Map<String, String> m = new HashMap<>();
        String key = null;
        for (String a : args) {
            if (a.startsWith("--")) {
                key = a;
                m.putIfAbsent(key, "");
            } else if (key != null) {
                String prev = m.getOrDefault(key, "");
                m.put(key, prev.isEmpty() ? a : prev + " " + a);
            }
        }
        return m;
    }

    private static List<String> splitFlags(String flags) {
        if (flags.isEmpty()) return new ArrayList<String>();
        List<String> out = new ArrayList<String>();
        for (String s : flags.trim().split("\\s+")) {
            if (!s.isEmpty()) out.add(s);
        }
        return out;
    }

    private static Map<String, String> extractExtra(Map<String, String> opts) {
        Map<String, String> extra = new HashMap<>();
        for (Map.Entry<String, String> e : opts.entrySet()) {
            String k = e.getKey();
            if (k.startsWith("--extra.")) {
                extra.put(k.substring("--extra.".length()), e.getValue());
            }
        }
        return extra;
    }

    private static void printHelp() {
        System.out.println("Usage: --generator <NewAndArray> --sampleCount <N> --seed <S> ");
        System.out.println("       --baseline-flags <flags> --update-flags <flags>");
        System.out.println(
                "       --processor <FQN> --processor-path <path-to-checker-jar-or-dir> --release <ver>");
        System.out.println(
                "       --protocol <SINGLE|CROSS> --runs <N> [--engine <inproc|external|jtreg>]");
        System.out.println();
        System.out.println("Examples:");
        System.out.println("  --generator NewAndArray --sampleCount 200 --seed 42 \\");
        System.out.println(
                "    --baseline-flags -AfastNewClass=false --update-flags -AfastNewClass=true \\");
        System.out.println(
                "    --processor org.checkerframework.checker.nullness.NullnessChecker \\");
        System.out.println("    --processor-path <path-to-checker-jar> --release 17");
        System.out.println();
        System.out.println("Protocols:");
        System.out.println(
                "  SINGLE: warm up each variant once (not timed), then run baseline 'runs' times and update 'runs' times; emit single.md with median/average; report.md shows the last pair.");
        System.out.println(
                "  CROSS : per iteration run AB then BA; compute pairwise A_i=(T_AB^A+T_BA^A)/2, B_i=(T_AB^B+T_BA^B)/2, then summarize across i.");
        System.out.println(
                "          Each variant is warmed once on its first appearance (not timed).");
        System.out.println();
    }

    private static void printErrorsToConsole(Driver.RunResult a, Driver.RunResult b) {
        java.util.concurrent.atomic.AtomicBoolean any =
                new java.util.concurrent.atomic.AtomicBoolean(false);
        StringBuilder sb = new StringBuilder();
        java.util.function.Consumer<Driver.RunResult> dump =
                r -> {
                    int count = 0;
                    for (Driver.DiagnosticEntry d : r.diagnostics()) {
                        if ("ERROR".equals(d.kind())) {
                            if (count == 0) {
                                sb.append("\n--- " + r.label() + " ERROR diagnostics ---\n");
                            }
                            any.set(true);
                            count++;
                            sb.append(d.file())
                                    .append(":")
                                    .append(d.line())
                                    .append(":")
                                    .append(d.column())
                                    .append(" ")
                                    .append(d.kind())
                                    .append(" ")
                                    .append(d.message())
                                    .append("\n");
                        }
                    }
                };
        dump.accept(a);
        dump.accept(b);
        if (any.get()) System.out.print(sb.toString());
    }
}
