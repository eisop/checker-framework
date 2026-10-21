package org.checkerframework.harness.core;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

/**
 * Executes a single jtreg test and returns timing, diagnostics, and metadata.
 *
 * <p>Behavior: - Measures only the jtreg process runtime (stdout is read after timing). - Persists
 * the full stdout next to the generated sources. - Reports a single ERROR diagnostic on non-zero
 * exit; detailed aggregation is handled by the CLI. - VM/javac options are forwarded via jtreg
 * command-line flags. - Source generation is kept to comply with {@link Driver} contract; jtreg
 * tests may generate their own sources and do not consume the generated ones here.
 *
 * <p>Protocol orchestration (SINGLE/CROSS) is implemented by the CLI, not by this driver.
 */
public final class JtregDriver implements Driver {

    private final Path jtregBin; // ${root}/jtreg/bin/jtreg
    private final String testPath; // e.g. checker/harness/jtreg/JtregPerfHarness.java

    public JtregDriver(Path jtregBin, String testPath) {
        this.jtregBin = jtregBin;
        this.testPath = testPath;
    }

    /**
     * Runs a single jtreg test (typically {@code JtregPerfHarness}) and reports timing and
     * diagnostics.
     *
     * @param spec run specification; generated sources are used as a working directory and artifact
     *     sink
     * @return a {@link Driver.RunResult} whose {@code wallMillis} is jtreg runtime or parsed median
     *     when available
     * @throws Exception if source generation or process setup fails; jtreg failures are captured as
     *     diagnostics
     */
    @Override
    public RunResult runOnce(RunSpec spec) throws Exception {
        Objects.requireNonNull(spec);
        // Still generate sources per interface (not used by jtreg), to keep a workDir
        CodeGenerator.GenerationResult genRes = spec.generator().generate(spec.genReq());

        List<String> cmd = new ArrayList<String>();
        cmd.add(jtregExecutable());
        // Use samevm for faster execution (parity with NewClassPerf.java); print summary to console
        cmd.addAll(Arrays.asList("-samevm", "-verbose:summary"));

        // Jtreg should run from the project root (where checker/ is), not harness-driver-cli
        File projectRoot = findProjectRoot();

        // Build -vmoptions string with all test VM properties
        StringBuilder vmOpts = new StringBuilder();

        if (spec.javacFlags() != null) {
            for (String f : spec.javacFlags()) {
                if (f == null) continue;
                String opt = f.trim();
                if (opt.isEmpty()) continue;
                // All flags go to test VM (JtregPerfHarness constructs its own javac command)
                if (vmOpts.length() > 0) vmOpts.append(' ');
                vmOpts.append(opt);
            }
        }

        // Ensure the test VM can find the generated sources directory (must be absolute)
        if (genRes.sourcesDir() != null) {
            Path absSrcDir =
                    genRes.sourcesDir().isAbsolute()
                            ? genRes.sourcesDir().normalize()
                            : projectRoot.toPath().resolve(genRes.sourcesDir()).normalize();
            if (vmOpts.length() > 0) vmOpts.append(' ');
            vmOpts.append("-Dharness.srcdir=").append(absSrcDir.toString());
        }

        // Inject processorpath for JtregPerfHarness (resolve relative paths to absolute)
        if (spec.compiler().processorPath() != null && !spec.compiler().processorPath().isEmpty()) {
            String cp = joinPathsResolved(spec.compiler().processorPath());
            if (vmOpts.length() > 0) vmOpts.append(' ');
            vmOpts.append("-Dharness.processorpath=").append(cp);
        }

        // Commit aggregated options to command
        if (vmOpts.length() > 0) {
            cmd.add("-vmoptions:" + vmOpts.toString());
        }

        // Work/report dirs under the harness workDir
        Path workDir = genRes.sourcesDir().resolve("jtreg-work");
        Path reportDir = genRes.sourcesDir().resolve("jtreg-report");
        // Clean previous jtreg artifacts to avoid stale .jtr affecting parsing across runs
        try {
            deleteRecursively(workDir);
        } catch (Throwable ignore) {
        }
        try {
            deleteRecursively(reportDir);
        } catch (Throwable ignore) {
        }
        try {
            Files.createDirectories(workDir);
            Files.createDirectories(reportDir);
        } catch (Throwable ignore) {
        }
        cmd.add("-w");
        cmd.add(workDir.toString());
        cmd.add("-r");
        cmd.add(reportDir.toString());

        // Convert test path to absolute from project root, since jtreg runs there
        String absTestPath = testPath;
        try {
            Path tp = Paths.get(testPath);
            if (!tp.isAbsolute()) {
                tp = projectRoot.toPath().resolve(testPath).normalize();
            }
            absTestPath = tp.toString();
        } catch (Throwable ignore) {
        }
        cmd.add(absTestPath);

        ProcessBuilder pb = new ProcessBuilder(cmd);
        pb.directory(projectRoot);
        pb.redirectErrorStream(true);
        Path stdoutPath = genRes.sourcesDir().resolve("jtreg-stdout.txt");
        File stdoutFile = stdoutPath.toFile();
        try {
            stdoutFile.getParentFile().mkdirs();
        } catch (Throwable ignore) {
        }
        pb.redirectOutput(stdoutFile);

        // Measure only jtreg execution; avoid IO deadlock by redirecting output to file
        long start = System.nanoTime();
        Process p = pb.start();
        // Allow override via -Dharness.timeoutSec, default 600s
        int timeoutSec = 600;
        try {
            String ts = System.getProperty("harness.timeoutSec", "600");
            timeoutSec = Integer.parseInt(ts.trim());
        } catch (Throwable ignore) {
        }
        boolean finished = p.waitFor(timeoutSec, java.util.concurrent.TimeUnit.SECONDS);
        int code;
        if (!finished) {
            // Timeout: kill process tree if possible
            try {
                killProcessTree(p);
            } catch (Throwable ignore) {
            }
            code = Integer.MIN_VALUE; // special code for timeout
        } else {
            code = p.exitValue();
        }
        long end = System.nanoTime();
        String stdout = "";
        try {
            if (stdoutFile.exists()) {
                stdout = Files.readString(stdoutPath, StandardCharsets.UTF_8);
            }
        } catch (Throwable ignore) {
        }

        List<Driver.DiagnosticEntry> diags = new ArrayList<Driver.DiagnosticEntry>();
        if (code != 0) {
            String msg =
                    (code == Integer.MIN_VALUE)
                            ? ("jtreg timed out after " + timeoutSec + "s.\n" + stdout)
                            : stdout;
            diags.add(new Driver.DiagnosticEntry("<jtreg>", 0, 0, "ERROR", msg));
        }

        // Find HARNESS_RESULT line in .jtr file and parse median timing
        long median = findMedianFromJtr(workDir);

        if (median == -1 && code == 0) {
            // Succeeded but couldn't parse results, which is a failure of the harness itself.
            diags.add(
                    new Driver.DiagnosticEntry(
                            "<jtreg>",
                            0,
                            0,
                            "ERROR",
                            "Failed to parse HARNESS_RESULT from test output."));
            code = 1; // Mark as failure
        }

        Map<String, Object> meta = new java.util.HashMap<String, Object>();
        meta.put("timestamp", Instant.now().toString());
        meta.put("generator", spec.generator().name());
        meta.put("label", spec.label());
        meta.put(
                "flags",
                String.join(
                        " ",
                        spec.javacFlags() == null
                                ? java.util.Collections.<String>emptyList()
                                : spec.javacFlags()));
        meta.put("jtregExit", Integer.valueOf(code));
        meta.put("stdoutPath", stdoutPath.toString());
        if (code == Integer.MIN_VALUE) meta.put("timeoutSec", Integer.valueOf(timeoutSec));
        // Minimal metrics; optionally enrich from .jtr when available
        Map<String, Object> metrics = new java.util.HashMap<String, Object>();
        try {
            Long jtrElapsed = findElapsedFromJtr(workDir);
            if (jtrElapsed != null) metrics.put("jtrElapsedMillis", jtrElapsed);
            String jtrStatus = findResultStatusFromJtr(workDir);
            if (jtrStatus != null) meta.put("jtrResult", jtrStatus);
        } catch (Throwable ignore) {
        }
        RunResult result =
                new RunResult(
                        spec.label(),
                        median != -1
                                ? median
                                : (end - start) / 1_000_000L, // Use median if available
                        code == 0,
                        java.util.Collections.unmodifiableList(diags),
                        metrics,
                        genRes.sourcesDir(),
                        meta);
        HarnessIO.writeJson(genRes.sourcesDir().resolve("result.json"), result);
        return result;
    }

    private static void deleteRecursively(Path p) throws java.io.IOException {
        if (p == null || !Files.exists(p)) return;
        java.util.List<Path> paths = new java.util.ArrayList<>();
        Files.walk(p).forEach(paths::add);
        // delete children first
        for (int i = paths.size() - 1; i >= 0; i--) {
            try {
                Files.deleteIfExists(paths.get(i));
            } catch (Throwable ignore) {
            }
        }
    }

    /** Extract median timing, elapsed time, and result status from .jtr file in a single pass. */
    private static long findMedianFromJtr(Path workDir) {
        if (workDir == null) return -1;
        try {
            java.util.Optional<Path> jtr =
                    java.nio.file.Files.walk(workDir)
                            .filter(p -> p.getFileName().toString().endsWith(".jtr"))
                            .findFirst();
            if (!jtr.isPresent()) return -1;

            String content = Files.readString(jtr.get(), StandardCharsets.UTF_8);
            for (String line : content.split("\\R")) {
                if (line.startsWith("HARNESS_RESULT:")) {
                    String data = line.substring("HARNESS_RESULT:".length()).trim();
                    for (String part : data.split(",")) {
                        String[] kv = part.trim().split("=");
                        if (kv.length == 2 && "median".equals(kv[0])) {
                            return (long) Double.parseDouble(kv[1]);
                        }
                    }
                }
            }
        } catch (Throwable ignore) {
        }
        return -1;
    }

    private static Long findElapsedFromJtr(Path workDir) {
        if (workDir == null) return null;
        try {
            java.util.Optional<Path> jtr =
                    java.nio.file.Files.walk(workDir)
                            .filter(p -> p.getFileName().toString().endsWith(".jtr"))
                            .findFirst();
            if (!jtr.isPresent()) return null;

            for (String line : Files.readAllLines(jtr.get(), StandardCharsets.UTF_8)) {
                int idx = line.indexOf("elapsed=");
                if (idx >= 0) {
                    int end = line.indexOf(' ', idx);
                    String num =
                            (end > idx) ? line.substring(idx + 8, end) : line.substring(idx + 8);
                    try {
                        return Long.valueOf(Long.parseLong(num.trim()));
                    } catch (NumberFormatException ignore) {
                    }
                }
            }
        } catch (Throwable ignore) {
        }
        return null;
    }

    private static String findResultStatusFromJtr(Path workDir) {
        if (workDir == null) return null;
        try {
            java.util.Optional<Path> jtr =
                    java.nio.file.Files.walk(workDir)
                            .filter(p -> p.getFileName().toString().endsWith(".jtr"))
                            .findFirst();
            if (!jtr.isPresent()) return null;

            for (String line : Files.readAllLines(jtr.get(), StandardCharsets.UTF_8)) {
                if (line.startsWith("result:")) return line.substring("result:".length()).trim();
                if (line.startsWith("test result:"))
                    return line.substring("test result:".length()).trim();
            }
        } catch (Throwable ignore) {
        }
        return null;
    }

    private String jtregExecutable() {
        Path bin = jtregBin;
        if (bin != null) {
            File f = bin.toFile();
            if (f.isDirectory()) {
                File jt = new File(f, isWindows() ? "jtreg.exe" : "jtreg");
                if (jt.exists()) return jt.getPath();
            } else if (f.exists()) {
                return f.getPath();
            }
        }
        // Fallback: search upwards for repo-level jtreg/bin/jtreg
        Path cwd = Paths.get(".").toAbsolutePath().normalize();
        for (int i = 0; i < 6; i++) {
            Path base = cwd;
            for (int j = 0; j < i; j++) base = base.getParent();
            if (base == null) break;
            File jt =
                    base.resolve("jtreg")
                            .resolve("bin")
                            .resolve(isWindows() ? "jtreg.exe" : "jtreg")
                            .toFile();
            if (jt.exists()) return jt.getPath();
        }
        return "jtreg";
    }

    private static boolean isWindows() {
        String os = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
        return os.contains("win");
    }

    private static String joinPaths(List<Path> paths) {
        String sep = File.pathSeparator;
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < paths.size(); i++) {
            if (i > 0) sb.append(sep);
            sb.append(paths.get(i).toString());
        }
        return sb.toString();
    }

    /**
     * Joins paths while resolving relative paths to absolute paths based on current working
     * directory. This ensures paths work correctly even when jtreg changes its working directory.
     */
    private static String joinPathsResolved(List<Path> paths) {
        String sep = File.pathSeparator;
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < paths.size(); i++) {
            if (i > 0) sb.append(sep);
            Path p = paths.get(i);
            // Convert relative paths to absolute paths based on current working directory
            // This prevents issues when jtreg changes the working directory
            if (!p.isAbsolute()) {
                try {
                    p = p.toAbsolutePath().normalize();
                } catch (Throwable ignore) {
                    // Fallback to original path if resolution fails
                }
            }
            sb.append(p.toString());
        }
        return sb.toString();
    }

    private static File findProjectRoot() {
        // When running via gradlew from checker/harness, user.dir is typically harness-driver-cli
        // Walk up from user.dir until we find a directory containing:
        // - checker/ subdirectory AND
        // - build.gradle or settings.gradle at the same level
        String userDir = System.getProperty("user.dir", ".");
        File candidate = new File(userDir).getAbsoluteFile();

        for (int i = 0; i < 8; i++) {
            File checkerDir = new File(candidate, "checker");
            File checkerBuildGradle = new File(checkerDir, "build.gradle");
            File rootBuildGradle = new File(candidate, "build.gradle");
            File settingsGradle = new File(candidate, "settings.gradle");

            // Check if this directory contains checker/ with its own build.gradle AND a root gradle
            // file
            if (checkerDir.exists()
                    && checkerDir.isDirectory()
                    && checkerBuildGradle.exists()
                    && (rootBuildGradle.exists() || settingsGradle.exists())) {
                return candidate;
            }

            // Move up one level
            File parent = candidate.getParentFile();
            if (parent == null) break;
            candidate = parent;
        }

        // Fallback: return original user.dir
        return new File(userDir).getAbsoluteFile();
    }

    private static void killProcessTree(Process p) {
        if (p == null) return;
        try {
            java.lang.ProcessHandle h = p.toHandle();
            // Best-effort: kill descendants first, then the root
            try {
                h.descendants()
                        .forEach(
                                ph -> {
                                    try {
                                        ph.destroyForcibly();
                                    } catch (Throwable ignore) {
                                    }
                                });
            } catch (Throwable ignore) {
            }
            try {
                h.destroyForcibly();
            } catch (Throwable ignore) {
            }
        } catch (Throwable ignore) {
            try {
                p.destroyForcibly();
            } catch (Throwable ignore2) {
            }
        }
    }
}
