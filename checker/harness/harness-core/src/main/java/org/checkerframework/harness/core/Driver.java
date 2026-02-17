package org.checkerframework.harness.core;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

/**
 * Driver API: executes a single harness run (generate sources, then invoke {@code javac} with the
 * requested annotation processors) and returns timing, diagnostics, and metadata.
 *
 * <p>Designed for A/B comparisons: implementations should produce stable, deterministically-sorted
 * diagnostics so results are comparable across runs.
 */
public interface Driver {
    /**
     * Executes one run per the given specification: calls the {@link CodeGenerator} to write
     * sources to disk, then compiles them with the configured processors and flags.
     *
     * <p>Implementations may report compilation failures via diagnostics rather than throwing; only
     * unrecoverable generation/IO errors should surface as exceptions.
     *
     * @param spec the run description (generator, generation request, compiler config, flags,
     *     label)
     * @return the outcome of the run (elapsed time, diagnostics, metadata, work directory)
     * @throws Exception on unrecoverable errors during generation or setup
     */
    RunResult runOnce(RunSpec spec) throws Exception;

    /** Input describing how to perform a single run. */
    final class RunSpec {
        private final CodeGenerator generator;
        private final CodeGenerator.GenerationRequest genReq;
        private final CompilerConfig compiler;
        private final List<String> javacFlags;
        private final String label;

        /**
         * Creates a new run specification.
         *
         * @param generator code generator implementation
         * @param genReq generation request (output dir/seed/samples/extras)
         * @param compiler compiler configuration (classpath, processors, release, out dir)
         * @param javacFlags extra javac/processor flags
         * @param label human-readable label such as "baseline" or "update"
         */
        public RunSpec(
                CodeGenerator generator,
                CodeGenerator.GenerationRequest genReq,
                CompilerConfig compiler,
                List<String> javacFlags,
                String label) {
            this.generator = generator;
            this.genReq = genReq;
            this.compiler = compiler;
            this.javacFlags = javacFlags;
            this.label = label;
        }

        public CodeGenerator generator() {
            return generator;
        }

        public CodeGenerator.GenerationRequest genReq() {
            return genReq;
        }

        public CompilerConfig compiler() {
            return compiler;
        }

        public List<String> javacFlags() {
            return javacFlags;
        }

        public String label() {
            return label;
        }
    }

    /** Outcome of a run. */
    final class RunResult {
        private final String label;
        private final long wallMillis;
        private final boolean success;
        private final List<DiagnosticEntry> diagnostics;
        private final Map<String, Object> metrics;
        private final Path workDir;
        private final Map<String, Object> meta;

        /**
         * Creates a new run result.
         *
         * @param label run label (baseline/update)
         * @param wallMillis wall-clock duration in milliseconds
         * @param success true if compilation succeeded
         * @param diagnostics deterministically-sorted diagnostics
         * @param metrics optional metrics map (reserved for future use)
         * @param workDir working directory containing generated sources
         * @param meta metadata such as timestamp, generator, flags
         */
        public RunResult(
                String label,
                long wallMillis,
                boolean success,
                List<DiagnosticEntry> diagnostics,
                Map<String, Object> metrics,
                Path workDir,
                Map<String, Object> meta) {
            this.label = label;
            this.wallMillis = wallMillis;
            this.success = success;
            this.diagnostics = diagnostics;
            this.metrics = metrics;
            this.workDir = workDir;
            this.meta = meta;
        }

        public String label() {
            return label;
        }

        public long wallMillis() {
            return wallMillis;
        }

        public boolean success() {
            return success;
        }

        public List<DiagnosticEntry> diagnostics() {
            return diagnostics;
        }

        public Map<String, Object> metrics() {
            return metrics;
        }

        public Path workDir() {
            return workDir;
        }

        public Map<String, Object> meta() {
            return meta;
        }
    }

    /** Stable diagnostic record suitable for equality and sorting. */
    final class DiagnosticEntry {
        private final String file;
        private final int line;
        private final int column;
        private final String kind;
        private final String message;

        /**
         * Creates a diagnostic entry.
         *
         * @param file source path (relative when possible)
         * @param line 1-based line number, 0 if unknown
         * @param column 1-based column number, 0 if unknown
         * @param kind diagnostic kind (ERROR/WARNING/NOTE)
         * @param message diagnostic message text
         */
        public DiagnosticEntry(String file, int line, int column, String kind, String message) {
            this.file = file;
            this.line = line;
            this.column = column;
            this.kind = kind;
            this.message = message;
        }

        public String file() {
            return file;
        }

        public int line() {
            return line;
        }

        public int column() {
            return column;
        }

        public String kind() {
            return kind;
        }

        public String message() {
            return message;
        }
    }

    /** Compiler configuration for invoking javac and its processors. */
    final class CompilerConfig {
        private final Path javacExecutable;
        private final List<Path> classpath;
        private final List<String> processors;
        private final List<Path> processorPath;
        private final List<String> sourceOpts;
        private final Path outDir;

        /**
         * Creates a new compiler configuration.
         *
         * @param javacExecutable explicit path to javac executable (nullable for system default)
         * @param classpath regular compilation classpath
         * @param processors FQNs of annotation processors
         * @param processorPath locations where processors are loaded from
         * @param sourceOpts language/compilation options (e.g., --release, -proc:only)
         * @param outDir output directory passed as -d
         */
        public CompilerConfig(
                Path javacExecutable,
                List<Path> classpath,
                List<String> processors,
                List<Path> processorPath,
                List<String> sourceOpts,
                Path outDir) {
            this.javacExecutable = javacExecutable;
            this.classpath = classpath;
            this.processors = processors;
            this.processorPath = processorPath;
            this.sourceOpts = sourceOpts;
            this.outDir = outDir;
        }

        public Path javacExecutable() {
            return javacExecutable;
        }

        public List<Path> classpath() {
            return classpath;
        }

        public List<String> processors() {
            return processors;
        }

        public List<Path> processorPath() {
            return processorPath;
        }

        public List<String> sourceOpts() {
            return sourceOpts;
        }

        public Path outDir() {
            return outDir;
        }
    }
}
