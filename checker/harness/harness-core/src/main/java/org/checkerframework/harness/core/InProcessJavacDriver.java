package org.checkerframework.harness.core;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.stream.Collectors;

import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.JavaCompiler.CompilationTask;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;

/**
 * In-process driver that invokes {@code javac} via {@link javax.tools.ToolProvider}.
 *
 * <p>Flow: 1) Generate sources using the provided {@link CodeGenerator}. 2) Build the {@code javac}
 * argument list from {@link Driver.CompilerConfig} and {@code javacFlags}. 3) Compile with a {@link
 * javax.tools.DiagnosticCollector}; measure wall time; collect diagnostics. 4) Deterministically
 * sort diagnostics (file → line → column → kind → message). 5) Assemble a {@link Driver.RunResult}
 * (including {@code success}), persist {@code result.json} next to the generated sources, and
 * return.
 *
 * <p>Failure policy: generation/IO errors are thrown; plain compilation failures are surfaced via
 * diagnostics while still returning a {@link Driver.RunResult} so A/B reports can be produced.
 */
public final class InProcessJavacDriver implements Driver {

    /**
     * Executes one generate+compile run and returns timing, diagnostics, and metadata.
     *
     * @param spec run specification: generator request, compiler config, extra javac flags, and
     *     label
     * @return a {@link Driver.RunResult} containing wall-clock time, success, diagnostics, and
     *     metadata
     * @throws Exception if source generation or setup fails; plain compilation failures are
     *     reported via diagnostics
     */
    @Override
    public RunResult runOnce(RunSpec spec) throws Exception {
        Objects.requireNonNull(spec);
        CodeGenerator.GenerationResult genRes = spec.generator().generate(spec.genReq());

        // Diff vs ExternalProcessJavacDriver: this driver invokes javac in-process via ToolProvider
        // (lower overhead, direct DiagnosticCollector). The external driver launches a separate
        // javac process (better environment isolation, success determined by exit code).
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();

        if (compiler == null) {
            throw new IllegalStateException("No system Java compiler. Are you running on a JRE?");
        }

        DiagnosticCollector<JavaFileObject> diags = new DiagnosticCollector<>();
        List<String> args = new ArrayList<>();

        // Source/target/release
        if (spec.compiler().sourceOpts() != null) {
            args.addAll(spec.compiler().sourceOpts());
        }

        // Classpath: if empty, fall back to processorPath so processors' dependent annotations are
        // visible
        boolean hasCp =
                spec.compiler().classpath() != null && !spec.compiler().classpath().isEmpty();
        if (hasCp) {
            args.add("-classpath");
            args.add(
                    spec.compiler().classpath().stream()
                            .map(Path::toString)
                            .collect(Collectors.joining(java.io.File.pathSeparator)));
        } else if (spec.compiler().processorPath() != null
                && !spec.compiler().processorPath().isEmpty()) {
            args.add("-classpath");
            args.add(
                    spec.compiler().processorPath().stream()
                            .map(Path::toString)
                            .collect(Collectors.joining(java.io.File.pathSeparator)));
        }

        // Processor path
        if (spec.compiler().processorPath() != null && !spec.compiler().processorPath().isEmpty()) {
            args.add("-processorpath");
            args.add(
                    spec.compiler().processorPath().stream()
                            .map(Path::toString)
                            .collect(Collectors.joining(java.io.File.pathSeparator)));
        }

        // Processors
        if (spec.compiler().processors() != null && !spec.compiler().processors().isEmpty()) {
            args.add("-processor");
            args.add(String.join(",", spec.compiler().processors()));
        }

        // Annotation processor and javac flags.
        // For in-process runs, translate -Dk=v into temporary System properties (javac doesn't
        // accept -D).
        java.util.Map<String, String> prevSysProps = new java.util.HashMap<String, String>();
        if (spec.javacFlags() != null) {
            for (String f : spec.javacFlags()) {
                if (f == null) continue;
                // Accept both -Dk=v and -J-Dk=v (the latter is common with external javac)
                if (f.startsWith("-J") && f.length() > 2) {
                    f = f.substring(2);
                }
                if (f.startsWith("-D")) {
                    int eq = f.indexOf('=');
                    if (eq > 2) {
                        String key = f.substring(2, eq);
                        String val = f.substring(eq + 1);
                        String old = System.getProperty(key);
                        prevSysProps.put(key, old == null ? null : old);
                        System.setProperty(key, val);
                        continue; // do not pass -D to javac
                    }
                }
                args.add(f);
            }
        }

        // Output dir
        if (spec.compiler().outDir() != null) {
            Files.createDirectories(spec.compiler().outDir());
            args.addAll(Arrays.asList("-d", spec.compiler().outDir().toString()));
        }

        // Compile sources
        StandardJavaFileManager fm =
                compiler.getStandardFileManager(diags, Locale.ROOT, StandardCharsets.UTF_8);
        Iterable<? extends JavaFileObject> units =
                fm.getJavaFileObjectsFromPaths(genRes.sourceFiles());
        List<String> finalArgs = Collections.unmodifiableList(args);

        long start = System.nanoTime();
        CompilationTask task =
                compiler.getTask(
                        new PrintWriter(new StringWriter()), fm, diags, finalArgs, null, units);
        boolean ok = Boolean.TRUE.equals(task.call());
        long end = System.nanoTime();

        // Restore previous system properties to avoid cross-run leakage
        for (java.util.Map.Entry<String, String> e : prevSysProps.entrySet()) {
            if (e.getValue() == null) {
                System.clearProperty(e.getKey());
            } else {
                System.setProperty(e.getKey(), e.getValue());
            }
        }

        // Convert compiler diagnostics to stable, comparable records
        List<Driver.DiagnosticEntry> diagEntries = new ArrayList<>();
        for (Diagnostic<? extends JavaFileObject> d : diags.getDiagnostics()) {
            JavaFileObject src = d.getSource();
            String file = "";
            if (src != null) {
                try {
                    java.nio.file.Path p = Paths.get(src.toUri());
                    java.nio.file.Path base = genRes.sourcesDir();
                    if (base != null) {
                        try {
                            file = base.relativize(p).toString();
                        } catch (Throwable ignore) {
                            file = p.getFileName().toString();
                        }
                    } else {
                        file = p.getFileName().toString();
                    }
                } catch (Throwable ignore) {
                    file = "";
                }
            }
            int line = (int) d.getLineNumber();
            int col = (int) d.getColumnNumber();
            String kind = d.getKind().name();
            String msg = d.getMessage(Locale.ROOT);
            diagEntries.add(new Driver.DiagnosticEntry(file, line, col, kind, msg));
        }
        // Sort ensures deterministic ordering across runs for set-equality comparisons
        diagEntries.sort(
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
        // diagnostics collected and deterministically sorted

        // If compilation failed, keep going; diagnostics will reflect issues. Caller can inspect
        // results.

        // Minimal metadata captured for reporting
        java.util.Map<String, Object> meta = new java.util.HashMap<String, Object>();
        meta.put("timestamp", Instant.now().toString());
        meta.put("generator", spec.generator().name());
        meta.put("label", spec.label());
        meta.put("flags", String.join(" ", finalArgs));

        RunResult result =
                new RunResult(
                        spec.label(),
                        (end - start) / 1_000_000L,
                        ok,
                        Collections.unmodifiableList(diagEntries),
                        java.util.Collections.<String, Object>emptyMap(),
                        genRes.sourcesDir(),
                        meta);

        // Persist a JSON snapshot next to the generated sources (read by the CLI reporter)
        Path out = genRes.sourcesDir().resolve("result.json");
        HarnessIO.writeJson(out, result);

        return result;
    }
}
