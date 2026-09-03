/*
 * @test
 * @summary jtreg-side wrapper that performs a single compilation timing. Cross/SINGLE orchestration is done by the CLI.
 * @run main/timeout=600 JtregPerfHarness
 */

import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

public class JtregPerfHarness {

    public static void main(String[] args) throws Exception {
        Path srcDir =
                Paths.get(System.getProperty("harness.srcdir", ".")).toAbsolutePath().normalize();
        boolean skipFastPath = Boolean.getBoolean("cf.skipNonnullFastPath");
        int runs = Integer.parseInt(System.getProperty("perf.runs", "1"));

        List<Path> sources = new ArrayList<>();
        Files.walk(srcDir)
                .forEach(
                        p -> {
                            if (p.toString().endsWith(".java")) sources.add(p);
                        });
        if (sources.isEmpty())
            throw new IllegalStateException("No .java files found in harness.srcdir: " + srcDir);

        List<Long> timings = new ArrayList<>();
        for (int i = 0; i < runs; i++) {
            long time = runOnce(sources, skipFastPath);
            timings.add(time);
        }

        Result result = new Result(timings);
        result.printReport();
    }

    // Execute one compile and return wall time (ms).
    private static long runOnce(List<Path> sources, boolean skipFastPath) throws Exception {
        long start = System.nanoTime();
        ExecResult r = compileWithCF(sources, skipFastPath);
        long end = System.nanoTime();
        if (r.exitCode != 0)
            throw new RuntimeException(
                    "javac failed with exit code " + r.exitCode + ". Output:\n" + r.stdout);
        return (end - start) / 1_000_000L;
    }

    // Build and execute a javac command. Returns exit code and stdout.
    private static ExecResult compileWithCF(List<Path> sources, boolean skipFastPath)
            throws Exception {
        String javac = findJavac();
        List<String> cmd = new ArrayList<>();
        cmd.add(javac);
        // Add required --add-opens for JDK 9+
        String[] pkgs =
                new String[] {
                    "com.sun.tools.javac.api", "com.sun.tools.javac.code",
                            "com.sun.tools.javac.comp",
                    "com.sun.tools.javac.file", "com.sun.tools.javac.main",
                            "com.sun.tools.javac.parser",
                    "com.sun.tools.javac.processing", "com.sun.tools.javac.tree",
                            "com.sun.tools.javac.util"
                };
        for (String p : pkgs) {
            cmd.add("-J--add-opens=jdk.compiler/" + p + "=ALL-UNNAMED");
        }
        if (skipFastPath) {
            cmd.add("-J-Dcf.skipNonnullFastPath=true");
        }

        String release = System.getProperty("harness.release", "17").trim();
        cmd.add("--release");
        cmd.add(release);

        String processor =
                System.getProperty(
                        "harness.processor",
                        "org.checkerframework.checker.nullness.NullnessChecker");
        cmd.add("-processor");
        cmd.add(processor);

        // Use processorpath from system property injected by JtregDriver.
        String ppath = System.getProperty("harness.processorpath", "").trim();
        if (ppath.isEmpty()) {
            throw new IllegalStateException("-Dharness.processorpath was not set");
        }
        cmd.add("-processorpath");
        cmd.add(ppath);
        cmd.add("-classpath");
        cmd.add(ppath);

        cmd.add("-proc:only");
        cmd.add("-Xlint:-options");

        // Add all source files. Since we run from the source dir, just use file names.
        for (Path p : sources) {
            cmd.add(p.getFileName().toString());
        }

        Path srcDir =
                Paths.get(System.getProperty("harness.srcdir", ".")).toAbsolutePath().normalize();
        ProcessBuilder pb = new ProcessBuilder(cmd);
        pb.directory(srcDir.toFile());
        pb.redirectErrorStream(true);
        Process p = pb.start();
        byte[] out = p.getInputStream().readAllBytes();
        int code = p.waitFor();
        return new ExecResult(code, new String(out, StandardCharsets.UTF_8));
    }

    private static final class ExecResult {
        final int exitCode;
        final String stdout;

        ExecResult(int exitCode, String stdout) {
            this.exitCode = exitCode;
            this.stdout = stdout;
        }
    }

    private static String findJavac() {
        String javaHome = System.getProperty("java.home");
        java.io.File jh = new java.io.File(javaHome);
        java.io.File bin = new java.io.File(jh.getParentFile(), "bin");
        java.io.File exe = new java.io.File(bin, isWindows() ? "javac.exe" : "javac");
        if (exe.exists()) return exe.getAbsolutePath();
        return isWindows() ? "javac.exe" : "javac";
    }

    private static boolean isWindows() {
        String os = System.getProperty("os.name", "").toLowerCase(java.util.Locale.ROOT);
        return os.contains("win");
    }

    private static final class Result {
        final List<Long> timingsMs;

        Result(List<Long> t) {
            this.timingsMs = Collections.unmodifiableList(new ArrayList<>(t));
        }

        double average() {
            return timingsMs.stream().mapToLong(Long::longValue).average().orElse(0);
        }

        double median() {
            if (timingsMs.isEmpty()) return 0;
            List<Long> copy = new ArrayList<>(timingsMs);
            Collections.sort(copy);
            int n = copy.size();
            return (n & 1) == 1 ? copy.get(n / 2) : (copy.get(n / 2 - 1) + copy.get(n / 2)) / 2.0;
        }

        // Prints a machine-readable report to stdout for JtregDriver to parse.
        void printReport() {
            // HARNESS_RESULT: median=123.0, average=124.5, samples=[120,123,129]
            StringBuilder sb = new StringBuilder();
            sb.append("HARNESS_RESULT: ");
            sb.append("median=").append(median()).append(", ");
            sb.append("average=").append(average()).append(", ");
            sb.append("samples=").append(timingsMs);
            System.out.println(sb.toString());
        }
    }
}
