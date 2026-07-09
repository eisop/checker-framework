package org.checkerframework.framework.stubifier;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseResult;
import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.StubUnit;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Build-time generator that pre-parses {@code .astub} files into the binary stub format read by
 * {@code org.checkerframework.framework.stub.BinaryStubReader}. For every {@code .astub} file found
 * under the given input roots, writes a sibling-named {@code .astub.bin.gz} file under the output
 * root (preserving the relative path, so the binary is packaged as a sibling resource of the text
 * stub). At checker startup, {@code AnnotationFileElementTypes} loads the binary form of a built-in
 * stub file if present and falls back to text parsing otherwise.
 *
 * <p>A file is skipped — no binary is emitted, so the checker text-parses it — if it cannot be
 * parsed, or fails to serialize. Skipping is always safe; it only forgoes the speedup for that
 * file.
 *
 * <p>Usage: {@code BinaryStubFileGenerator <outputDir> <inputRoot>...}
 */
public class BinaryStubFileGenerator {

    /** Do not instantiate; this is a main class. */
    private BinaryStubFileGenerator() {}

    /**
     * Generates binary forms for all {@code .astub} files under the given roots.
     *
     * @param args the output directory, followed by one or more input root directories
     * @throws IOException if walking an input root fails
     */
    public static void main(String[] args) throws IOException {
        if (args.length < 2) {
            System.err.println("Usage: BinaryStubFileGenerator <outputDir> <inputRoot>...");
            System.exit(2);
        }
        Path outRoot = Paths.get(args[0]);
        int written = 0;
        int skipped = 0;
        for (int i = 1; i < args.length; i++) {
            Path inRoot = Paths.get(args[i]);
            if (!Files.isDirectory(inRoot)) {
                continue;
            }
            List<Path> astubs;
            try (Stream<Path> walk = Files.walk(inRoot)) {
                astubs =
                        walk.filter(p -> p.toString().endsWith(".astub"))
                                .sorted()
                                .collect(Collectors.toList());
            }
            for (Path astub : astubs) {
                if (generateOne(inRoot, astub, outRoot)) {
                    written++;
                } else {
                    skipped++;
                }
            }
        }
        System.out.printf(
                "BinaryStubFileGenerator: wrote %d binary stub files, skipped %d.%n",
                written, skipped);
    }

    /**
     * Generates the binary form of one {@code .astub} file, or skips it if it contains constructs
     * the binary format does not model.
     *
     * @param inRoot the input root, used to compute the file's relative path
     * @param astub the stub file to process
     * @param outRoot the output root the binary is written under
     * @return true if a binary was written, false if the file was skipped
     */
    private static boolean generateOne(Path inRoot, Path astub, Path outRoot) {
        Path out = outRoot.resolve(inRoot.relativize(astub) + BinaryStubWriter.BIN_SUFFIX);
        try (InputStream in = Files.newInputStream(astub)) {
            // Mirror JavaParserUtil.parseStubUnit: a stub file may contain several `package`
            // sections, which the stub parser represents as several compilation units.
            ParserConfiguration configuration = new ParserConfiguration();
            // Same language level as JavaParserUtil.DEFAULT_LANGUAGE_LEVEL, which the text
            // parser uses (JavaParserUtil is not on the stubifier classpath).
            configuration.setLanguageLevel(ParserConfiguration.LanguageLevel.JAVA_21);
            configuration.setStoreTokens(false);
            configuration.setLexicalPreservationEnabled(false);
            configuration.setAttributeComments(false);
            configuration.setDetectOriginalLineSeparator(false);
            configuration.setPreprocessUnicodeEscapes(true);
            ParseResult<StubUnit> parseResult = new JavaParser(configuration).parseStubUnit(in);
            if (!parseResult.isSuccessful() || !parseResult.getResult().isPresent()) {
                System.err.println(
                        "BinaryStubFileGenerator: cannot parse "
                                + astub
                                + " (falls back to text parsing): "
                                + parseResult.getProblems());
                return false;
            }
            List<CompilationUnit> cus = parseResult.getResult().get().getCompilationUnits();
            BinaryStubWriter writer = new BinaryStubWriter();
            writer.processStubUnit(cus);
            Files.createDirectories(out.getParent());
            writer.writeTo(out.toFile());
            return true;
        } catch (Exception e) {
            System.err.println(
                    "BinaryStubFileGenerator: skipping "
                            + astub
                            + " (falls back to text parsing): "
                            + e);
            try {
                Files.deleteIfExists(out);
            } catch (IOException cleanupFailure) {
                // A stale binary must not ship; make the build fail loudly.
                throw new RuntimeException(
                        "Could not delete stale binary stub " + out, cleanupFailure);
            }
            return false;
        }
    }
}
