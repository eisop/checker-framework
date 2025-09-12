/*
 * @test
 * @summary Measure impact of skipping hasEffectiveAnnotation(NONNULL) fast-path on many `new` sites.
 *
 * @run main/timeout=600 NewClassPerf
 */

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class NewClassPerf {

    private static final int RUNS = Integer.getInteger("perf.runs", 10);
    private static final int GROUPS = Integer.getInteger("perf.groups", 400); // ~2k new-exprs total (5 per group)
    // Protocol: AB | BA | BOTH | SEPARATE. Default BOTH runs interleaved AB and BA to cancel order bias.
    private static final String PROTOCOL = System.getProperty("perf.protocol", "BOTH");
    private static final int WARMUP = Integer.getInteger("perf.warmupPerVariant", 1);

    public static void main(String[] args) throws Exception {
        Path workDir = Paths.get(".").toAbsolutePath().normalize();
        Path src = workDir.resolve("ManyNew.java");
        writeManyNewSource(src, GROUPS);

        switch (PROTOCOL.toUpperCase()) {
            case "AB": {
                Result[] ab = timeInterleaved(src, "AB");
                printResults("Interleaved AB", ab[0], ab[1]);
                break;
            }
            case "BA": {
                Result[] ba = timeInterleaved(src, "BA");
                printResults("Interleaved BA", ba[0], ba[1]);
                break;
            }
            case "SEPARATE": {
                Result[] sep = timeSeparate(src, WARMUP);
                printResults("Separate (warmup=" + WARMUP + ")", sep[0], sep[1]);
                break;
            }
            case "BOTH":
            default: {
                Result[] ab = timeInterleaved(src, "AB");
                Result[] ba = timeInterleaved(src, "BA");
                System.out.println("==== Interleaved AB ====");
                printResults("AB", ab[0], ab[1]);
                System.out.println();
                System.out.println("==== Interleaved BA ====");
                printResults("BA", ba[0], ba[1]);
                // Consistency check
                double sign1 = Math.signum((ab[1].median() - ab[0].median()));
                double sign2 = Math.signum((ba[1].median() - ba[0].median()));
                System.out.println();
                if (sign1 == 0 || sign2 == 0 || Math.signum(sign1) != Math.signum(sign2)) {
                    System.out.println("Direction: ORDER-SENSITIVE (results differ between AB and BA)");
                } else {
                    System.out.println("Direction: CONSISTENT across AB and BA");
                }
                break;
            }
        }
    }

    private static void writeManyNewSource(Path file, int groups) throws IOException {
        StringBuilder sb = new StringBuilder(1024 * 1024);
        sb.append("import java.util.*;\n");
        sb.append("public class ManyNew {\n");
        // Avoid initialization checker errors; use @Nullable type variable upper bound.
        sb.append("  static class Box<T extends Object> { T t; Box(){ this.t = (T) (Object) new Object(); } }\n");
        sb.append("  void f() {\n");
        for (int i = 0; i < groups; i++) {
            sb.append("    Object o").append(i).append(" = new Object();\n");
            sb.append("    ArrayList<String> l").append(i).append(" = new ArrayList<>();\n");
            sb.append("    Box<String> b").append(i).append(" = new Box<>();\n");
            sb.append("    int[] ai").append(i).append(" = new int[10];\n");
            sb.append("    String[] as").append(i).append(" = new String[10];\n");
            sb.append("    Runnable r").append(i)
              .append(" = new Runnable(){ public void run(){} };\n");
        }
        sb.append("  }\n");
        sb.append("}\n");
        try (BufferedWriter w = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
            w.write(sb.toString());
        }
    }

    private static Result timeVariant(Path src, boolean skipFastPath) throws Exception {
        List<Long> timings = new ArrayList<>();
        for (int i = 0; i < RUNS; i++) {
            timings.add(runOnceMs(src, skipFastPath));
        }
        return new Result(timings);
    }

    private static Result[] timeInterleaved(Path src, String order) throws Exception {
        List<Long> aTimes = new ArrayList<>();
        List<Long> bTimes = new ArrayList<>();
        boolean firstA = !"BA".equalsIgnoreCase(order);
        for (int i = 0; i < RUNS; i++) {
            if (firstA) {
                aTimes.add(runOnceMs(src, /*skipFastPath=*/false));
                bTimes.add(runOnceMs(src, /*skipFastPath=*/true));
            } else {
                bTimes.add(runOnceMs(src, /*skipFastPath=*/true));
                aTimes.add(runOnceMs(src, /*skipFastPath=*/false));
            }
        }
        return new Result[] { new Result(aTimes), new Result(bTimes) };
    }

    private static long runOnceMs(Path src, boolean skipFastPath) throws Exception {
        long start = System.nanoTime();
        int exit = runJavac(src, skipFastPath);
        long end = System.nanoTime();
        if (exit != 0) {
            throw new RuntimeException("javac failed with exit code " + exit);
        }
        return (end - start) / 1_000_000L;
    }

    private static Result[] timeSeparate(Path src, int warmup) throws Exception {
        // Variant A (fast-path enabled)
        for (int i = 0; i < warmup; i++) {
            runOnceMs(src, /*skipFastPath=*/false);
        }
        List<Long> aTimes = new ArrayList<>();
        for (int i = 0; i < RUNS; i++) {
            aTimes.add(runOnceMs(src, /*skipFastPath=*/false));
        }

        // Variant B (fast-path disabled)
        for (int i = 0; i < warmup; i++) {
            runOnceMs(src, /*skipFastPath=*/true);
        }
        List<Long> bTimes = new ArrayList<>();
        for (int i = 0; i < RUNS; i++) {
            bTimes.add(runOnceMs(src, /*skipFastPath=*/true));
        }
        return new Result[] { new Result(aTimes), new Result(bTimes) };
    }

    private static void printResults(String label, Result a, Result b) {
        // Print raw timings
        System.out.println("Variant A (fast-path enabled) timings ms: " + a.timingsMs);
        System.out.println("Variant B (fast-path disabled) timings ms: " + b.timingsMs);

        // Summary table and deltas
        System.out.println();
        System.out.println("Results (ms) - " + label + ":");
        System.out.println("Variant  |  median  |  average");
        System.out.println(String.format("A        |  %7.2f |  %7.2f", a.median(), a.average()));
        System.out.println(String.format("B        |  %7.2f |  %7.2f", b.median(), b.average()));
        double medA = a.median(), medB = b.median();
        double avgA = a.average(), avgB = b.average();
        System.out.println();
        System.out.printf("Median delta (B vs A): %.3f%%%n", (medB - medA) / medA * 100.0);
        System.out.printf("Average delta (B vs A): %.3f%%%n", (avgB - avgA) / avgA * 100.0);
    }

    private static int runJavac(Path src, boolean skipFastPath) throws Exception {
        String checkerJar = locateCheckerJar();

        List<String> cmd = new ArrayList<>();
        cmd.add(findJavacExecutable());
        // Add required --add-opens for running Checker Framework on JDK 9+
        String[] javacPkgs = new String[] {
            "com.sun.tools.javac.api",
            "com.sun.tools.javac.code",
            "com.sun.tools.javac.comp",
            "com.sun.tools.javac.file",
            "com.sun.tools.javac.main",
            "com.sun.tools.javac.parser",
            "com.sun.tools.javac.processing",
            "com.sun.tools.javac.tree",
            "com.sun.tools.javac.util"
        };
        for (String p : javacPkgs) {
            cmd.add("-J--add-opens=jdk.compiler/" + p + "=ALL-UNNAMED");
        }
        if (skipFastPath) {
            cmd.add("-J-Dcf.skipNonnullFastPath=true");
        }
        cmd.addAll(Arrays.asList(
                "-classpath", checkerJar,
                "-processor", "org.checkerframework.checker.nullness.NullnessChecker",
                "-proc:only",
                "-source", "8",
                "-target", "8",
                "-Xlint:-options",
                src.getFileName().toString()));

        ProcessBuilder pb = new ProcessBuilder(cmd);
        pb.directory(src.getParent().toFile());
        pb.redirectErrorStream(true);
        Process p = pb.start();
        // Consume output to avoid buffer blockage.
        byte[] out = p.getInputStream().readAllBytes();
        int code = p.waitFor();
        // In case of failure, print compiler output for debugging.
        if (code != 0) {
            System.out.write(out);
        }
        return code;
    }

    private static String locateCheckerJar() {
        try {
            String testRoot = System.getProperty("test.root");
            if (testRoot != null) {
                Path p = Paths.get(testRoot).toAbsolutePath().normalize().getParent().resolve("dist/checker.jar");
                if (Files.exists(p)) {
                    return p.toString();
                }
            }
            Path p1 = Paths.get("checker/dist/checker.jar").toAbsolutePath().normalize();
            if (Files.exists(p1)) {
                return p1.toString();
            }
            Path p2 = Paths.get("../../../dist/checker.jar").toAbsolutePath().normalize();
            if (Files.exists(p2)) {
                return p2.toString();
            }
            // Fallback: walk up from the current working dir (jtreg scratch)
            Path cwd = Paths.get(".").toAbsolutePath().normalize();
            for (int i = 0; i < 8; i++) {
                Path cand = cwd;
                for (int j = 0; j < i; j++) cand = cand.getParent();
                if (cand == null) break;
                Path jar = cand.resolve("checker/dist/checker.jar");
                if (Files.exists(jar)) return jar.toString();
            }
        } catch (Throwable ignore) {
        }
        return "checker/dist/checker.jar";
    }

    private static String findJavacExecutable() {
        String javaHome = System.getProperty("java.home");
        // Typical JDK layout: $JAVA_HOME/bin/javac
        File jh = new File(javaHome);
        File bin = new File(jh.getParentFile(), "bin");
        File javac = new File(bin, isWindows() ? "javac.exe" : "javac");
        if (javac.exists()) {
            return javac.getAbsolutePath();
        }
        // Fallback to PATH
        return isWindows() ? "javac.exe" : "javac";
    }

    private static boolean isWindows() {
        String os = System.getProperty("os.name", "").toLowerCase();
        return os.contains("win");
    }

    private static final class Result {
        final List<Long> timingsMs;
        Result(List<Long> t) { this.timingsMs = Collections.unmodifiableList(new ArrayList<>(t)); }
        double average() { return timingsMs.stream().mapToLong(Long::longValue).average().orElse(0); }
        double median() {
            if (timingsMs.isEmpty()) return 0;
            List<Long> copy = new ArrayList<>(timingsMs);
            Collections.sort(copy);
            int n = copy.size();
            if ((n & 1) == 1) {
                return copy.get(n/2);
            } else {
                return (copy.get(n/2 - 1) + copy.get(n/2)) / 2.0;
            }
        }
    }
}


