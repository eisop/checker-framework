package org.checkerframework.framework.stubifier;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.ImportDeclaration;
import com.github.javaparser.ast.body.AnnotationDeclaration;
import com.github.javaparser.ast.body.AnnotationMemberDeclaration;
import com.github.javaparser.ast.body.BodyDeclaration;
import com.github.javaparser.ast.body.CallableDeclaration;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.ConstructorDeclaration;
import com.github.javaparser.ast.body.EnumConstantDeclaration;
import com.github.javaparser.ast.body.EnumDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.body.ReceiverParameter;
import com.github.javaparser.ast.body.RecordDeclaration;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.AnnotationExpr;
import com.github.javaparser.ast.expr.ArrayInitializerExpr;
import com.github.javaparser.ast.expr.BinaryExpr;
import com.github.javaparser.ast.expr.BooleanLiteralExpr;
import com.github.javaparser.ast.expr.CharLiteralExpr;
import com.github.javaparser.ast.expr.ClassExpr;
import com.github.javaparser.ast.expr.DoubleLiteralExpr;
import com.github.javaparser.ast.expr.EnclosedExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.FieldAccessExpr;
import com.github.javaparser.ast.expr.IntegerLiteralExpr;
import com.github.javaparser.ast.expr.LongLiteralExpr;
import com.github.javaparser.ast.expr.MarkerAnnotationExpr;
import com.github.javaparser.ast.expr.MemberValuePair;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.NormalAnnotationExpr;
import com.github.javaparser.ast.expr.SingleMemberAnnotationExpr;
import com.github.javaparser.ast.expr.StringLiteralExpr;
import com.github.javaparser.ast.expr.UnaryExpr;
import com.github.javaparser.ast.type.ArrayType;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.ast.type.PrimitiveType;
import com.github.javaparser.ast.type.Type;
import com.github.javaparser.ast.type.TypeParameter;
import com.github.javaparser.ast.type.WildcardType;
import com.github.javaparser.ast.visitor.ModifierVisitor;
import com.github.javaparser.ast.visitor.SimpleVoidVisitor;
import com.github.javaparser.ast.visitor.Visitable;

import org.checkerframework.checker.nullness.qual.Nullable;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.annotation.ElementType;
import java.lang.annotation.Target;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.GZIPOutputStream;

/**
 * Writes parsed stub compilation units into the compressed binary stub format read by {@code
 * org.checkerframework.framework.stub.BinaryStubReader}. The output is a GZIP-compressed file (see
 * {@link #writeTo}), conventionally named with the {@link #BIN_SUFFIX} suffix ({@code .bin.gz},
 * e.g. {@link #OUTPUT_FILENAME}). Used at build time for the annotated JDK (via {@link
 * JavaStubifier}) and for the built-in checker stub files (via {@link BinaryStubFileGenerator}).
 *
 * <p>This extracts annotations structurally from class, interface, enum, and annotation-type
 * declarations and their members — including type-parameter bound annotations, enum constants, and
 * annotation-type members — and writes them into a dense binary format optimized for rapid loading
 * without parsing overhead at checker startup. Record declarations are not supported; files
 * containing them keep text parsing (see {@link BinaryStubFileGenerator}).
 *
 * <p>Name resolution and member filtering deliberately mirror the text parser ({@code
 * AnnotationFileParser}): private declarations are skipped, annotation names resolve through
 * explicit imports, {@code java.lang}, and asterisk imports, and {@code @CFComment} is dropped.
 * Equivalence is enforced by {@code BinaryStubDiffChecker}; run {@code NullnessBinaryStubDiffTest}
 * after changing this class.
 */
public class BinaryStubWriter {
    /**
     * Magic number identifying the Checker Framework binary stub format ({@code CF} + {@code JDK}:
     * 0xCF non-ASCII marker byte, {@code 'J'} 0x4A, {@code 'D'} 0x44, {@code 'K'} 0x4B). This is
     * the canonical definition; {@code org.checkerframework.framework.stub.BinaryStubData#MAGIC}
     * references it.
     */
    public static final int MAGIC = 0xCF4A444B;

    /**
     * Format version of the binary stub file. This is the canonical definition; {@code
     * org.checkerframework.framework.stub.BinaryStubData#VERSION} references it. Increment whenever
     * the binary format changes.
     */
    public static final short VERSION = 1;

    /**
     * File name of the binary stub output file. This is the canonical definition; {@code
     * org.checkerframework.framework.stub.BinaryStubData#FILENAME} references it.
     */
    public static final String OUTPUT_FILENAME = "annotated-jdk.bin.gz";

    /**
     * File-name suffix appended to a source stub file's name to name its binary form (e.g. {@code
     * jdk.astub} → {@code jdk.astub.bin.gz}). This is the canonical definition; {@code
     * org.checkerframework.framework.stub.BinaryStubData#BIN_SUFFIX} references it.
     */
    public static final String BIN_SUFFIX = ".bin.gz";

    /**
     * Fully-qualified name of {@code CFComment}, which is never written to the binary format; see
     * {@link AnnotationPool#addAnnotation}.
     */
    private static final String CF_COMMENT = "org.checkerframework.framework.qual.CFComment";

    /**
     * Sentinel returned by {@link AnnotationPool#addAnnotation} for annotations that are not
     * written to the binary format ({@code @CFComment}). Callers must skip it.
     */
    private static final int IGNORED = -1;

    /** Constant pool for strings to minimize binary size. */
    private static class ConstantPool {
        /** Map from string content to its constant-pool index. */
        private final Map<String, Integer> stringToIndex = new LinkedHashMap<>();

        /**
         * Constructs an empty constant pool, with index 0 unconditionally reserved for the empty
         * string. Several {@code ClassRecord} fields (e.g. {@code outerNameIndex}) use 0 as a
         * sentinel meaning "no such string" without going through {@link #addString}; without this
         * reservation, whichever real string is added first would silently take index 0 instead,
         * making it indistinguishable from the sentinel (e.g. a nested class whose outer class
         * happens to be the very first class processed in this writer's lifetime would have its --
         * correct -- outerNameIndex of 0 misread by the reader as "top-level").
         */
        ConstantPool() {
            addString("");
        }

        /**
         * Adds a string to the constant pool and returns its index.
         *
         * @param s the string to add (may be {@code null}, which is treated as empty)
         * @return the index of the string in the constant pool
         */
        public int addString(String s) {
            if (s == null) s = "";
            return stringToIndex.computeIfAbsent(s, k -> stringToIndex.size());
        }

        /**
         * Writes the size and all strings in the pool to the output stream.
         *
         * @param out the data output stream to write to
         * @throws IOException if writing fails
         */
        public void write(DataOutputStream out) throws IOException {
            out.writeInt(stringToIndex.size());
            for (String s : stringToIndex.keySet()) {
                out.writeUTF(s);
            }
        }
    }

    /** Unique structural annotation pool to avoid duplicate records. */
    private static class AnnotationPool {
        private final List<byte[]> serializedAnnos = new ArrayList<>();
        private final Map<String, Integer> annoToIdx = new LinkedHashMap<>();

        /**
         * Adds a structural annotation to the pool and returns its index.
         *
         * @param anno the annotation expression
         * @param cu the compilation unit context
         * @param writer the writer holding the string pool and qualification logic
         * @return the index in the annotation pool
         * @throws IOException if serialization fails
         */
        public int addAnnotation(AnnotationExpr anno, CompilationUnit cu, BinaryStubWriter writer)
                throws IOException {
            // qualifyAnnotation deep-clones and qualifies all nested nodes, so neither
            // writeAnnotationInline nor writeValue needs to qualify again.
            AnnotationExpr qualified = writer.qualifyAnnotation(anno, cu);
            if (qualified.getNameAsString().equals(CF_COMMENT)) {
                // @CFComment is documentation for humans with no effect on checking; do not
                // waste binary-format space on it. Callers skip the IGNORED sentinel.
                return IGNORED;
            }
            String key = qualified.toString();
            Integer idx = annoToIdx.get(key);
            if (idx != null) {
                return idx;
            }

            idx = annoToIdx.size();
            annoToIdx.put(key, idx);

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            DataOutputStream dos = new DataOutputStream(baos);
            writer.writeAnnotationInline(dos, qualified, cu);
            dos.flush();
            serializedAnnos.add(baos.toByteArray());

            return idx;
        }

        /**
         * Writes the size and all serialized annotations in the pool to the output stream.
         *
         * @param out the output stream
         * @throws IOException if writing fails
         */
        public void write(DataOutputStream out) throws IOException {
            out.writeInt(serializedAnnos.size());
            for (byte[] data : serializedAnnos) {
                out.write(data);
            }
        }
    }

    /** Represents a step in a TypeAnnotation path. */
    private static class TypePathStep {
        /**
         * The kind of path step: {@code 0} = array component, {@code 1} = nested type, {@code 2} =
         * wildcard bound, {@code 3} = type argument. Only 4 values are ever assigned (all well
         * within a signed byte's positive range), so this stays a plain {@code byte}, matching the
         * reader's {@code BinaryStubData.TypePathStep#kind} field.
         */
        final byte kind;

        /**
         * For {@link #kind} {@code 3} (TYPE_ARGUMENT), the type argument index. For {@link #kind}
         * {@code 2} (WILDCARD_BOUND), repurposed to distinguish an extends bound ({@code 0}) from a
         * super bound ({@code 1}): JVMS itself leaves this byte unused for WILDCARD_BOUND
         * (assuming, at the bytecode level, that a wildcard has only one structurally possible
         * bound), but CF's {@code AnnotatedWildcardType} always synthesizes both bounds, so {@code
         * BinaryStubReader.resolvePath} needs this to know which one a given path step is for.
         * Unused (0) for every other kind. Kept as {@code byte} (matching JVMS's 1-byte {@code
         * type_argument_index}) rather than widened to {@code int}, to avoid quadrupling the size
         * of every {@link TypePathStep} instance (one per path step of every type annotation
         * processed) for a value that is realistically always 0 or 1; see {@code
         * BinaryStubData.TypePathStep#argIndex} for how a value of 128 or greater is reinterpreted
         * as unsigned on the reading side.
         */
        final byte argIndex;

        /**
         * Constructs a TypePathStep.
         *
         * @param kind the kind of path step
         * @param argIndex the type argument index
         */
        TypePathStep(byte kind, byte argIndex) {
            this.kind = kind;
            this.argIndex = argIndex;
        }
    }

    /** Represents a TypeAnnotation with its path and annotation pool index. */
    private static class TypeAnno {
        /** Index into the annotation pool. */
        int annoIndex;

        /** The path to the annotated type component. */
        List<TypePathStep> path;

        /**
         * Constructs a TypeAnno with an empty type path (applies to the base type).
         *
         * @param annoIndex index of the annotation record in the annotation pool
         */
        TypeAnno(int annoIndex) {
            this.annoIndex = annoIndex;
            this.path = Collections.emptyList();
        }

        /**
         * Constructs a TypeAnno.
         *
         * @param annoIndex index of the annotation record in the annotation pool
         * @param path the type path; copied defensively since the caller mutates it during
         *     traversal
         */
        TypeAnno(int annoIndex, List<TypePathStep> path) {
            this.annoIndex = annoIndex;
            this.path = new ArrayList<>(path);
        }

        /**
         * Writes this type annotation to the output stream.
         *
         * @param out the data output stream to write to
         * @throws IOException if writing fails
         */
        void write(DataOutputStream out) throws IOException {
            out.writeInt(annoIndex);
            out.writeByte(path.size());
            for (TypePathStep step : path) {
                out.writeByte(step.kind);
                // JVMS leaves argIndex (type_argument_index) unused (0) for every kind except
                // TYPE_ARGUMENT (3), but WILDCARD_BOUND (2) repurposes it here to distinguish an
                // extends bound (0) from a super bound (1) -- see TypePathStep's field javadoc.
                if (step.kind == 3 || step.kind == 2) {
                    out.writeByte(step.argIndex);
                }
            }
        }
    }

    /** Represents the annotations for a single method or constructor. */
    private static class MethodRecord {
        /** Index of the method signature in the constant pool. */
        int sigIndex;

        /** Annotation-pool indices of the declaration annotations on this method. */
        List<Integer> declAnnos = new ArrayList<>();

        /** Type annotations on the return type. */
        List<TypeAnno> returnTypeAnnos = new ArrayList<>();

        /** Type annotations on the receiver type. */
        List<TypeAnno> receiverAnnos = new ArrayList<>();

        /** List of type annotations for each parameter. */
        List<List<TypeAnno>> paramAnnos = new ArrayList<>();

        /** List of declaration annotation pool indices for each parameter. */
        List<List<Integer>> paramDeclAnnos = new ArrayList<>();

        /** Per-type-parameter annotation records for this method. */
        List<TypeParamRecord> typeParams = new ArrayList<>();
    }

    /** Represents the annotations for a single field. */
    private static class FieldRecord {
        /** Index of the field name in the constant pool. */
        int nameIndex;

        /** Annotation-pool indices of the declaration annotations on this field. */
        List<Integer> declAnnos = new ArrayList<>();

        /** Type annotations on the field's type. */
        List<TypeAnno> typeAnnos = new ArrayList<>();
    }

    /** Represents the annotation data for a single record component. */
    private static class ComponentRecord {
        /** Index of the component name in the constant pool. */
        int nameIndex;

        /** Annotation-pool indices of the declaration annotations on this component. */
        List<Integer> declAnnos = new ArrayList<>();

        /** Type annotations on the component's type. */
        List<TypeAnno> typeAnnos = new ArrayList<>();

        /**
         * True if the record body contains an explicit zero-argument accessor method with the same
         * name as this component.
         */
        boolean hasAccessor;
    }

    /** Represents the annotations and members of a single class. */
    private static class ClassRecord {
        /**
         * {@link #kind} value for a class or interface declaration: both {@code ElementKind.CLASS}
         * and {@code ElementKind.INTERFACE} map to this constant.
         */
        static final byte KIND_CLASS_OR_INTERFACE = 0;

        /** {@link #kind} value for an enum declaration. */
        static final byte KIND_ENUM = 1;

        /** {@link #kind} value for an annotation-type declaration. */
        static final byte KIND_ANNOTATION_TYPE = 2;

        /** {@link #kind} value for a record declaration. */
        static final byte KIND_RECORD = 3;

        /** Index of the fully-qualified class name in the constant pool. */
        int nameIndex;

        /**
         * Index of the outermost enclosing class name in the constant pool, or 0 if this is a
         * top-level class (i.e., {@code nameIndex} itself is the outermost).
         */
        int outerNameIndex;

        /**
         * One of the {@code KIND_*} constants, recording what kind of declaration this class record
         * came from. Read back by the reader and compared against the real {@code TypeElement}'s
         * kind, mirroring {@code AnnotationFileParser.processTypeDecl}'s own defensive check for
         * exactly this mismatch (e.g. a stub still declaring {@code java.nio.ByteOrder} as a class
         * after it became a real enum in JDK 26): if they disagree, the class record must not be
         * applied, since the annotated JDK is meant to be usable across JDK versions whose API can
         * drift out from under a fixed stub source.
         */
        byte kind;

        /** Annotation-pool indices of the declaration annotations on this class. */
        List<Integer> declAnnos = new ArrayList<>();

        /** Records for all annotated fields of this class. */
        List<FieldRecord> fields = new ArrayList<>();

        /** Records for all annotated methods of this class. */
        List<MethodRecord> methods = new ArrayList<>();

        /** Per-type-parameter annotation records for this class. */
        List<TypeParamRecord> typeParams = new ArrayList<>();

        /**
         * Per-component records for a record declaration ({@code kind == KIND_RECORD}). Empty for
         * non-record classes.
         */
        List<ComponentRecord> components = new ArrayList<>();

        /**
         * Constructs a ClassRecord.
         *
         * @param nameIndex index of the class name in the constant pool
         * @param outerNameIndex index of the outermost enclosing class name, or 0 if top-level
         * @param kind one of the {@code KIND_*} constants
         */
        ClassRecord(int nameIndex, int outerNameIndex, byte kind) {
            this.nameIndex = nameIndex;
            this.outerNameIndex = outerNameIndex;
            this.kind = kind;
        }
    }

    /** Annotation data for a single type parameter in the writer. */
    private static class TypeParamRecord {
        /** Annotation-pool indices of annotations on the type variable itself. */
        List<Integer> typeVarAnnos = new ArrayList<>();

        /** Per-bound type annotations. Element {@code i} holds annotations for the i-th bound. */
        List<List<TypeAnno>> boundAnnos = new ArrayList<>();
    }

    /** The constant pool used to share strings. */
    private final ConstantPool pool = new ConstantPool();

    /** The structural annotation pool. */
    private final AnnotationPool annosPool = new AnnotationPool();

    /** Records for all classes processed. */
    private final List<ClassRecord> classes = new ArrayList<>();

    /** Map of package name to annotation-pool indices of its declaration annotations. */
    private final Map<String, List<Integer>> packages = new LinkedHashMap<>();

    /** Map of module name to annotation-pool indices of its declaration annotations. */
    private final Map<String, List<Integer>> modules = new LinkedHashMap<>();

    /** Map from simple class names to their fully-qualified names. */
    private final Map<String, String> simpleToFqn = new HashMap<>();

    /**
     * Package names imported via asterisk imports ({@code import java.beans.*;}) in the compilation
     * unit currently being processed.
     */
    private final List<String> asteriskImportPackages = new ArrayList<>();

    /** Cache for {@link #annotationInPackage}, keyed by {@code pkg + "." + name}. */
    private final Map<String, String> annotationInPackageCache = new HashMap<>();

    /**
     * Fully qualifies an annotation name, mirroring how the text parser resolves annotation names
     * at read time ({@code AnnotationFileParser.getImportedAnnotations} and {@code getAnnotation}):
     * a dotted name is used as written; a simple name is resolved against the {@code java.lang}
     * package (the text parser unconditionally adds all {@code java.lang} annotations, so e.g. an
     * unimported {@code @Override} resolves — and {@code java.lang} wins name conflicts because it
     * is added last there), then against the file's explicit imports, then against its asterisk
     * imports. A name that resolves through none of these is returned unchanged; the reader will
     * then fail to resolve it and drop the annotation — exactly as the text parser drops
     * annotations it cannot resolve.
     *
     * @param name the annotation name as written in the source
     * @return the fully-qualified annotation name, or {@code name} if it cannot be resolved
     */
    private String fullyQualifyAnnotationName(String name) {
        if (name.contains(".")) {
            return name;
        }
        String javaLang = annotationInPackage("java.lang", name);
        if (javaLang != null) {
            return javaLang;
        }
        String known = simpleToFqn.get(name);
        if (known != null) {
            return known;
        }
        for (String pkg : asteriskImportPackages) {
            String candidate = annotationInPackage(pkg, name);
            if (candidate != null) {
                return candidate;
            }
        }
        return name;
    }

    /**
     * Returns the fully-qualified name of the annotation type with the given simple name in the
     * given package, or {@code null} if the package contains no annotation type of that name (on
     * the stubifier classpath).
     *
     * @param pkg the package name
     * @param name the simple name
     * @return the fully-qualified name, or {@code null}
     */
    private String annotationInPackage(String pkg, String name) {
        String candidate = pkg + "." + name;
        // Cache by candidate, including negative (not-found) results: fullyQualifyAnnotationName
        // calls this once per simple annotation name per candidate package for every annotation
        // use in the whole source tree, and most candidates -- e.g. "java.lang." + some
        // non-java.lang simple name -- do not exist, so an uncached Class.forName would repeatedly
        // pay for throwing and discarding a ClassNotFoundException for the exact same string.
        String result =
                annotationInPackageCache.computeIfAbsent(
                        candidate,
                        c -> {
                            try {
                                Class<?> cls = Class.forName(c);
                                return cls.isAnnotation() ? c : NOT_FOUND;
                            } catch (ClassNotFoundException e) {
                                return NOT_FOUND;
                            }
                        });
        return result == NOT_FOUND ? null : result;
    }

    /** Sentinel for {@link #annotationInPackageCache}: no annotation class by that name exists. */
    private static final String NOT_FOUND = "";

    /**
     * Sentinel for {@link #annotationTargetsCache}: the annotation class could not be loaded or has
     * no {@code @Target} meta-annotation.
     */
    private static final ElementType[] UNKNOWN_TARGETS = new ElementType[0];

    /**
     * Cache from fully-qualified annotation class name to the element types in its {@code @Target}
     * meta-annotation, or {@link #UNKNOWN_TARGETS} if the class cannot be loaded or has no
     * {@code @Target}. Avoids a reflective {@code Class.forName} lookup per annotation occurrence
     * (the same few annotation classes occur tens of thousands of times across the JDK stubs).
     */
    private final Map<String, ElementType[]> annotationTargetsCache = new HashMap<>();

    /**
     * Returns the element types in the {@code @Target} meta-annotation of the given annotation
     * class, or {@link #UNKNOWN_TARGETS} if the class cannot be loaded (e.g. a JDK-internal
     * annotation not on the stubifier classpath) or has no {@code @Target}.
     *
     * @param fqn the fully-qualified name of the annotation class
     * @return the {@code @Target} element types, or {@link #UNKNOWN_TARGETS}
     */
    private ElementType[] annotationTargets(String fqn) {
        return annotationTargetsCache.computeIfAbsent(
                fqn,
                name -> {
                    try {
                        Target target = Class.forName(name).getAnnotation(Target.class);
                        return target == null ? UNKNOWN_TARGETS : target.value();
                    } catch (ClassNotFoundException e) {
                        return UNKNOWN_TARGETS;
                    }
                });
    }

    /**
     * Processes a single compilation unit, extracting annotations for its classes, methods, and
     * fields.
     *
     * @param cu the compilation unit to process
     */
    public void process(CompilationUnit cu) {
        initImportTables(cu);
        processTypes(cu);
    }

    /**
     * Processes the compilation units of one stub file. A stub file may contain multiple {@code
     * package} sections, which the stub parser represents as multiple compilation units; the text
     * parser resolves names against the imports of the <em>first</em> unit only (see {@code
     * AnnotationFileParser.getImportedAnnotations}), and this method does the same.
     *
     * @param cus the compilation units of the stub file, in order
     */
    public void processStubUnit(List<CompilationUnit> cus) {
        if (cus.isEmpty()) {
            return;
        }
        initImportTables(cus.get(0));
        for (CompilationUnit cu : cus) {
            processTypes(cu);
        }
    }

    /**
     * Initializes the per-file name-resolution tables ({@link #simpleToFqn}, {@link
     * #asteriskImportPackages}) from the imports of the given compilation unit.
     *
     * @param cu the compilation unit whose imports to use
     */
    private void initImportTables(CompilationUnit cu) {
        simpleToFqn.clear();
        asteriskImportPackages.clear();

        for (ImportDeclaration imp : cu.getImports()) {
            if (imp.isStatic()) {
                continue;
            }
            if (imp.isAsterisk()) {
                asteriskImportPackages.add(imp.getNameAsString());
            } else {
                String fqn = imp.getNameAsString();
                String simple = fqn.substring(fqn.lastIndexOf('.') + 1);
                simpleToFqn.put(simple, fqn);
            }
        }
    }

    /**
     * Processes the package, module, and type declarations of one compilation unit, using the
     * name-resolution tables established by {@link #initImportTables}.
     *
     * @param cu the compilation unit to process
     */
    private void processTypes(CompilationUnit cu) {

        cu.getPackageDeclaration()
                .ifPresent(
                        pkg -> {
                            try {
                                String pkgName = pkg.getNameAsString();
                                pool.addString(pkgName);
                                List<Integer> annos = new ArrayList<>();
                                for (AnnotationExpr anno : pkg.getAnnotations()) {
                                    int idx = annosPool.addAnnotation(anno, cu, this);
                                    if (idx != IGNORED) {
                                        annos.add(idx);
                                    }
                                }
                                if (!annos.isEmpty()) {
                                    packages.put(pkgName, annos);
                                }
                            } catch (IOException e) {
                                throw new RuntimeException(
                                        "Serialization failure in package: " + e.getMessage(), e);
                            }
                        });

        cu.getModule()
                .ifPresent(
                        mod -> {
                            try {
                                String modName = mod.getNameAsString();
                                pool.addString(modName);
                                List<Integer> annos = new ArrayList<>();
                                for (AnnotationExpr anno : mod.getAnnotations()) {
                                    int idx = annosPool.addAnnotation(anno, cu, this);
                                    if (idx != IGNORED) {
                                        annos.add(idx);
                                    }
                                }
                                if (!annos.isEmpty()) {
                                    modules.put(modName, annos);
                                }
                            } catch (IOException e) {
                                throw new RuntimeException(
                                        "Serialization failure in module: " + e.getMessage(), e);
                            }
                        });

        String pkg =
                cu.getPackageDeclaration().isPresent()
                        ? cu.getPackageDeclaration().get().getNameAsString()
                        : "";
        for (TypeDeclaration<?> typeDecl : cu.getTypes()) {
            processTypeDeclaration(typeDecl, pkg, "", cu);
        }
    }

    /**
     * Dispatches a type declaration to {@link #processClass}, {@link #processEnum}, {@link
     * #processAnnotationType}, or {@link #processRecord}. Other kinds of type declarations are
     * silently skipped.
     *
     * @param typeDecl the type declaration to process
     * @param enclosingFqn the fully-qualified name of the enclosing class, or the package name for
     *     top-level declarations
     * @param outermostFqn the fully-qualified name of the outermost enclosing class, or empty for
     *     top-level declarations
     * @param cu the compilation unit
     */
    private void processTypeDeclaration(
            BodyDeclaration<?> typeDecl,
            String enclosingFqn,
            String outermostFqn,
            CompilationUnit cu) {
        if (typeDecl instanceof ClassOrInterfaceDeclaration) {
            processClass((ClassOrInterfaceDeclaration) typeDecl, enclosingFqn, outermostFqn, cu);
        } else if (typeDecl instanceof EnumDeclaration) {
            processEnum((EnumDeclaration) typeDecl, enclosingFqn, outermostFqn, cu);
        } else if (typeDecl instanceof AnnotationDeclaration) {
            processAnnotationType((AnnotationDeclaration) typeDecl, enclosingFqn, outermostFqn, cu);
        } else if (typeDecl instanceof RecordDeclaration) {
            processRecord((RecordDeclaration) typeDecl, enclosingFqn, outermostFqn, cu);
        }
    }

    /** Helper to fully qualify class and annotation names inside an AnnotationExpr. */
    private AnnotationExpr qualifyAnnotation(AnnotationExpr anno, CompilationUnit cu) {
        AnnotationExpr copy = anno.clone();
        copy.accept(
                new ModifierVisitor<Void>() {
                    @Override
                    public Visitable visit(ClassExpr n, Void arg) {
                        n.setType(fullyQualify(n.getType(), cu));
                        return super.visit(n, arg);
                    }

                    @Override
                    public Visitable visit(MarkerAnnotationExpr n, Void arg) {
                        n.setName(fullyQualifyAnnotationName(n.getNameAsString()));
                        return super.visit(n, arg);
                    }

                    @Override
                    public Visitable visit(NormalAnnotationExpr n, Void arg) {
                        n.setName(fullyQualifyAnnotationName(n.getNameAsString()));
                        return super.visit(n, arg);
                    }

                    @Override
                    public Visitable visit(SingleMemberAnnotationExpr n, Void arg) {
                        n.setName(fullyQualifyAnnotationName(n.getNameAsString()));
                        return super.visit(n, arg);
                    }

                    @Override
                    public Visitable visit(FieldAccessExpr n, Void arg) {
                        if (n.getScope() instanceof NameExpr) {
                            NameExpr scope = (NameExpr) n.getScope();
                            scope.setName(fullyQualify(scope.getNameAsString(), cu));
                        }
                        return super.visit(n, arg);
                    }
                },
                null);
        return copy;
    }

    /** Serializes a single annotation value expression structurally to the stream. */
    private void writeValue(DataOutputStream out, Expression expr, CompilationUnit cu)
            throws IOException {
        if (expr instanceof EnclosedExpr) {
            // Redundant parentheses are legal Java in an annotation value (e.g.
            // "@SuppressWarnings((\"unchecked\"))"); unwrap them rather than falling through to
            // the "unsupported" case below.
            writeValue(out, ((EnclosedExpr) expr).getInner(), cu);
        } else if (expr instanceof BooleanLiteralExpr) {
            out.writeByte('Z');
            out.writeBoolean(((BooleanLiteralExpr) expr).getValue());
        } else if (expr instanceof CharLiteralExpr) {
            out.writeByte('C');
            out.writeChar(((CharLiteralExpr) expr).asChar());
        } else if (expr instanceof IntegerLiteralExpr) {
            out.writeByte('J');
            out.writeLong(((IntegerLiteralExpr) expr).asNumber().longValue());
        } else if (expr instanceof LongLiteralExpr) {
            out.writeByte('J');
            out.writeLong(((LongLiteralExpr) expr).asNumber().longValue());
        } else if (expr instanceof DoubleLiteralExpr) {
            out.writeByte('D');
            out.writeDouble(((DoubleLiteralExpr) expr).asDouble());
        } else if (expr instanceof UnaryExpr) {
            UnaryExpr ue = (UnaryExpr) expr;
            if (ue.getOperator() == UnaryExpr.Operator.MINUS) {
                Expression inner = ue.getExpression();
                if (inner instanceof IntegerLiteralExpr) {
                    out.writeByte('J');
                    out.writeLong(-((IntegerLiteralExpr) inner).asNumber().longValue());
                } else if (inner instanceof LongLiteralExpr) {
                    out.writeByte('J');
                    out.writeLong(-((LongLiteralExpr) inner).asNumber().longValue());
                } else if (inner instanceof DoubleLiteralExpr) {
                    out.writeByte('D');
                    out.writeDouble(-((DoubleLiteralExpr) inner).asDouble());
                } else {
                    throw new IOException("Unsupported unary operator expression: " + expr);
                }
            } else {
                throw new IOException("Unsupported unary operator: " + ue.getOperator());
            }
        } else if (expr instanceof StringLiteralExpr) {
            out.writeByte('s');
            out.writeInt(pool.addString(((StringLiteralExpr) expr).getValue()));
        } else if (expr instanceof ClassExpr) {
            out.writeByte('c');
            Type type = ((ClassExpr) expr).getType();
            out.writeInt(pool.addString(fullyQualify(type, cu).toString()));
        } else if (expr instanceof FieldAccessExpr) {
            out.writeByte('e');
            FieldAccessExpr fae = (FieldAccessExpr) expr;
            out.writeInt(pool.addString(fullyQualify(fae.getScope().toString(), cu)));
            out.writeInt(pool.addString(fae.getNameAsString()));
        } else if (expr instanceof AnnotationExpr) {
            out.writeByte('@');
            writeAnnotationInline(out, (AnnotationExpr) expr, cu);
        } else if (expr instanceof ArrayInitializerExpr) {
            out.writeByte('[');
            List<Expression> vals = ((ArrayInitializerExpr) expr).getValues();
            out.writeShort(vals.size());
            for (Expression val : vals) {
                writeValue(out, val, cu);
            }
        } else if (expr instanceof NameExpr) {
            out.writeByte('n');
            out.writeInt(pool.addString(((NameExpr) expr).getNameAsString()));
        } else if (expr instanceof BinaryExpr) {
            String val = evaluateStringLiteralConcatenation(expr);
            out.writeByte('s');
            out.writeInt(pool.addString(val));
        } else {
            throw new IOException(
                    "Unsupported annotation value class: "
                            + expr.getClass().getName()
                            + " for expression: `"
                            + expr
                            + "`");
        }
    }

    /**
     * Recursively evaluates a binary expression that represents string literal concatenation.
     *
     * @param expr the expression representing the string literal concatenation
     * @return the fully concatenated string value
     * @throws IOException if the expression contains non-literal values that cannot be evaluated
     */
    private String evaluateStringLiteralConcatenation(Expression expr) throws IOException {
        if (expr instanceof StringLiteralExpr) {
            return ((StringLiteralExpr) expr).getValue();
        } else if (expr instanceof BinaryExpr) {
            BinaryExpr be = (BinaryExpr) expr;
            if (be.getOperator() == BinaryExpr.Operator.PLUS) {
                return evaluateStringLiteralConcatenation(be.getLeft())
                        + evaluateStringLiteralConcatenation(be.getRight());
            }
        }
        throw new IOException("Cannot evaluate string concatenation for expression: " + expr);
    }

    /**
     * Writes an AnnotationExpr inline (also used for nested annotation values).
     *
     * @param out the data output stream to write to
     * @param anno the annotation to write; must already be fully qualified (see {@link
     *     #qualifyAnnotation}, which qualifies nested annotation values as well)
     * @param cu the compilation unit, used to resolve names in annotation values
     * @throws IOException if writing fails
     */
    private void writeAnnotationInline(
            DataOutputStream out, AnnotationExpr anno, CompilationUnit cu) throws IOException {
        out.writeInt(pool.addString(anno.getNameAsString()));
        if (anno instanceof MarkerAnnotationExpr) {
            out.writeShort(0);
        } else if (anno instanceof SingleMemberAnnotationExpr) {
            out.writeShort(1);
            out.writeInt(pool.addString("value"));
            writeValue(out, ((SingleMemberAnnotationExpr) anno).getMemberValue(), cu);
        } else if (anno instanceof NormalAnnotationExpr) {
            List<MemberValuePair> pairs = ((NormalAnnotationExpr) anno).getPairs();
            out.writeShort(pairs.size());
            for (MemberValuePair pair : pairs) {
                out.writeInt(pool.addString(pair.getNameAsString()));
                writeValue(out, pair.getValue(), cu);
            }
        }
    }

    /**
     * Fully qualifies a JavaParser type by resolving it against the compilation unit's imports.
     *
     * @param type the type to fully qualify
     * @param cu the compilation unit, used to resolve imports
     * @return the fully-qualified type
     */
    private Type fullyQualify(Type type, CompilationUnit cu) {
        if (type instanceof ClassOrInterfaceType) {
            ClassOrInterfaceType cit = (ClassOrInterfaceType) type;
            String name = cit.getNameAsString();
            if (!cit.getScope().isPresent()) {
                String fq = fullyQualify(name, cu);
                if (!fq.equals(name)) {
                    return StaticJavaParser.parseType(fq);
                }
            }
        } else if (type instanceof ArrayType) {
            ArrayType at = (ArrayType) type;
            at.setComponentType(fullyQualify(at.getComponentType(), cu));
        }
        return type;
    }

    /**
     * Fully qualifies a simple name by resolving it against the compilation unit's imports or
     * standard java.lang classes.
     *
     * @param name the simple name to fully qualify
     * @param cu the compilation unit, used to resolve imports
     * @return the fully-qualified name, or the original name if resolution fails
     */
    private String fullyQualify(String name, CompilationUnit cu) {
        if (name.contains(".")) {
            return name;
        }
        String known = simpleToFqn.get(name);
        if (known != null) {
            return known;
        }
        for (ImportDeclaration imp : cu.getImports()) {
            if (!imp.isAsterisk()) {
                String impName = imp.getNameAsString();
                if (impName.endsWith("." + name)) {
                    return impName;
                }
            }
        }
        if (name.equals("String")
                || name.equals("Object")
                || name.equals("Class")
                || name.equals("Enum")
                || name.equals("Math")
                || name.equals("System")
                || name.equals("Thread")
                || name.equals("Exception")
                || name.equals("RuntimeException")
                || name.equals("Throwable")
                || name.equals("Error")) {
            return "java.lang." + name;
        }
        try {
            Class.forName("java.lang." + name);
            return "java.lang." + name;
        } catch (ClassNotFoundException e) {
            // ignore
        }
        for (String pkg : asteriskImportPackages) {
            String candidate = pkg + "." + name;
            try {
                Class.forName(candidate);
                return candidate;
            } catch (ClassNotFoundException e) {
                // Try the next asterisk import.
            }
        }
        return name;
    }

    /**
     * Shared prologue of {@link #processClass}, {@link #processEnum}, and {@link
     * #processAnnotationType}: computes the fully-qualified name, creates the {@link ClassRecord}
     * with the declaration's annotations, and registers it — or returns {@code null} for a private
     * declaration, which the text parser skips (see {@code AnnotationFileParser.skipNode}).
     *
     * @param simpleName the declaration's simple name
     * @param isPrivate whether the declaration is private
     * @param annotations the annotations on the declaration
     * @param enclosingFqn the fully-qualified name of the enclosing class, or the package name for
     *     top-level declarations
     * @param outermostFqn the fully-qualified name of the outermost enclosing class, or empty for
     *     top-level declarations
     * @param kind one of the {@link ClassRecord}{@code .KIND_*} constants, identifying which of the
     *     three callers this is
     * @param cu the compilation unit
     * @return the registered class record, or {@code null} if the declaration is skipped
     */
    private @Nullable ClassRecord startClassRecord(
            String simpleName,
            boolean isPrivate,
            List<AnnotationExpr> annotations,
            String enclosingFqn,
            String outermostFqn,
            byte kind,
            CompilationUnit cu) {
        if (isPrivate) {
            return null;
        }
        String fqn = enclosingFqn.isEmpty() ? simpleName : enclosingFqn + "." + simpleName;
        int outerNameIndex = outermostFqn.isEmpty() ? 0 : pool.addString(outermostFqn);
        ClassRecord cr = new ClassRecord(pool.addString(fqn), outerNameIndex, kind);
        classes.add(cr);
        try {
            for (AnnotationExpr anno : annotations) {
                int idx = annosPool.addAnnotation(anno, cu, this);
                if (idx != IGNORED) {
                    cr.declAnnos.add(idx);
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(
                    "Serialization failure in annotations of " + fqn + ": " + e.getMessage(), e);
        }
        return cr;
    }

    /**
     * Processes a class or interface declaration, extracting its annotations and members.
     *
     * @param typeDecl the class or interface declaration to process
     * @param enclosingFqn the fully-qualified name of the enclosing class, or the package name for
     *     top-level classes
     * @param outermostFqn the fully-qualified name of the outermost enclosing class, or empty for
     *     top-level classes
     * @param cu the compilation unit
     */
    private void processClass(
            ClassOrInterfaceDeclaration typeDecl,
            String enclosingFqn,
            String outermostFqn,
            CompilationUnit cu) {
        ClassRecord cr =
                startClassRecord(
                        typeDecl.getNameAsString(),
                        typeDecl.isPrivate(),
                        typeDecl.getAnnotations(),
                        enclosingFqn,
                        outermostFqn,
                        ClassRecord.KIND_CLASS_OR_INTERFACE,
                        cu);
        if (cr == null) {
            return;
        }
        try {
            cr.typeParams.addAll(extractTypeParams(typeDecl.getTypeParameters(), cu));
        } catch (IOException e) {
            throw new RuntimeException(
                    "Serialization failure in class type parameters: " + e.getMessage(), e);
        }

        String fqn =
                enclosingFqn.isEmpty()
                        ? typeDecl.getNameAsString()
                        : enclosingFqn + "." + typeDecl.getNameAsString();
        String myOutermost = outermostFqn.isEmpty() ? fqn : outermostFqn;
        processMembers(typeDecl.getMembers(), cr, fqn, myOutermost, cu);
    }

    /**
     * Processes an enum declaration, extracting its annotations, enum constants, and members. The
     * resulting {@link ClassRecord} is indistinguishable from a class record; the reader resolves
     * enum constants through the same field lookup as ordinary fields.
     *
     * @param enumDecl the enum declaration to process
     * @param enclosingFqn the fully-qualified name of the enclosing class, or the package name for
     *     top-level enums
     * @param outermostFqn the fully-qualified name of the outermost enclosing class, or empty for
     *     top-level enums
     * @param cu the compilation unit
     */
    private void processEnum(
            EnumDeclaration enumDecl,
            String enclosingFqn,
            String outermostFqn,
            CompilationUnit cu) {
        ClassRecord cr =
                startClassRecord(
                        enumDecl.getNameAsString(),
                        enumDecl.isPrivate(),
                        enumDecl.getAnnotations(),
                        enclosingFqn,
                        outermostFqn,
                        ClassRecord.KIND_ENUM,
                        cu);
        if (cr == null) {
            return;
        }
        try {
            // Enum constants are modeled as field records; the reader's field lookup
            // (ElementFilter.fieldsIn) includes enum constants. A record is emitted even for
            // unannotated constants: for built-in stub files, the reader marks every matched
            // member with @FromStubFile, exactly as the text parser marks every enum constant it
            // processes.
            for (EnumConstantDeclaration constant : enumDecl.getEntries()) {
                FieldRecord fr = new FieldRecord();
                fr.nameIndex = pool.addString(constant.getNameAsString());
                for (AnnotationExpr anno : constant.getAnnotations()) {
                    int idx = annosPool.addAnnotation(anno, cu, this);
                    if (idx == IGNORED) {
                        continue;
                    }
                    if (hasTypeUse(anno, cu)) {
                        fr.typeAnnos.add(new TypeAnno(idx));
                    }
                    if (!isTypeUseOnly(anno, cu)) {
                        fr.declAnnos.add(idx);
                    }
                }
                cr.fields.add(fr);
            }
        } catch (IOException e) {
            throw new RuntimeException(
                    "Serialization failure in enum constants: " + e.getMessage(), e);
        }

        String fqn =
                enclosingFqn.isEmpty()
                        ? enumDecl.getNameAsString()
                        : enclosingFqn + "." + enumDecl.getNameAsString();
        String myOutermost = outermostFqn.isEmpty() ? fqn : outermostFqn;
        processMembers(enumDecl.getMembers(), cr, fqn, myOutermost, cu);
    }

    /**
     * Processes an annotation type declaration ({@code @interface}), extracting its annotations and
     * members. The resulting {@link ClassRecord} is indistinguishable from a class record;
     * annotation members are modeled as zero-parameter method records.
     *
     * @param annoDecl the annotation type declaration to process
     * @param enclosingFqn the fully-qualified name of the enclosing class, or the package name for
     *     top-level declarations
     * @param outermostFqn the fully-qualified name of the outermost enclosing class, or empty for
     *     top-level declarations
     * @param cu the compilation unit
     */
    private void processAnnotationType(
            AnnotationDeclaration annoDecl,
            String enclosingFqn,
            String outermostFqn,
            CompilationUnit cu) {
        ClassRecord cr =
                startClassRecord(
                        annoDecl.getNameAsString(),
                        annoDecl.isPrivate(),
                        annoDecl.getAnnotations(),
                        enclosingFqn,
                        outermostFqn,
                        ClassRecord.KIND_ANNOTATION_TYPE,
                        cu);
        if (cr == null) {
            return;
        }
        String fqn =
                enclosingFqn.isEmpty()
                        ? annoDecl.getNameAsString()
                        : enclosingFqn + "." + annoDecl.getNameAsString();
        String myOutermost = outermostFqn.isEmpty() ? fqn : outermostFqn;
        processMembers(annoDecl.getMembers(), cr, fqn, myOutermost, cu);
    }

    /**
     * Processes a record declaration, extracting its annotations, record components, and body
     * members. Each component is written as a {@link ComponentRecord} carrying its type
     * annotations, declaration annotations, and an {@code hasAccessor} flag that mirrors the text
     * parser's {@code hasAccessorInStubs} field on {@code RecordComponentStub}: true when the
     * record body contains an explicit zero-argument accessor method with the same name.
     *
     * @param recordDecl the record declaration to process
     * @param enclosingFqn the fully-qualified name of the enclosing class, or the package name for
     *     top-level records
     * @param outermostFqn the fully-qualified name of the outermost enclosing class, or empty for
     *     top-level records
     * @param cu the compilation unit
     */
    private void processRecord(
            RecordDeclaration recordDecl,
            String enclosingFqn,
            String outermostFqn,
            CompilationUnit cu) {
        ClassRecord cr =
                startClassRecord(
                        recordDecl.getNameAsString(),
                        recordDecl.isPrivate(),
                        recordDecl.getAnnotations(),
                        enclosingFqn,
                        outermostFqn,
                        ClassRecord.KIND_RECORD,
                        cu);
        if (cr == null) {
            return;
        }
        try {
            cr.typeParams.addAll(extractTypeParams(recordDecl.getTypeParameters(), cu));
        } catch (IOException e) {
            throw new RuntimeException(
                    "Serialization failure in record type parameters: " + e.getMessage(), e);
        }

        String fqn =
                enclosingFqn.isEmpty()
                        ? recordDecl.getNameAsString()
                        : enclosingFqn + "." + recordDecl.getNameAsString();

        // Build a set of component names that have an explicit zero-arg accessor in the body.
        // A zero-arg MethodDeclaration whose name equals a component name is considered an
        // accessor (matching AnnotationFileParser's hasAccessorInStubs logic).
        Set<String> accessorNames = new HashSet<>();
        for (BodyDeclaration<?> m : recordDecl.getMembers()) {
            if (m instanceof MethodDeclaration) {
                MethodDeclaration md = (MethodDeclaration) m;
                if (md.getParameters().isEmpty()) {
                    accessorNames.add(md.getNameAsString());
                }
            }
        }

        // Process each record component.
        for (Parameter comp : recordDecl.getParameters()) {
            try {
                ComponentRecord compRec = new ComponentRecord();
                compRec.nameIndex = pool.addString(comp.getNameAsString());

                // Declaration-position annotations: dual-route like method annotations.
                for (AnnotationExpr anno : comp.getAnnotations()) {
                    int idx = annosPool.addAnnotation(anno, cu, this);
                    if (idx == IGNORED) {
                        continue;
                    }
                    if (hasTypeUse(anno, cu)) {
                        // Annotation in declaration position on a record component annotates
                        // the element type of an array component (if any), matching
                        // AnnotationFileParser.processRecordField's annotate() call.
                        compRec.typeAnnos.add(new TypeAnno(idx, arrayElementPath(comp.getType())));
                    }
                    if (!isTypeUseOnly(anno, cu)) {
                        compRec.declAnnos.add(idx);
                    }
                }
                extractTypeAnnotations(comp.getType(), new ArrayList<>(), compRec.typeAnnos, cu);

                compRec.hasAccessor = accessorNames.contains(comp.getNameAsString());
                cr.components.add(compRec);
            } catch (IOException e) {
                throw new RuntimeException(
                        "Serialization failure in record component "
                                + comp.getNameAsString()
                                + ": "
                                + e.getMessage(),
                        e);
            }
        }

        String myOutermost = outermostFqn.isEmpty() ? fqn : outermostFqn;
        processMembers(recordDecl.getMembers(), cr, fqn, myOutermost, cu);
    }

    /**
     * Processes the member declarations of a class, interface, or enum: methods, constructors,
     * fields, and nested class/interface/enum declarations.
     *
     * @param members the member declarations
     * @param cr the record of the enclosing class to add member records to
     * @param fqn the fully-qualified name of the enclosing class
     * @param outermostFqn the fully-qualified name of the outermost enclosing class
     * @param cu the compilation unit
     */
    private void processMembers(
            List<BodyDeclaration<?>> members,
            ClassRecord cr,
            String fqn,
            String outermostFqn,
            CompilationUnit cu) {
        for (BodyDeclaration<?> m : members) {
            if (m instanceof MethodDeclaration) {
                processMethod((MethodDeclaration) m, cr, cu);
            } else if (m instanceof ConstructorDeclaration) {
                processConstructor((ConstructorDeclaration) m, cr, cu);
            } else if (m instanceof FieldDeclaration) {
                processField((FieldDeclaration) m, cr, cu);
            } else if (m instanceof AnnotationMemberDeclaration) {
                processAnnotationMember((AnnotationMemberDeclaration) m, cr, cu);
            } else {
                processTypeDeclaration(m, fqn, outermostFqn, cu);
            }
        }
    }

    /**
     * Processes an annotation type member ({@code String value() default "";}), modeling it as a
     * zero-parameter method record. Annotations are routed like method annotations: {@code
     * TYPE_USE} annotations to the member's (return) type, others to the declaration annotations.
     *
     * @param member the annotation member declaration to process
     * @param cr the record of the enclosing annotation type
     * @param cu the compilation unit
     */
    private void processAnnotationMember(
            AnnotationMemberDeclaration member, ClassRecord cr, CompilationUnit cu) {
        try {
            MethodRecord mr = new MethodRecord();
            mr.sigIndex = pool.addString(member.getNameAsString() + "()");
            for (AnnotationExpr anno : member.getAnnotations()) {
                int idx = annosPool.addAnnotation(anno, cu, this);
                if (idx == IGNORED) {
                    continue;
                }
                if (hasTypeUse(anno, cu)) {
                    mr.returnTypeAnnos.add(new TypeAnno(idx, arrayElementPath(member.getType())));
                }
                if (!isTypeUseOnly(anno, cu)) {
                    mr.declAnnos.add(idx);
                }
            }
            extractTypeAnnotations(member.getType(), new ArrayList<>(), mr.returnTypeAnnos, cu);
            cr.methods.add(mr);
        } catch (IOException e) {
            throw new RuntimeException(
                    "Serialization failure in annotation member: " + e.getMessage(), e);
        }
    }

    /**
     * Processes a method declaration, extracting its annotations, return type annotations, receiver
     * annotations, and parameter annotations.
     *
     * @param md the method declaration to process
     * @param cr the class record to add the method to
     * @param cu the compilation unit
     */
    private void processMethod(MethodDeclaration md, ClassRecord cr, CompilationUnit cu) {
        // A declaration-position TYPE_USE annotation on a method annotates the element type of an
        // array return (if any), not the array reference; build the matching type path.
        processCallable(md, md.getType(), arrayElementPath(md.getType()), cr, cu);
    }

    /**
     * Processes a constructor declaration, extracting its annotations, receiver annotations, and
     * parameter annotations.
     *
     * @param cd the constructor declaration to process
     * @param cr the class record to add the constructor to
     * @param cu the compilation unit
     */
    private void processConstructor(ConstructorDeclaration cd, ClassRecord cr, CompilationUnit cu) {
        // A declaration-position TYPE_USE annotation on a constructor describes the constructed
        // object; an empty type path targets the (return) type directly.
        processCallable(cd, null, new ArrayList<>(), cr, cu);
    }

    /**
     * Processes a method or constructor declaration: the signature, the annotations in declaration
     * position (routed to the return type, the declaration annotations, or both, according to their
     * {@code @Target}), the return type's type annotations, the receiver, the parameters, and the
     * type parameters.
     *
     * @param decl the method or constructor declaration to process
     * @param returnType the declared return type whose type annotations to extract, or {@code null}
     *     for a constructor (which has none)
     * @param declPositionPath the type path a declaration-position {@code TYPE_USE} annotation
     *     applies to: the innermost array component of the return type for a method, the empty path
     *     for a constructor
     * @param cr the class record to add the method record to
     * @param cu the compilation unit
     */
    private void processCallable(
            CallableDeclaration<?> decl,
            @Nullable Type returnType,
            List<TypePathStep> declPositionPath,
            ClassRecord cr,
            CompilationUnit cu) {
        if (decl.isPrivate()) {
            // Mirror AnnotationFileParser.skipNode, which skips private declarations.
            return;
        }
        try {
            MethodRecord mr = new MethodRecord();
            mr.sigIndex = pool.addString(MethodSignaturePrinter.toString(decl));
            for (AnnotationExpr anno : decl.getAnnotations()) {
                int idx = annosPool.addAnnotation(anno, cu, this);
                if (idx == IGNORED) {
                    continue;
                }
                if (hasTypeUse(anno, cu)) {
                    mr.returnTypeAnnos.add(new TypeAnno(idx, declPositionPath));
                }
                if (!isTypeUseOnly(anno, cu)) {
                    // Annotation has a declaration-position target: also a declaration annotation.
                    mr.declAnnos.add(idx);
                }
            }
            if (returnType != null) {
                extractTypeAnnotations(returnType, new ArrayList<>(), mr.returnTypeAnnos, cu);
            }

            if (decl.getReceiverParameter().isPresent()) {
                ReceiverParameter rp = decl.getReceiverParameter().get();
                for (AnnotationExpr anno : rp.getAnnotations()) {
                    int recIdx = annosPool.addAnnotation(anno, cu, this);
                    if (recIdx != IGNORED) {
                        mr.receiverAnnos.add(new TypeAnno(recIdx));
                    }
                }
                extractTypeAnnotations(rp.getType(), new ArrayList<>(), mr.receiverAnnos, cu);
            }

            for (Parameter p : decl.getParameters()) {
                List<TypeAnno> pAnnos = new ArrayList<>();
                extractTypeAnnotations(p.getType(), new ArrayList<>(), pAnnos, cu);
                mr.paramAnnos.add(pAnnos);
                List<Integer> pdAnnos = new ArrayList<>();
                for (AnnotationExpr anno : p.getAnnotations()) {
                    int idx = annosPool.addAnnotation(anno, cu, this);
                    if (idx == IGNORED) {
                        continue;
                    }
                    if (hasTypeUse(anno, cu)) {
                        // For varargs, annotations on the parameter declaration apply to the
                        // innermost array component (matching AnnotationFileParser.
                        // annotateInnermostComponentType), which for a multidimensional vararg
                        // (e.g. "String[]... args", equivalent to "String[][] args") is more than
                        // one level below the overall parameter type: one ARRAY step for the
                        // vararg's own implicit array level, plus one more per array level already
                        // present in the declared type (p.getType(), "String[]" here). For
                        // non-varargs, use only the element path of the declared type.
                        List<TypePathStep> path;
                        if (p.isVarArgs()) {
                            path = new ArrayList<>();
                            path.add(new TypePathStep((byte) 0, (byte) 0));
                            path.addAll(arrayElementPath(p.getType()));
                        } else {
                            path = arrayElementPath(p.getType());
                        }
                        pAnnos.add(new TypeAnno(idx, path));
                    }
                    if (!isTypeUseOnly(anno, cu)) {
                        pdAnnos.add(idx);
                    }
                }
                if (p.isVarArgs()) {
                    // Annotations written immediately before "..." (e.g. "Foo @Nullable ...
                    // args") apply to the array type itself, matching
                    // AnnotationFileParser.processParameters's
                    // annotate(paramType, param.getVarArgsAnnotations(), param). JLS does not
                    // permit declaration annotations in this position, so these are always
                    // type-use and apply directly to the parameter's array type (empty path).
                    for (AnnotationExpr anno : p.getVarArgsAnnotations()) {
                        int idx = annosPool.addAnnotation(anno, cu, this);
                        if (idx != IGNORED) {
                            pAnnos.add(new TypeAnno(idx));
                        }
                    }
                }
                mr.paramDeclAnnos.add(pdAnnos);
            }
            mr.typeParams.addAll(extractTypeParams(decl.getTypeParameters(), cu));
            cr.methods.add(mr);
        } catch (IOException e) {
            throw new RuntimeException(
                    "Serialization failure in " + decl.getNameAsString() + ": " + e.getMessage(),
                    e);
        }
    }

    /**
     * Processes a field declaration, extracting its annotations and type annotations.
     *
     * @param fd the field declaration to process
     * @param cr the class record to add the field to
     * @param cu the compilation unit
     */
    private void processField(FieldDeclaration fd, ClassRecord cr, CompilationUnit cu) {
        if (fd.isPrivate()) {
            // Mirror AnnotationFileParser.skipNode, which skips private declarations.
            return;
        }
        try {
            for (VariableDeclarator vd : fd.getVariables()) {
                FieldRecord fr = new FieldRecord();
                fr.nameIndex = pool.addString(vd.getNameAsString());
                for (AnnotationExpr anno : fd.getAnnotations()) {
                    int idx = annosPool.addAnnotation(anno, cu, this);
                    if (idx == IGNORED) {
                        continue;
                    }
                    if (hasTypeUse(anno, cu)) {
                        // Annotation in declaration position annotates the element type of an
                        // array field (if any), not the array reference.
                        fr.typeAnnos.add(new TypeAnno(idx, arrayElementPath(vd.getType())));
                    }
                    if (!isTypeUseOnly(anno, cu)) {
                        // Annotation has a declaration-position target: also a declaration
                        // annotation.
                        fr.declAnnos.add(idx);
                    }
                }
                extractTypeAnnotations(vd.getType(), new ArrayList<>(), fr.typeAnnos, cu);
                cr.fields.add(fr);
            }
        } catch (IOException e) {
            throw new RuntimeException("Serialization failure in field: " + e.getMessage(), e);
        }
    }

    /**
     * Extracts type annotations from a JavaParser type, walking the type tree to build the
     * corresponding type paths.
     *
     * @param type the type to extract annotations from
     * @param currentPath the current type path, built during traversal
     * @param result the list to add the extracted type annotations to
     * @param cu the compilation unit
     */
    private void extractTypeAnnotations(
            Type type, List<TypePathStep> currentPath, List<TypeAnno> result, CompilationUnit cu)
            throws IOException {
        if (type == null) return;

        for (AnnotationExpr ann : type.getAnnotations()) {
            int idx = annosPool.addAnnotation(ann, cu, this);
            if (idx != IGNORED) {
                result.add(new TypeAnno(idx, currentPath));
            }
        }

        if (type instanceof ArrayType) {
            currentPath.add(new TypePathStep((byte) 0, (byte) 0)); // ARRAY
            extractTypeAnnotations(((ArrayType) type).getComponentType(), currentPath, result, cu);
            currentPath.remove(currentPath.size() - 1);
        } else if (type instanceof ClassOrInterfaceType) {
            ClassOrInterfaceType cit = (ClassOrInterfaceType) type;
            if (cit.getTypeArguments().isPresent()) {
                int i = 0;
                for (Type t : cit.getTypeArguments().get()) {
                    currentPath.add(new TypePathStep((byte) 3, (byte) i)); // TYPE_ARGUMENT
                    extractTypeAnnotations(t, currentPath, result, cu);
                    currentPath.remove(currentPath.size() - 1);
                    i++;
                }
            }
        } else if (type instanceof WildcardType) {
            WildcardType wt = (WildcardType) type;
            if (wt.getExtendedType().isPresent()) {
                // WILDCARD_BOUND. JVMS leaves argIndex unused (0) for this kind, since a real
                // wildcard has only one structurally possible bound; CF's AnnotatedWildcardType,
                // however, always synthesizes both an extends and a super bound (defaulting
                // whichever was not written), so argIndex is repurposed here (0 = extends bound,
                // 1 = super bound, below) to tell BinaryStubReader.resolvePath which one to
                // annotate -- see that method for why this distinction cannot be recovered from
                // the resolved AnnotatedWildcardType alone.
                currentPath.add(new TypePathStep((byte) 2, (byte) 0));
                extractTypeAnnotations(wt.getExtendedType().get(), currentPath, result, cu);
                currentPath.remove(currentPath.size() - 1);
            }
            if (wt.getSuperType().isPresent()) {
                currentPath.add(new TypePathStep((byte) 2, (byte) 1)); // WILDCARD_BOUND, super
                extractTypeAnnotations(wt.getSuperType().get(), currentPath, result, cu);
                currentPath.remove(currentPath.size() - 1);
            }
        }
    }

    /**
     * Extracts type-parameter annotations from a list of {@link TypeParameter} declarations.
     *
     * @param typeParameters the type parameter declarations
     * @param cu the compilation unit
     * @return a list of TypeParamRecord, one per type parameter
     * @throws IOException if annotation serialization fails
     */
    private List<TypeParamRecord> extractTypeParams(
            List<TypeParameter> typeParameters, CompilationUnit cu) throws IOException {
        List<TypeParamRecord> result = new ArrayList<>(typeParameters.size());
        for (TypeParameter tp : typeParameters) {
            TypeParamRecord rec = new TypeParamRecord();
            // Annotations on the type variable itself (e.g. @X in <@X T>)
            for (AnnotationExpr ann : tp.getAnnotations()) {
                int idx = annosPool.addAnnotation(ann, cu, this);
                if (idx != IGNORED) {
                    rec.typeVarAnnos.add(idx);
                }
            }
            // Annotations on each bound (e.g. @NonNull Object in <T extends @NonNull Object>)
            for (ClassOrInterfaceType bound : tp.getTypeBound()) {
                List<TypeAnno> boundAnnos = new ArrayList<>();
                extractTypeAnnotations(bound, new ArrayList<>(), boundAnnos, cu);
                rec.boundAnnos.add(boundAnnos);
            }
            result.add(rec);
        }
        return result;
    }

    /**
     * Writes a length-prefixed list of annotation-pool indices to the output stream.
     *
     * @param out the output stream
     * @param annoIndices the annotation-pool indices to write
     * @throws IOException if writing fails
     */
    private static void writeAnnoIndices(DataOutputStream out, List<Integer> annoIndices)
            throws IOException {
        out.writeShort(annoIndices.size());
        for (int annoIdx : annoIndices) {
            out.writeInt(annoIdx);
        }
    }

    /**
     * Writes a length-prefixed list of type annotations to the output stream.
     *
     * @param out the output stream
     * @param typeAnnos the type annotations to write
     * @throws IOException if writing fails
     */
    private static void writeTypeAnnos(DataOutputStream out, List<TypeAnno> typeAnnos)
            throws IOException {
        out.writeShort(typeAnnos.size());
        for (TypeAnno ta : typeAnnos) {
            ta.write(out);
        }
    }

    /**
     * Writes a map from name (package or module) to annotation-pool indices to the output stream.
     *
     * @param out the output stream
     * @param annotatedNames map from name to the annotation-pool indices of its declaration
     *     annotations
     * @throws IOException if writing fails
     */
    private void writeAnnotatedNames(
            DataOutputStream out, Map<String, List<Integer>> annotatedNames) throws IOException {
        out.writeInt(annotatedNames.size());
        for (Map.Entry<String, List<Integer>> entry : annotatedNames.entrySet()) {
            out.writeInt(pool.addString(entry.getKey()));
            writeAnnoIndices(out, entry.getValue());
        }
    }

    /**
     * Writes a list of type-parameter records to the output stream.
     *
     * @param out the output stream
     * @param typeParams the list of type parameter records to write
     * @throws IOException if writing fails
     */
    private static void writeTypeParams(DataOutputStream out, List<TypeParamRecord> typeParams)
            throws IOException {
        out.writeShort(typeParams.size());
        for (TypeParamRecord tp : typeParams) {
            writeAnnoIndices(out, tp.typeVarAnnos);
            out.writeShort(tp.boundAnnos.size());
            for (List<TypeAnno> boundList : tp.boundAnnos) {
                writeTypeAnnos(out, boundList);
            }
        }
    }

    /**
     * Writes the accumulated class records and constant pool to the specified file in a compressed
     * binary format.
     *
     * @param file the output file (usually ending in .bin.gz)
     * @throws IOException if writing to the file fails
     */
    public void writeTo(File file) throws IOException {
        try (DataOutputStream out =
                new DataOutputStream(new GZIPOutputStream(new FileOutputStream(file)))) {
            out.writeInt(MAGIC);
            out.writeShort(VERSION);
            pool.write(out);
            annosPool.write(out);

            out.writeInt(classes.size());
            for (ClassRecord cr : classes) {
                out.writeInt(cr.nameIndex);
                out.writeInt(cr.outerNameIndex);
                out.writeByte(cr.kind);
                writeAnnoIndices(out, cr.declAnnos);

                out.writeShort(cr.fields.size());
                for (FieldRecord fr : cr.fields) {
                    out.writeInt(fr.nameIndex);
                    writeAnnoIndices(out, fr.declAnnos);
                    writeTypeAnnos(out, fr.typeAnnos);
                }

                out.writeShort(cr.methods.size());
                for (MethodRecord mr : cr.methods) {
                    out.writeInt(mr.sigIndex);
                    writeAnnoIndices(out, mr.declAnnos);
                    writeTypeAnnos(out, mr.returnTypeAnnos);
                    writeTypeAnnos(out, mr.receiverAnnos);

                    out.writeShort(mr.paramAnnos.size());
                    for (int p = 0; p < mr.paramAnnos.size(); p++) {
                        writeTypeAnnos(out, mr.paramAnnos.get(p));
                        writeAnnoIndices(out, mr.paramDeclAnnos.get(p));
                    }
                    writeTypeParams(out, mr.typeParams);
                }
                writeTypeParams(out, cr.typeParams);
                if (cr.kind == ClassRecord.KIND_RECORD) {
                    out.writeShort(cr.components.size());
                    for (ComponentRecord comp : cr.components) {
                        out.writeInt(comp.nameIndex);
                        writeAnnoIndices(out, comp.declAnnos);
                        writeTypeAnnos(out, comp.typeAnnos);
                        out.writeBoolean(comp.hasAccessor);
                    }
                }
            }

            writeAnnotatedNames(out, packages);
            writeAnnotatedNames(out, modules);
        }
    }

    /**
     * A visitor that prints the type-erased signature of a method or constructor. This ensures that
     * method signatures match the format expected by the binary stub reader.
     */
    private static class MethodSignaturePrinter extends SimpleVoidVisitor<Void> {
        /**
         * Returns the type-erased signature of a method or constructor declaration.
         *
         * @param decl the method or constructor declaration
         * @return the type-erased signature
         */
        static String toString(CallableDeclaration<?> decl) {
            MethodSignaturePrinter printer = new MethodSignaturePrinter();
            decl.accept(printer, null);
            return printer.getOutput();
        }

        /** The builder where the signature is accumulated. */
        private final StringBuilder sb = new StringBuilder();

        /**
         * Returns the accumulated signature.
         *
         * @return the signature string
         */
        String getOutput() {
            return sb.toString();
        }

        @Override
        public void visit(ConstructorDeclaration n, Void arg) {
            sb.append("<init>");
            appendParameters(n.getParameters(), arg);
        }

        @Override
        public void visit(MethodDeclaration n, Void arg) {
            sb.append(n.getName());
            appendParameters(n.getParameters(), arg);
        }

        /**
         * Appends the parenthesized, comma-separated parameter types.
         *
         * @param parameters the parameters
         * @param arg the visitor argument
         */
        private void appendParameters(List<Parameter> parameters, Void arg) {
            sb.append("(");
            for (Iterator<Parameter> i = parameters.iterator(); i.hasNext(); ) {
                i.next().accept(this, arg);
                if (i.hasNext()) {
                    sb.append(",");
                }
            }
            sb.append(")");
        }

        @Override
        public void visit(Parameter n, Void arg) {
            n.getType().accept(this, arg);
            if (n.isVarArgs()) {
                sb.append("[]");
            }
        }

        @Override
        public void visit(ClassOrInterfaceType n, Void arg) {
            sb.append(n.getName());
        }

        @Override
        public void visit(PrimitiveType n, Void arg) {
            switch (n.getType()) {
                case BOOLEAN:
                    sb.append("boolean");
                    break;
                case BYTE:
                    sb.append("byte");
                    break;
                case CHAR:
                    sb.append("char");
                    break;
                case DOUBLE:
                    sb.append("double");
                    break;
                case FLOAT:
                    sb.append("float");
                    break;
                case INT:
                    sb.append("int");
                    break;
                case LONG:
                    sb.append("long");
                    break;
                case SHORT:
                    sb.append("short");
                    break;
            }
        }

        @Override
        public void visit(ArrayType n, Void arg) {
            n.getComponentType().accept(this, arg);
            sb.append("[]");
        }
    }

    /**
     * Returns true if the annotation's {@code @Target} contains {@code TYPE_USE} — regardless of
     * whether it also contains declaration-position element types. Use this to decide whether an
     * annotation in declaration position should also be applied to the adjacent type.
     *
     * <p>If the annotation class cannot be loaded, returns false conservatively.
     *
     * @param anno the annotation expression
     * @param cu the compilation unit, used to resolve the annotation's simple name to its FQN
     * @return true if the annotation's {@code @Target} contains {@code TYPE_USE}
     * @see #isTypeUseOnly
     */
    private boolean hasTypeUse(AnnotationExpr anno, CompilationUnit cu) {
        ElementType[] targets =
                annotationTargets(fullyQualifyAnnotationName(anno.getNameAsString()));
        for (ElementType et : targets) {
            if (et == ElementType.TYPE_USE) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns true if the annotation is a pure type annotation — i.e., its {@code @Target} contains
     * only {@code TYPE_USE} and/or {@code TYPE_PARAMETER}. Such annotations appearing in
     * declaration position (field, method return) must be stored as type annotations only, not as
     * declaration annotations.
     *
     * <p>Contrast with {@link #hasTypeUse}, which returns true whenever {@code @Target} contains
     * {@code TYPE_USE} — even if it also contains declaration-position element types like {@code
     * METHOD} or {@code FIELD}. Dual-purpose annotations (where {@link #hasTypeUse} is true but
     * {@link #isTypeUseOnly} is false) must be stored in <em>both</em> places, matching the
     * behavior of the text-based {@link org.checkerframework.framework.stub.AnnotationFileParser}.
     *
     * <p>This also differs from {@link
     * org.checkerframework.javacutil.AnnotationUtils#isTypeUseAnnotation
     * AnnotationUtils.isTypeUseAnnotation}, which returns true whenever {@code @Target} contains
     * {@code TYPE_USE} — even if it also contains {@code METHOD}, {@code FIELD}, or other
     * declaration-position element types. That method is appropriate for deciding whether an
     * annotation <em>can</em> appear on a type use; this method is appropriate for deciding whether
     * an annotation appearing in declaration position must be treated exclusively as a type
     * annotation (and not also stored in {@code declAnnos}).
     *
     * <p>If the annotation class cannot be loaded (e.g., not on the stubifier classpath), returns
     * false conservatively.
     *
     * @param anno the annotation expression
     * @param cu the compilation unit, used to resolve the annotation's simple name to its FQN
     * @return true if the annotation's {@code @Target} contains only {@code TYPE_USE} and/or {@code
     *     TYPE_PARAMETER}
     * @see #hasTypeUse
     */
    private boolean isTypeUseOnly(AnnotationExpr anno, CompilationUnit cu) {
        ElementType[] targets =
                annotationTargets(fullyQualifyAnnotationName(anno.getNameAsString()));
        if (targets.length == 0) {
            // The class could not be loaded or has no @Target: conservatively treat the
            // annotation as a declaration annotation.
            return false;
        }
        for (ElementType et : targets) {
            if (et != ElementType.TYPE_USE && et != ElementType.TYPE_PARAMETER) {
                return false;
            }
        }
        return true;
    }

    /**
     * Returns a type-path list containing one {@code ARRAY} step for each array dimension of {@code
     * type}. For a non-array type, returns an empty list (annotates the type itself).
     *
     * <p>The text-based stub parser's {@code annotateAsArray} calls {@code
     * annotateInnermostComponentType}, which applies declaration-position {@code TYPE_USE}
     * annotations to the innermost component of the array, not to the array reference. For example,
     * {@code @Nullable T[]} (where {@code @Nullable} is {@code TYPE_USE}-only and appears in
     * declaration position) has {@code @Nullable} applied to {@code T}, not to {@code T[]}. The
     * correct binary type-path encoding is one ARRAY step per dimension. This does not apply to
     * declaration annotations (non-{@code TYPE_USE}), which are handled by {@code declAnnos} and do
     * bind to the whole array.
     *
     * @param type the JavaParser return type or field type
     * @return a mutable list of ARRAY path steps (empty if the type is not an array)
     */
    private static List<TypePathStep> arrayElementPath(Type type) {
        List<TypePathStep> path = new ArrayList<>();
        Type t = type;
        while (t instanceof ArrayType) {
            path.add(new TypePathStep((byte) 0, (byte) 0)); // ARRAY step
            t = ((ArrayType) t).getComponentType();
        }
        return path;
    }
}
