package org.checkerframework.framework.stubifier;

import com.github.javaparser.ParseResult;
import com.github.javaparser.ParserConfiguration.LanguageLevel;
import com.github.javaparser.ast.AccessSpecifier;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.ConstructorDeclaration;
import com.github.javaparser.ast.body.EnumDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.InitializerDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.ast.expr.NormalAnnotationExpr;
import com.github.javaparser.ast.nodeTypes.modifiers.NodeWithAccessModifiers;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.visitor.ModifierVisitor;
import com.github.javaparser.utils.CollectionStrategy;
import com.github.javaparser.utils.ParserCollectionStrategy;
import com.github.javaparser.utils.ProjectRoot;
import com.github.javaparser.utils.SourceRoot;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

/**
 * Process Java source files in a directory to produce, in-place, minimal stub files, and also
 * generate the compressed binary stub file ({@link BinaryStubWriter#OUTPUT_FILENAME}) for the same
 * directory from the same parsed compilation units.
 *
 * <p>To process a file means to remove:
 *
 * <ol>
 *   <li>everything that is private or package-private,
 *   <li>all comments, except for an initial copyright header,
 *   <li>all method bodies,
 *   <li>all field initializers,
 *   <li>all initializer blocks,
 *   <li>attributes to the {@code Deprecated} annotation (to be Java 8 compatible).
 * </ol>
 *
 * <p>Usage: {@code JavaStubifier [--skipUnloadableAnnotations] <directory> [<directory> ...]}. By
 * default, an annotation whose {@code @Target} cannot be read (because its class is missing from
 * the stubifier's own build classpath) aborts the run with a message naming the annotation and the
 * source file being processed; see {@link #SKIP_UNLOADABLE_ANNOTATIONS_FLAG} for the opt-in flag
 * that trades that safety for being able to finish the run anyway.
 */
public class JavaStubifier {
    /**
     * The language level used to parse both the annotated JDK sources (by this class) and the
     * built-in {@code .astub} files (by {@link BinaryStubFileGenerator}, which reuses this
     * constant). Intentionally duplicates {@code JavaParserUtil.DEFAULT_LANGUAGE_LEVEL}, which the
     * text parser uses at checker runtime: {@code JavaParserUtil} lives in framework main, which
     * this stubifier source set cannot depend on (the dependency runs the other way — framework
     * main depends on this source set's output — and framework.jar ships no stubifier classes), so
     * the constant can't be unified further.
     */
    public static final LanguageLevel DEFAULT_LANGUAGE_LEVEL = LanguageLevel.JAVA_21;

    /**
     * Command-line flag that makes an unloadable annotation a dropped-and-warned event rather than
     * a fatal error. When passed, {@link BinaryStubWriter} omits, from the binary stub output, any
     * annotation whose {@code @Target} it cannot read -- printing one warning line to stderr naming
     * the annotation and its source file -- and processing continues with the rest of the
     * directory. Without the flag (the default), the same condition aborts the whole run; see
     * {@link BinaryStubWriter#annotationTargets}.
     *
     * <p><b>Trade-off:</b> dropping such an annotation is not always safe. It could be a real type
     * qualifier that a checker's own (wider) classpath would have resolved, in which case dropping
     * it here silently removes it from the annotated JDK with no way for a later checker run to
     * detect the omission. Pass this flag only when the unloadable annotation is known to be
     * irrelevant to type-checking (e.g. a JUnit annotation on a stray test source file that ended
     * up in the JDK source tree being stubified), not as a default way to push through classpath
     * problems.
     */
    public static final String SKIP_UNLOADABLE_ANNOTATIONS_FLAG = "--skipUnloadableAnnotations";

    /**
     * Processes each provided command-line argument; see class documentation for details.
     *
     * @param args command-line arguments: an optional {@link #SKIP_UNLOADABLE_ANNOTATIONS_FLAG},
     *     followed by one or more directories to process
     */
    public static void main(String[] args) {
        boolean skipUnloadableAnnotations = false;
        int dirCount = 0;
        for (String arg : args) {
            if (arg.equals(SKIP_UNLOADABLE_ANNOTATIONS_FLAG)) {
                skipUnloadableAnnotations = true;
            } else {
                dirCount++;
            }
        }
        if (dirCount < 1) {
            System.err.printf(
                    "Usage: JavaStubifier [%s] <directory> [<directory> ...]%n",
                    SKIP_UNLOADABLE_ANNOTATIONS_FLAG);
            System.exit(1);
        }
        for (String arg : args) {
            if (!arg.equals(SKIP_UNLOADABLE_ANNOTATIONS_FLAG)) {
                process(arg, skipUnloadableAnnotations);
            }
        }
    }

    /**
     * Process each file in the given directory; see class documentation for details.
     *
     * @param dir directory to process
     * @param skipUnloadableAnnotations whether to drop, rather than fail on, an annotation whose
     *     {@code @Target} cannot be read; see {@link #SKIP_UNLOADABLE_ANNOTATIONS_FLAG}
     */
    private static void process(String dir, boolean skipUnloadableAnnotations) {
        // Scoped to this call, not a shared/static field: main() may process several
        // directories, and a BinaryStubWriter accumulates classes/pool state across every
        // process(CompilationUnit) call with no reset, so reusing one across directories would
        // make each directory's output file also contain every earlier directory's classes.
        //
        // omitUnannotatedMembers: this writer produces the annotated JDK, whose members the
        // reader never marks with @FromStubFile, so a member record with no annotations has no
        // effect and is not worth writing. BinaryStubFileGenerator, which produces the built-in
        // stub files' binaries, must not pass this.
        BinaryStubWriter binaryStubWriter =
                new BinaryStubWriter(/* omitUnannotatedMembers= */ true, skipUnloadableAnnotations);
        Path root = dirnameToPath(dir);
        MinimizerCallback mc = new MinimizerCallback(binaryStubWriter);
        CollectionStrategy strategy = new ParserCollectionStrategy();
        // Required to include directories that contain a module-info.java, which don't parse by
        // default.
        strategy.getParserConfiguration().setLanguageLevel(DEFAULT_LANGUAGE_LEVEL);
        ProjectRoot projectRoot = strategy.collect(root);

        projectRoot
                .getSourceRoots()
                .forEach(
                        sourceRoot -> {
                            try {
                                sourceRoot.parse("", mc);
                            } catch (IOException e) {
                                System.err.println("IOException: " + e);
                            }
                        });

        File outputFile = new File(dir, BinaryStubWriter.OUTPUT_FILENAME);
        try {
            binaryStubWriter.writeTo(outputFile);
        } catch (IOException e) {
            // Do not print and carry on. The checker prefers this file over the text stubs
            // whenever it exists, so a truncated one -- or one an earlier run left behind --
            // would ship and be applied in place of the JDK's real annotations, silently.
            // BinaryStubFileGenerator makes the same file-level failure fatal for the same
            // reason; it can afford to merely skip a stub file because that file then keeps its
            // text parsing, whereas there is no text fallback for a half-written annotated JDK.
            try {
                Files.deleteIfExists(outputFile.toPath());
            } catch (IOException cleanupFailure) {
                throw new RuntimeException(
                        "Could not delete incomplete binary stub " + outputFile, cleanupFailure);
            }
            throw new RuntimeException("Failed to write binary stub " + outputFile, e);
        }
    }

    /**
     * Converts a directory name to a path. It issues a warning and terminates the program if the
     * argument does not exist or is not a directory.
     *
     * <p>Unlike {@code Paths.get}, it handles "." which means the current directory in Unix.
     *
     * @param dir a directory name
     * @return a path for the directory name
     */
    public static Path dirnameToPath(String dir) {
        File f = new File(dir);
        if (!f.exists()) {
            System.err.printf("Directory %s (%s) does not exist.%n", dir, f);
            System.exit(1);
        }
        if (!f.isDirectory()) {
            System.err.printf("Not a directory: %s (%s).%n", dir, f);
            System.exit(1);
        }
        String absoluteDir = f.getAbsolutePath();
        if (absoluteDir.endsWith("/.")) {
            absoluteDir = absoluteDir.substring(0, absoluteDir.length() - 2);
        }
        return Paths.get(absoluteDir);
    }

    /** Callback to process each Java file; see class documentation for details. */
    private static class MinimizerCallback implements SourceRoot.Callback {
        /** The visitor instance. */
        private final MinimizerVisitor mv;

        /** The writer used to generate the compressed binary stub file for this directory. */
        private final BinaryStubWriter binaryStubWriter;

        /**
         * Create a MinimizerCallback instance.
         *
         * @param binaryStubWriter the writer to accumulate this directory's classes into
         */
        public MinimizerCallback(BinaryStubWriter binaryStubWriter) {
            this.mv = new MinimizerVisitor();
            this.binaryStubWriter = binaryStubWriter;
        }

        @Override
        public Result process(
                Path localPath, Path absolutePath, ParseResult<CompilationUnit> result) {
            Result res = Result.SAVE;
            // System.out.printf("Minimizing %s%n", absolutePath);
            Optional<CompilationUnit> opt = result.getResult();
            if (opt.isPresent()) {
                CompilationUnit cu = opt.get();
                // Only remove the "contained" comments so that the copyright comment is not
                // removed.
                cu.getAllContainedComments().forEach(Node::remove);
                mv.visit(cu, null);
                // ClassOrInterfaceDeclaration, AnnotationDeclaration, EnumDeclaration, and
                // RecordDeclaration all extend TypeDeclaration, so one findAll covers all four
                // kinds with no predicate filter needed. package-info.java and module-info.java
                // never have any TypeDeclaration but still carry declaration annotations
                // (BinaryStubWriter.processTypes reads cu.getPackageDeclaration()/cu.getModule()),
                // so both are kept even when otherwise "empty".
                if (cu.findAll(TypeDeclaration.class).isEmpty()
                        && !absolutePath.endsWith("package-info.java")
                        && !absolutePath.endsWith("module-info.java")) {
                    // All content is removed, delete this file.
                    new File(absolutePath.toUri()).delete();
                    res = Result.DONT_SAVE;
                } else {
                    try {
                        binaryStubWriter.process(cu);
                    } catch (RuntimeException e) {
                        // BinaryStubWriter's own failure messages (e.g. "cannot load annotation
                        // ...") do not know which file was being processed -- it operates on a
                        // CompilationUnit, and process(CompilationUnit) is also called directly by
                        // tests with no file behind it at all. This layer is the first one that
                        // does know the file for certain (it is a callback parameter), so it is
                        // the simplest place to add that context to every failure that can escape
                        // process(cu), not just the annotation-loading one.
                        throw new RuntimeException(
                                "Failed to process " + absolutePath + ": " + e.getMessage(), e);
                    }
                }
            }
            return res;
        }
    }

    /** Visitor to process one compilation unit; see class documentation for details. */
    private static class MinimizerVisitor extends ModifierVisitor<Void> {
        /** Whether to consider members implicitly public. */
        private boolean implicitlyPublic = false;

        @Override
        public ClassOrInterfaceDeclaration visit(ClassOrInterfaceDeclaration cid, Void arg) {
            boolean prevIP = implicitlyPublic;
            if (cid.isInterface()) {
                // All members of interfaces are implicitly public.
                implicitlyPublic = true;
            }
            super.visit(cid, arg);
            if (cid.isInterface()) {
                implicitlyPublic = prevIP;
            }
            // Do not remove private or package-private classes, because there could
            // be externally-visible members in externally-visible subclasses.
            return cid;
        }

        @Override
        public EnumDeclaration visit(EnumDeclaration ed, Void arg) {
            super.visit(ed, arg);
            // Enums can't be extended, so it is ok to remove them if they are not externally
            // visible.
            removeIfPrivateOrPkgPrivate(ed);
            return ed;
        }

        @Override
        public ConstructorDeclaration visit(ConstructorDeclaration cd, Void arg) {
            super.visit(cd, arg);
            // Constructors cannot be overridden, so it is ok to remove them if they are
            // not externally visible.
            if (!removeIfPrivateOrPkgPrivate(cd)) {
                // ConstructorDeclaration has to have a body
                cd.setBody(new BlockStmt());
            }
            return cd;
        }

        @Override
        public MethodDeclaration visit(MethodDeclaration md, Void arg) {
            super.visit(md, arg);
            // Non-private methods could be overridden with larger visibility.
            // So it is only safe to remove private methods, which can't be overridden.
            if (!removeIfPrivate(md)) {
                md.removeBody();
            }
            return md;
        }

        @Override
        public FieldDeclaration visit(FieldDeclaration fd, Void arg) {
            super.visit(fd, arg);
            // It is safe to remove fields that are not externally visible.
            if (!removeIfPrivateOrPkgPrivate(fd)) {
                fd.getVariables().forEach(v -> v.getInitializer().ifPresent(Node::remove));
            }
            return fd;
        }

        @Override
        public InitializerDeclaration visit(InitializerDeclaration id, Void arg) {
            super.visit(id, arg);
            id.remove();
            return id;
        }

        @Override
        public NormalAnnotationExpr visit(NormalAnnotationExpr nae, Void arg) {
            super.visit(nae, arg);
            if (nae.getNameAsString().equals("Deprecated")) {
                nae.setPairs(new NodeList<>());
            }
            return nae;
        }

        /**
         * Remove the whole node if it is private or package private.
         *
         * @param node a Node to inspect
         * @return true if the node was removed
         */
        private boolean removeIfPrivateOrPkgPrivate(NodeWithAccessModifiers<?> node) {
            if (implicitlyPublic) {
                return false;
            }
            AccessSpecifier as = node.getAccessSpecifier();
            if (as == AccessSpecifier.PRIVATE || as == AccessSpecifier.NONE) {
                ((Node) node).remove();
                return true;
            }
            return false;
        }

        /**
         * Remove the whole node if it is private.
         *
         * @param node a Node to inspect
         * @return true if the node was removed
         */
        private boolean removeIfPrivate(NodeWithAccessModifiers<?> node) {
            if (implicitlyPublic) {
                return false;
            }
            AccessSpecifier as = node.getAccessSpecifier();
            if (as == AccessSpecifier.PRIVATE) {
                ((Node) node).remove();
                return true;
            }
            return false;
        }
    }
}
