package org.checkerframework.harness.core;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

/**
 * Pluggable source generator API.
 *
 * <p>Implementations write compilable Java sources under {@link GenerationRequest#outputDir()} and
 * return created files plus metadata. Output should be reproducible for the same {@code
 * seed}/{@code sampleCount}/{@code extra} so A/B runs are comparable.
 */
public interface CodeGenerator {
    /**
     * Returns the generator's short, stable identifier.
     *
     * @return non-null name used in metadata and reports
     */
    String name();

    /**
     * Generates Java sources for the given request.
     *
     * @param req generation parameters (output directory, seed, sample count, extras)
     * @return generation result containing the sources directory, created files, and metadata
     * @throws Exception if generation fails (I/O or content production errors)
     */
    GenerationResult generate(GenerationRequest req) throws Exception;

    /** Input describing where to write sources and how many/what to generate. */
    final class GenerationRequest {
        private final Path outputDir;
        private final long seed;
        private final int sampleCount;
        private final Map<String, String> extra;

        /**
         * Creates a new generation request.
         *
         * @param outputDir base directory under which sources will be written
         * @param seed pseudo-random seed for deterministic content
         * @param sampleCount number of source files to emit (generator-defined semantics)
         * @param extra optional string key/value parameters for generator-specific tuning
         */
        public GenerationRequest(
                Path outputDir, long seed, int sampleCount, Map<String, String> extra) {
            this.outputDir = outputDir;
            this.seed = seed;
            this.sampleCount = sampleCount;
            this.extra = extra;
        }

        /**
         * @return output directory where sources should be written
         */
        public Path outputDir() {
            return outputDir;
        }

        /**
         * @return deterministic seed driving content variability
         */
        public long seed() {
            return seed;
        }

        /**
         * @return requested number of samples/files
         */
        public int sampleCount() {
            return sampleCount;
        }

        /**
         * @return optional generator-specific parameters; may be null
         */
        public Map<String, String> extra() {
            return extra;
        }
    }

    /** Output describing where sources were written, which files were created, and metadata. */
    final class GenerationResult {
        private final Path sourcesDir;
        private final List<Path> sourceFiles;
        private final Map<String, Object> metadata;

        /**
         * Creates a new immutable generation result.
         *
         * @param sourcesDir directory containing all generated sources
         * @param sourceFiles deterministic, sorted list of created source file paths
         * @param metadata generator-defined metadata for reporting
         */
        public GenerationResult(
                Path sourcesDir, List<Path> sourceFiles, Map<String, Object> metadata) {
            this.sourcesDir = sourcesDir;
            this.sourceFiles = sourceFiles;
            this.metadata = metadata;
        }

        /**
         * @return directory containing generated sources
         */
        public Path sourcesDir() {
            return sourcesDir;
        }

        /**
         * @return list of generated source files
         */
        public List<Path> sourceFiles() {
            return sourceFiles;
        }

        /**
         * @return metadata map for diagnostics/reporting
         */
        public Map<String, Object> metadata() {
            return metadata;
        }
    }
}
