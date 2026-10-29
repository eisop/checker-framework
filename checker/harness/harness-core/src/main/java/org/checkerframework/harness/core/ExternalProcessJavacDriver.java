package org.checkerframework.harness.core;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * External-process driver that launches {@code javac} via {@link ProcessBuilder}.
 *
 * <p>Flow: 1) Generate sources using the provided {@link CodeGenerator}. 2) Build the {@code javac}
 * command line (add-opens, classpath, processors, flags, output dir). 3) Launch a separate {@code
 * javac} process; measure wall time. 4) If exit code != 0, attach the entire stdout as a single
 * ERROR diagnostic (best-effort visibility). 5) Assemble a {@link Driver.RunResult} (with {@code
 * success} from exit code), persist {@code result.json}, return.
 *
 * <p>Failure policy: generation/IO errors are thrown; compilation failures are surfaced via
 * diagnostics while still returning a {@link Driver.RunResult} so A/B reports can be produced.
 */
public final class ExternalProcessJavacDriver implements Driver {

    /**
     * Executes a single compile by invoking an external javac process.
     *
     * @param spec run specification: generator request, compiler config, extra javac flags, and
     *     label
     * @return a {@link Driver.RunResult} with process wall time, exit-code based success, parsed
     *     diagnostics, and meta
     * @throws Exception if source generation or process setup fails (non-zero compilation exit
     *     codes do not throw)
     */
    @Override
    public RunResult runOnce(RunSpec spec) throws Exception {
        Objects.requireNonNull(spec);
        CodeGenerator.GenerationResult genRes = spec.generator().generate(spec.genReq());

        List<String> cmd = new ArrayList<String>();

        // Diff vs InProcessJavacDriver: this driver executes a separate JVM/javac process (better
        // isolation),
        // with success determined by exit code, instead of using ToolProvider. Diagnostics are
        // coarse unless parsed.
        // javac executable
        String javac = findJavacExecutable();
        cmd.add(javac);

        // Add required --add-opens similar to other engines
        String[] pkgs =
                new String[] {
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
        for (String p : pkgs) {
            cmd.add("-J--add-opens=jdk.compiler/" + p + "=ALL-UNNAMED");
        }

        // Force English diagnostics for stable parsing (parity with Locale.ROOT in in-process)
        cmd.add("-J-Duser.language=en");
        cmd.add("-J-Duser.country=US");

        if (spec.javacFlags() != null) {
            cmd.addAll(spec.javacFlags());
        }

        // Build core args from config: source/release, proc mode, lints (parity with in-process
        // engine)
        List<String> args = new ArrayList<String>();
        if (spec.compiler().sourceOpts() != null) {
            args.addAll(spec.compiler().sourceOpts());
        }

        // Processor path
        if (spec.compiler().processorPath() != null && !spec.compiler().processorPath().isEmpty()) {
            args.add("-processorpath");
            args.add(joinPathsResolved(spec.compiler().processorPath()));
        }

        // Classpath
        if (spec.compiler().classpath() != null && !spec.compiler().classpath().isEmpty()) {
            args.add("-classpath");
            args.add(joinPathsResolved(spec.compiler().classpath()));
        } else if (spec.compiler().processorPath() != null
                && !spec.compiler().processorPath().isEmpty()) {
            // Parity with InProcessJavacDriver: if no explicit classpath is given, fall back to
            // using the processorPath so that processor dependencies (e.g., annotation types in
            // checker-qual.jar) are visible to the compiler and processors at runtime.
            args.add("-classpath");
            args.add(joinPathsResolved(spec.compiler().processorPath()));
        }

        if (spec.compiler().processors() != null && !spec.compiler().processors().isEmpty()) {
            args.add("-processor");
            args.add(joinComma(spec.compiler().processors()));
        }

        if (spec.compiler().outDir() != null) {
            args.addAll(Arrays.asList("-d", spec.compiler().outDir().toString()));
        }

        // Keep a copy of option-only args for reporting
        List<String> optionArgsForMeta = new ArrayList<String>(args);

        // Append sources: pass paths relative to sourcesDir to avoid duplicate prefixes
        for (Path p : genRes.sourceFiles()) {
            String rel;
            try {
                Path base = genRes.sourcesDir();
                rel = (base != null) ? base.relativize(p).toString() : p.toString();
            } catch (Throwable ignore) {
                rel = p.getFileName().toString();
            }
            args.add(rel);
        }

        cmd.addAll(args);

        ProcessBuilder pb = new ProcessBuilder(cmd);
        if (genRes.sourcesDir() != null) {
            pb.directory(genRes.sourcesDir().toFile());
        }
        pb.redirectErrorStream(true);

        // Measure only javac execution (process run) to align with in-process timing
        long start = System.nanoTime();
        Process pr = pb.start();
        int code = pr.waitFor();
        long end = System.nanoTime();
        // Read stdout after timing window to avoid skewing wallMillis with IO/diagnostic parsing
        byte[] out = pr.getInputStream().readAllBytes();
        String stdout = new String(out, StandardCharsets.UTF_8);

        List<Driver.DiagnosticEntry> diags = parseJavacDiagnostics(stdout);
        if (diags.isEmpty() && code != 0) {
            // Fallback: attach whole stdout as a single ERROR diagnostic for visibility
            diags.add(new Driver.DiagnosticEntry("<process>", 0, 0, "ERROR", stdout));
        }

        // Normalize file path to relative path against sourcesDir (match in-process behavior)
        java.util.List<Driver.DiagnosticEntry> norm =
                new java.util.ArrayList<Driver.DiagnosticEntry>(diags.size());
        for (Driver.DiagnosticEntry d : diags) {
            String f = d.file();
            String rel = f;
            try {
                if (f != null && !f.isEmpty() && !f.startsWith("<")) {
                    java.nio.file.Path p = Paths.get(f);
                    java.nio.file.Path base = genRes.sourcesDir();
                    if (base != null) {
                        try {
                            rel = base.relativize(p).toString();
                        } catch (Throwable ignore) {
                            rel = p.getFileName().toString();
                        }
                    } else {
                        rel = p.getFileName().toString();
                    }
                }
            } catch (Throwable ignore) {
            }
            norm.add(
                    new Driver.DiagnosticEntry(
                            rel == null ? "" : rel, d.line(), d.column(), d.kind(), d.message()));
        }
        // Deterministic sort: file → line → column → kind → message (same as in-process)
        norm.sort(
                (a, b) -> {
                    int c;
                    c = a.file().compareTo(b.file());
                    if (c != 0) return c;
                    c = Integer.compare(a.line(), b.line());
                    if (c != 0) return c;
                    c = Integer.compare(a.column(), b.column());
                    if (c != 0) return c;
                    c = a.kind().compareTo(b.kind());
                    if (c != 0) return c;
                    return a.message().compareTo(b.message());
                });

        Map<String, Object> meta = new java.util.HashMap<String, Object>();
        meta.put("timestamp", Instant.now().toString());
        meta.put("generator", spec.generator().name());
        meta.put("label", spec.label());
        List<String> reportArgs = new ArrayList<String>(optionArgsForMeta);
        int di = reportArgs.indexOf("-d");
        if (di >= 0) {
            reportArgs.remove(di);
            if (di < reportArgs.size()) reportArgs.remove(di);
        }
        meta.put("flags", String.join(" ", reportArgs));

        RunResult result =
                new RunResult(
                        spec.label(),
                        (end - start) / 1_000_000L,
                        code == 0,
                        java.util.Collections.unmodifiableList(norm),
                        java.util.Collections.<String, Object>emptyMap(),
                        genRes.sourcesDir(),
                        meta);

        Path outFile = genRes.sourcesDir().resolve("result.json");
        HarnessIO.writeJson(outFile, result);
        return result;
    }

    /**
     * Best-effort parse of javac stdout into structured diagnostics. Supports formats with and
     * without column numbers: file:line:column: kind: message file:line: kind: message Lines not
     * matching a new record are appended to the previous message.
     */
    private static List<Driver.DiagnosticEntry> parseJavacDiagnostics(String stdout) {
        List<Driver.DiagnosticEntry> out = new ArrayList<Driver.DiagnosticEntry>();
        if (stdout == null || stdout.isEmpty()) return out;

        Pattern pWithCol =
                Pattern.compile("^(.+?):(\\d+):(\\d+):\\s*(error|warning|note):\\s*(.*)$");
        Pattern pNoCol = Pattern.compile("^(.+?):(\\d+):\\s*(error|warning|note):\\s*(.*)$");

        String[] lines = stdout.split("\\r?\\n");
        String curFile = null, curKind = null;
        int curLine = 0, curCol = 0;
        StringBuilder curMsg = null;

        for (String line : lines) {
            if (line == null) line = "";
            Matcher m = pWithCol.matcher(line);
            Matcher m2 = pNoCol.matcher(line);
            boolean matched = false;
            if (m.matches()) {
                // flush previous
                if (curFile != null) {
                    out.add(
                            new Driver.DiagnosticEntry(
                                    curFile,
                                    curLine,
                                    curCol,
                                    curKind,
                                    curMsg == null ? "" : curMsg.toString().trim()));
                }
                curFile = m.group(1);
                curLine = safeInt(m.group(2));
                curCol = safeInt(m.group(3));
                curKind = toUpperKind(m.group(4));
                curMsg = new StringBuilder(m.group(5) == null ? "" : m.group(5));
                matched = true;
            } else if (m2.matches()) {
                if (curFile != null) {
                    out.add(
                            new Driver.DiagnosticEntry(
                                    curFile,
                                    curLine,
                                    curCol,
                                    curKind,
                                    curMsg == null ? "" : curMsg.toString().trim()));
                }
                curFile = m2.group(1);
                curLine = safeInt(m2.group(2));
                curCol = 0;
                curKind = toUpperKind(m2.group(3));
                curMsg = new StringBuilder(m2.group(4) == null ? "" : m2.group(4));
                matched = true;
            }

            if (!matched) {
                // continuation for previous diagnostic
                if (curFile != null) {
                    if (curMsg.length() > 0) curMsg.append(' ');
                    curMsg.append(line.trim());
                }
            }
        }

        if (curFile != null) {
            out.add(
                    new Driver.DiagnosticEntry(
                            curFile,
                            curLine,
                            curCol,
                            curKind,
                            curMsg == null ? "" : curMsg.toString().trim()));
        }

        // Stable sort is done by the report layer; maintain insertion order here
        return out;
    }

    private static int safeInt(String s) {
        try {
            return Integer.parseInt(s);
        } catch (Throwable t) {
            return 0;
        }
    }

    private static String toUpperKind(String k) {
        if (k == null) return "";
        String kk = k.trim().toUpperCase(Locale.ROOT);
        // Normalize common terms
        if ("ERROR".equals(kk) || "WARNING".equals(kk) || "NOTE".equals(kk)) return kk;
        return kk;
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
     * directory. This ensures paths work correctly even when the javac process changes its working
     * directory.
     */
    private static String joinPathsResolved(List<Path> paths) {
        String sep = File.pathSeparator;
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < paths.size(); i++) {
            if (i > 0) sb.append(sep);
            Path p = paths.get(i);
            // Convert relative paths to absolute paths based on current working directory
            // This prevents issues when ProcessBuilder changes the working directory
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

    private static String joinComma(List<String> items) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < items.size(); i++) {
            if (i > 0) sb.append(',');
            sb.append(items.get(i));
        }
        return sb.toString();
    }

    private static String findJavacExecutable() {
        String javaHome = System.getProperty("java.home");
        File jh = new File(javaHome);
        File bin = new File(jh.getParentFile(), "bin");
        File javac = new File(bin, isWindows() ? "javac.exe" : "javac");
        if (javac.exists()) return javac.getAbsolutePath();
        return isWindows() ? "javac.exe" : "javac";
    }

    private static boolean isWindows() {
        String os = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
        return os.contains("win");
    }
}
