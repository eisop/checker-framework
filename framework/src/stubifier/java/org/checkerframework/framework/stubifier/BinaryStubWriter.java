package org.checkerframework.framework.stubifier;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.ImportDeclaration;
import com.github.javaparser.ast.Node;
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
import java.util.Optional;
import java.util.Set;
import java.util.zip.GZIPOutputStream;

/**
 * Writes parsed stub compilation units into the compressed binary stub format read by {@code
 * org.checkerframework.framework.stub.BinaryStubReader}. The output is a GZIP-compressed file (see
 * {@link #writeTo}), conventionally named with the {@link #BIN_SUFFIX} suffix ({@code .bin.gz},
 * e.g. {@link #OUTPUT_FILENAME}). Used at build time for the annotated JDK (via {@link
 * JavaStubifier}) and for the built-in checker stub files (via {@link BinaryStubFileGenerator}).
 *
 * <p>This extracts annotations structurally from class, interface, enum, annotation-type, and
 * record declarations and their members — including type-parameter bound annotations, enum
 * constants, annotation-type members, and record components (including an explicit canonical
 * constructor's parameter annotation overrides) — and writes them into a dense binary format
 * optimized for rapid loading without parsing overhead at checker startup.
 *
 * <p>Name resolution and member filtering deliberately mirror the text parser ({@code
 * AnnotationFileParser}): private declarations are skipped, annotation names resolve through
 * explicit imports, {@code java.lang}, and asterisk imports, and {@code @CFComment} is dropped.
 *
 * <p>A writer constructed with {@link #BinaryStubWriter(boolean) omitUnannotatedMembers} also drops
 * member records that carry no annotations; that is correct only for the annotated JDK. See that
 * field's documentation. Equivalence is enforced by {@code BinaryStubDiffChecker}; run {@code
 * NullnessBinaryStubDiffTest} after changing this class.
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
     * {@code ClassRecord.kind} value for a class or interface declaration: both {@code
     * ElementKind.CLASS} and {@code ElementKind.INTERFACE} map to this constant. This is the
     * canonical definition; {@code
     * org.checkerframework.framework.stub.BinaryStubData.ClassRecord#KIND_CLASS_OR_INTERFACE}
     * references it.
     */
    public static final byte KIND_CLASS_OR_INTERFACE = 0;

    /**
     * {@code ClassRecord.kind} value for an enum declaration. This is the canonical definition;
     * {@code org.checkerframework.framework.stub.BinaryStubData.ClassRecord#KIND_ENUM} references
     * it.
     */
    public static final byte KIND_ENUM = 1;

    /**
     * {@code ClassRecord.kind} value for an annotation-type declaration. This is the canonical
     * definition; {@code
     * org.checkerframework.framework.stub.BinaryStubData.ClassRecord#KIND_ANNOTATION_TYPE}
     * references it.
     */
    public static final byte KIND_ANNOTATION_TYPE = 2;

    /**
     * {@code ClassRecord.kind} value for a record declaration. This is the canonical definition;
     * {@code org.checkerframework.framework.stub.BinaryStubData.ClassRecord#KIND_RECORD} references
     * it.
     */
    public static final byte KIND_RECORD = 3;

    /**
     * Prefix of the simple signature of a constructor, as {@code
     * org.checkerframework.javacutil.ElementUtils#getSimpleSignature} writes it: {@code
     * <init>(...)}. This is the canonical definition; {@code
     * org.checkerframework.framework.stub.BinaryStubData#CONSTRUCTOR_SIG_PREFIX} references it.
     */
    public static final String CONSTRUCTOR_SIG_PREFIX = "<init>(";

    /**
     * Fully-qualified name of {@code CFComment}, which is never written to the binary format; see
     * {@link AnnotationPool#addAnnotation}. Interned for reference-equality checks.
     */
    public static final String CF_COMMENT =
            org.checkerframework.framework.qual.CFComment.class.getCanonicalName().intern();

    /**
     * Sentinel returned by {@link AnnotationPool#addAnnotation} for annotations that are not
     * written to the binary format ({@code @CFComment}). Callers must skip it.
     */
    private static final int IGNORED = -1;

    /**
     * Whether to omit member records that carry no annotations at all. True when writing the
     * annotated JDK, false when writing a built-in stub file's binary form.
     *
     * <p>A built-in stub file needs a record for every member it declares, annotated or not: {@code
     * BinaryStubReader} marks each matched member with {@code @FromStubFile}, exactly as {@code
     * AnnotationFileParser.markAsFromStubFile} does on the text side, and a member with no record
     * is never matched. The annotated JDK is never marked, and applying an all-empty record is a
     * no-op there for a genuinely-declared member -- {@code applyMethodRecords} finds no
     * declaration annotations and {@code hasTypeInfo} is false, so it builds no {@code
     * AnnotatedExecutableType}.
     *
     * <p>That no-op argument does NOT extend to a fake override: a stub declaration for a method
     * the class only inherits, at this subtype, resets the member's type to a fresh {@code
     * getAnnotatedType(overridden)} even when {@code mr.returnTypeAnnos} is empty (see {@code
     * BinaryStubReader#applyFakeOverride}) -- an all-empty fake-override record is NOT a no-op, so
     * {@link #isUnannotated(MethodRecord)} must never omit one. Whether a given method record will
     * be treated as a fake override at read time depends on the JDK version actually being compiled
     * against (the annotated JDK's own source tree reflects one JDK's shape; an older or newer real
     * JDK can declare a member directly where this tree's version only inherits it, or vice versa),
     * which this writer cannot know. {@link MethodRecord#hasOverrideAnnotation} therefore acts as
     * the writer's exemption: a method with no override relationship at all can never be a fake
     * override, and the annotated JDK's OpenJDK-derived style marks every actual override with
     * {@code @Override}, so a record is only considered for omission when it carries neither
     * annotations nor {@code @Override}.
     *
     * <p>Since 69.6% of the annotated JDK's method records and 85.6% of its field records are
     * all-empty, writing them costs 26% of the compressed file (320,674 down to 237,283 bytes) and
     * 22,398 {@code MethodRecord} objects, with their arrays, on every {@code BinaryStubData} load,
     * to accomplish nothing.
     *
     * <p>Class records are always written, even when nothing about the class is annotated: their
     * presence in {@code BinaryStubData.classes} is what tells {@code AnnotationFileElementTypes}
     * not to fall back to text-parsing that class.
     */
    private final boolean omitUnannotatedMembers;

    /** Creates a writer for a built-in stub file, which keeps every member record. */
    public BinaryStubWriter() {
        this(false);
    }

    /**
     * Creates a writer.
     *
     * @param omitUnannotatedMembers whether to omit member records carrying no annotations; see
     *     {@link #omitUnannotatedMembers}. Pass true only for the annotated JDK.
     */
    public BinaryStubWriter(boolean omitUnannotatedMembers) {
        this.omitUnannotatedMembers = omitUnannotatedMembers;
    }

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
         * @throws IOException if serialization fails; the annotation pool is left unchanged (the
         *     string pool may retain entries added for the abandoned annotation, which is harmless:
         *     an unreferenced constant-pool string only costs space)
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

            // Serialize before recording the index, so that a failure leaves the pool consistent.
            // The other order hands out an index that serializedAnnos never receives an entry for,
            // shifting every later index down by one -- so a reader given such a file would
            // silently apply the wrong annotation rather than fail. Both production callers happen
            // to abort the whole writer on this exception, which is the only reason the desync has
            // never been observed.
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            DataOutputStream dos = new DataOutputStream(baos);
            writer.writeAnnotationInline(dos, qualified, cu);
            dos.flush();

            int newIdx = serializedAnnos.size();
            serializedAnnos.add(baos.toByteArray());
            annoToIdx.put(key, newIdx);
            return newIdx;
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
         *
         * <p>Kind {@code 1} exists only to keep the numbering aligned with JVMS &sect;4.7.20.2; no
         * step of that kind is ever constructed. See {@link
         * BinaryStubWriter#extractTypeAnnotations} for what is dropped instead, and why the text
         * parser drops it too.
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
            writeByteCount(out, path.size(), "type-path steps");
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

        /**
         * Whether the declaration carries an {@code @Override} annotation. A method that overrides
         * or implements something can be a fake override at read time -- the reader classifies a
         * method as a fake override purely by failing to find it among the class's own declared
         * members ({@code BinaryStubReader#applyMethodRecords}), which depends on the JDK version
         * actually being compiled against, not on anything this writer can determine build-time
         * (see {@link #isUnannotated(MethodRecord)}). A method with no override relationship at all
         * can never be a fake override, so this flag is the writer's best available, purely
         * syntactic proxy for "might be one": conservatively true whenever {@code @Override} is
         * written, false otherwise (the {@code @Target} of {@code java.lang.Override} is {@code
         * METHOD} only, so it would already end up in {@link #declAnnos} via the same {@code
         * isTypeUseOnly} routing as any other declaration annotation; this field makes that
         * exemption explicit and independent of that routing instead of relying on it as a side
         * effect).
         */
        boolean hasOverrideAnnotation;
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
         * For a record declaration ({@code kind == KIND_RECORD}) whose body declares an explicit
         * (non-compact) canonical constructor, one type-annotation list per constructor parameter,
         * in parameter order -- matching {@code AnnotationFileParser}'s {@code
         * RecordStub#componentsInCanonicalConstructor} override, which suppresses automatic
         * transfer of the record component's own annotations onto the canonical constructor in
         * favor of whatever the explicit constructor declares itself. {@code null} if the record
         * has no such explicit constructor (the common case: the canonical constructor is compact,
         * or absent, and the component's own annotations are used instead).
         */
        @Nullable List<List<TypeAnno>> canonicalConstructorParamAnnos;

        /**
         * This class's own fully-qualified name, computed once by {@link #startClassRecord} from
         * the {@code enclosingFqn}/simple-name it was given. Not serialized; exists so the four
         * {@code process*} callers don't each re-derive it.
         */
        final String fqn;

        /**
         * The fully-qualified name this class's own members should treat as their outermost
         * enclosing class: {@link #fqn} itself if this class is top-level, otherwise the same
         * outermost name that was passed to this class. Not serialized; computed once by {@link
         * #startClassRecord} alongside {@link #fqn}.
         */
        final String childOutermostFqn;

        /**
         * Constructs a ClassRecord.
         *
         * @param nameIndex index of the class name in the constant pool
         * @param outerNameIndex index of the outermost enclosing class name, or 0 if top-level
         * @param kind one of the {@code KIND_*} constants
         * @param fqn this class's own fully-qualified name
         * @param childOutermostFqn the fully-qualified name this class's own members should treat
         *     as their outermost enclosing class
         */
        ClassRecord(
                int nameIndex,
                int outerNameIndex,
                byte kind,
                String fqn,
                String childOutermostFqn) {
            this.nameIndex = nameIndex;
            this.outerNameIndex = outerNameIndex;
            this.kind = kind;
            this.fqn = fqn;
            this.childOutermostFqn = childOutermostFqn;
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

    /**
     * Constant-pool indices (see {@link #pool}) of the signatures of every method directly declared
     * by an {@code interface} processed so far, across the whole run -- not scoped to one class's
     * own supertypes. A method overriding an interface's (possibly default) method is the only
     * shape of method that can ever be a fake override at read time (see {@link
     * #omitUnannotatedMembers}): {@code BinaryStubReader} falls back to the fake-override path
     * exactly when a stub-declared method is not found among a class's own real members, which
     * happens for a method the class inherits rather than overrides -- and only interface default
     * methods (or, across JDK versions, a method a later JDK "pulled down" into a concrete
     * override, as {@code java.util.TreeMap}'s {@code computeIfPresent} et al. did) are ever both
     * "declared here" and "sometimes only inherited elsewhere." A same-signature method declared
     * only in a superclass, not an interface, cannot become a fake override this way and is exempt
     * only via {@link MethodRecord#hasOverrideAnnotation}, not this set.
     *
     * <p>This is a whole-run, not a per-hierarchy, index: it does not verify that the exempted
     * method's class actually implements the matching interface, only that some interface somewhere
     * in the processed tree declares the same signature. That is a deliberately conservative
     * over-approximation -- computing the real supertype chain would need this writer to track and
     * resolve each class's supertypes, which it does not do -- and only affects which unannotated
     * records are kept, never which annotations are applied.
     *
     * <p>Populated as interfaces are processed and consulted only when {@link #writeTo} makes the
     * final omission decision, since a class using this pattern (e.g. {@code
     * java.util.TreeMap.NavigableSubMap}) can be processed before the interface it implements (e.g.
     * {@code java.util.Map}) in file-traversal order.
     */
    private final Set<Integer> interfaceMethodSigs = new HashSet<>();

    /** Map of package name to annotation-pool indices of its declaration annotations. */
    private final Map<String, List<Integer>> packages = new LinkedHashMap<>();

    /** Map of module name to annotation-pool indices of its declaration annotations. */
    private final Map<String, List<Integer>> modules = new LinkedHashMap<>();

    /** Map from simple class names to their fully-qualified names. */
    private final Map<String, String> simpleToFqn = new HashMap<>();

    /**
     * Map from the simple name of a statically-imported constant to the fully-qualified name of its
     * declaring class, e.g. {@code "MAX_VALUE" -> "java.lang.Integer"} for {@code import static
     * java.lang.Integer.MAX_VALUE;}. Populated only from non-asterisk static imports; an asterisk
     * static import ({@code import static java.lang.Integer.*;}) does not name the constant's
     * declaring class up front, so it is left unresolved here the same way an asterisk (non-static)
     * import is left out of {@link #simpleToFqn}.
     */
    private final Map<String, String> staticImportedConstants = new HashMap<>();

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
     * imports. A name that resolves through none of these is returned unchanged.
     *
     * <p>An unchanged (dotless) result is NOT always safe to treat as unresolvable by both this
     * writer and the text parser: the asterisk-import probe above resolves a candidate package via
     * {@code Class.forName} on the stubifier's own (narrow) build classpath, so failure there only
     * proves the package is absent from THAT classpath, not that {@code Elements} would fail to
     * resolve it at checker runtime on whatever classpath the checker's invocation supplies. {@link
     * #annotationTargets} is the caller that acts on this distinction: see its documentation for
     * how it tells a genuinely-unresolvable name (no import at all) apart from this case.
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

    /** Cache for {@link #classInPackage}, keyed by {@code pkg + "." + name}. */
    private final Map<String, String> classInPackageCache = new HashMap<>();

    /**
     * Returns the fully-qualified name of the class with the given simple name in the given
     * package, or {@code null} if the package contains no class of that name (on the stubifier
     * classpath). Like {@link #annotationInPackage}, but for class-literal resolution: it does not
     * require the loaded class to be an annotation type, and negative results are cached the same
     * way and for the same reason (see {@link #annotationInPackage}'s comment).
     *
     * @param pkg the package name
     * @param name the simple name
     * @return the fully-qualified name, or {@code null}
     */
    private String classInPackage(String pkg, String name) {
        String candidate = pkg + "." + name;
        String result =
                classInPackageCache.computeIfAbsent(
                        candidate,
                        c -> {
                            try {
                                Class.forName(c);
                                return c;
                            } catch (ClassNotFoundException e) {
                                return NOT_FOUND;
                            }
                        });
        return result == NOT_FOUND ? null : result;
    }

    /** Sentinel for {@link #annotationInPackageCache}: no annotation class by that name exists. */
    private static final String NOT_FOUND = "";

    /**
     * Sentinel for {@link #annotationTargetsCache}: the annotation class loaded, but carries no
     * {@code @Target} meta-annotation. Such an annotation is permitted in every declaration context
     * and no type context (JLS 9.6.4.1), which is exactly what an empty array means to {@link
     * #hasTypeUse} and {@link #isTypeUseOnly}. Distinct from {@link #NOT_LOADABLE}: {@code
     * java.lang.SuppressWarnings} really does report no {@code @Target} through reflection.
     */
    private static final ElementType[] NO_TARGET = new ElementType[0];

    /**
     * Sentinel for {@link #annotationTargetsCache}: the annotation class could not be loaded, so
     * its {@code @Target} is unknown. Must not be confused with {@link #NO_TARGET}; see {@link
     * #annotationTargets(AnnotationExpr)}.
     */
    private static final ElementType[] NOT_LOADABLE = new ElementType[0];

    /**
     * Cache from fully-qualified annotation class name to the element types in its {@code @Target}
     * meta-annotation, or to {@link #NO_TARGET} or {@link #NOT_LOADABLE}. Avoids a reflective
     * {@code Class.forName} lookup per annotation occurrence (the same few annotation classes occur
     * tens of thousands of times across the JDK stubs).
     */
    private final Map<String, ElementType[]> annotationTargetsCache = new HashMap<>();

    /**
     * Returns the element types in the {@code @Target} meta-annotation of the given annotation
     * class, or {@link #NO_TARGET} if it declares none, or {@link #NOT_LOADABLE} if the class
     * cannot be loaded.
     *
     * @param fqn the fully-qualified name of the annotation class
     * @return the {@code @Target} element types, or a sentinel
     */
    private ElementType[] annotationTargetsByName(String fqn) {
        return annotationTargetsCache.computeIfAbsent(
                fqn,
                name -> {
                    try {
                        Target target = Class.forName(name).getAnnotation(Target.class);
                        return target == null ? NO_TARGET : target.value();
                    } catch (ClassNotFoundException | LinkageError e) {
                        return NOT_LOADABLE;
                    }
                });
    }

    /**
     * Returns the {@code @Target} element types of {@code anno}, for {@link #hasTypeUse} and {@link
     * #isTypeUseOnly} to route the annotation with.
     *
     * <p>Both callers must know the {@code @Target} to route an annotation correctly, and both used
     * to read a failure to load the class as "no {@code @Target}" -- which routes a {@code
     * TYPE_USE}-only qualifier to {@code declAnnos} alone, whereupon {@code
     * BinaryStubReader.filterApplicable} discards it against the real {@code @Target} at checker
     * runtime. The annotation vanishes, with no diagnostic on either side. Since the stubifier's
     * classpath (stubparser plus checker-qual) is narrower than a checker's, that is a live hazard
     * for any stub file naming a third-party annotation. Fail instead: the caller turns this into a
     * skipped stub file, which then keeps its text parsing.
     *
     * <p>A dotless result from {@link #fullyQualifyAnnotationName} means the name was never
     * resolved: no {@code java.lang} match, no explicit import, no asterisk import matched it
     * either. Two different situations produce that outcome, and only one of them is safe to treat
     * as "no {@code @Target}" silently:
     *
     * <ul>
     *   <li>The compilation unit has no asterisk import at all, so nothing could have supplied the
     *       name -- it is either a typo or (as with the JDK's own {@code java.beans.EventHandler}
     *       and {@code java.beans.Statement}, which use {@code @ConstructorProperties} by
     *       same-package visibility with no import) a reference the text parser cannot qualify
     *       either: {@code AnnotationFileParser.getAnnotation} looks the as-written name up with
     *       {@code Elements.getTypeElement}, which requires a canonical name and so also fails on a
     *       bare same-package reference. Neither this writer nor the text parser can resolve such a
     *       name, so both drop the annotation and the binary form's routing of it cannot matter.
     *       {@code SuppressFBWarnings} and {@code ConstructorProperties} are the two names in the
     *       annotated JDK that reach this case today.
     *   <li>The compilation unit DOES have an asterisk import, and {@link
     *       #fullyQualifyAnnotationName} tried every one of them via {@code Class.forName} and
     *       failed for all of them. That only shows the package is missing from the stubifier's own
     *       narrow build classpath (stubparser plus checker-qual) -- not that a checker's {@code
     *       Elements}, resolving against whatever classpath its invocation supplies, would also
     *       fail. Silently falling through here used to route the annotation as a bare, unqualified
     *       name that {@code BinaryStubReader} then also cannot resolve ({@code AnnotationBuilder}
     *       throws a {@code UserError} that is swallowed to {@code null}) -- dropping an annotation
     *       the text parser could have resolved, with no diagnostic on either side. Fail instead,
     *       the same way as the dotted {@code NOT_LOADABLE} case below.
     * </ul>
     *
     * @param anno the annotation expression
     * @return the {@code @Target} element types, or {@link #NO_TARGET} if the annotation declares
     *     none, or its simple name could not be resolved and no asterisk import could be
     *     responsible
     * @throws IOException if {@code anno}'s name resolved to a fully-qualified name whose class
     *     cannot be loaded, or is a dotless name that an asterisk import in the same compilation
     *     unit might have supplied, leaving its {@code @Target} unknown either way
     */
    private ElementType[] annotationTargets(AnnotationExpr anno) throws IOException {
        String fqn = fullyQualifyAnnotationName(anno.getNameAsString());
        ElementType[] targets = annotationTargetsByName(fqn);
        if (targets == NOT_LOADABLE) {
            if (fqn.indexOf('.') == -1) {
                if (asteriskImportPackages.isEmpty()) {
                    return NO_TARGET;
                }
                throw new IOException(
                        "cannot resolve annotation @"
                                + fqn
                                + " in "
                                + sourceDescription(anno)
                                + ": it matched neither java.lang, nor an explicit import, nor any"
                                + " of this file's asterisk imports ("
                                + String.join(", ", asteriskImportPackages)
                                + ") on the stubifier's classpath, so its @Target cannot be read;"
                                + " put the annotation's declaring package on the stubifier"
                                + " classpath, or import the annotation explicitly");
            }
            throw new IOException(
                    "cannot load annotation "
                            + fqn
                            + " to read its @Target, so it cannot be routed to the type or the"
                            + " declaration; put it on the stubifier classpath");
        }
        return targets;
    }

    /**
     * Returns a description of {@code node}'s source file for use in error messages, or the node's
     * compilation unit if no file storage is available (e.g. a compilation unit parsed from a
     * stream, as {@link BinaryStubFileGenerator} does).
     *
     * @param node an AST node
     * @return a description of the node's source, for diagnostics
     */
    private static String sourceDescription(Node node) {
        Optional<CompilationUnit> cu = node.findCompilationUnit();
        if (!cu.isPresent()) {
            return "<unknown source>";
        }
        return cu.get()
                .getStorage()
                .map(s -> s.getPath().toString())
                .orElseGet(() -> cu.get().toString());
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
     * #asteriskImportPackages}, {@link #staticImportedConstants}) from the imports of the given
     * compilation unit.
     *
     * @param cu the compilation unit whose imports to use
     */
    private void initImportTables(CompilationUnit cu) {
        simpleToFqn.clear();
        asteriskImportPackages.clear();
        staticImportedConstants.clear();

        for (ImportDeclaration imp : cu.getImports()) {
            if (imp.isStatic()) {
                if (!imp.isAsterisk()) {
                    String fqn = imp.getNameAsString();
                    int lastDot = fqn.lastIndexOf('.');
                    if (lastDot != -1) {
                        String memberName = fqn.substring(lastDot + 1);
                        String declaringClass = fqn.substring(0, lastDot);
                        staticImportedConstants.put(memberName, declaringClass);
                    }
                }
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
     * Collects the declaration annotations of a package or module declaration into {@code target},
     * keyed by {@code name}. Does nothing if there are no annotations to store.
     *
     * @param name the package or module name
     * @param annos the declaration's annotations
     * @param target the map to populate ({@link #packages} or {@link #modules})
     * @param kindWord a word describing the kind of declaration ({@code "package"} or {@code
     *     "module"}), used only in the exception message on failure
     * @param cu the enclosing compilation unit
     */
    private void collectDeclAnnos(
            String name,
            List<AnnotationExpr> annos,
            Map<String, List<Integer>> target,
            String kindWord,
            CompilationUnit cu) {
        try {
            pool.addString(name);
            List<Integer> indices = new ArrayList<>();
            for (AnnotationExpr anno : annos) {
                int idx = annosPool.addAnnotation(anno, cu, this);
                if (idx != IGNORED) {
                    indices.add(idx);
                }
            }
            if (!indices.isEmpty()) {
                target.put(name, indices);
            }
        } catch (IOException e) {
            throw new RuntimeException(
                    "Serialization failure in " + kindWord + ": " + e.getMessage(), e);
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
                        pkg ->
                                collectDeclAnnos(
                                        pkg.getNameAsString(),
                                        pkg.getAnnotations(),
                                        packages,
                                        "package",
                                        cu));

        cu.getModule()
                .ifPresent(
                        mod ->
                                collectDeclAnnos(
                                        mod.getNameAsString(),
                                        mod.getAnnotations(),
                                        modules,
                                        "module",
                                        cu));

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
            writeCount(out, vals.size(), "annotation array elements");
            for (Expression val : vals) {
                writeValue(out, val, cu);
            }
        } else if (expr instanceof NameExpr) {
            String name = ((NameExpr) expr).getNameAsString();
            String declaringClass = staticImportedConstants.get(name);
            if (declaringClass != null) {
                // A bare reference to a statically-imported constant (e.g. "MAX_VALUE" from
                // "import static java.lang.Integer.MAX_VALUE;"). The reader's 'n'
                // (NameLiteralValue) tag is resolved only against the enclosing class and its
                // supertypes (BinaryStubReader#findFieldInType), which never finds a constant
                // imported from an unrelated class. Emit it the same way a FieldAccessExpr is
                // emitted instead (the 'e' tag, declaring class FQN + constant name), which
                // BinaryStubReader#resolveSingleValue resolves via an enum-constant lookup or,
                // since a static-imported constant is usually a plain field, the findFieldInType
                // fallback. Mirrors AnnotationFileParser#findVariableElement(NameExpr), which
                // resolves a bare name the same way via its importedConstants table.
                out.writeByte('e');
                out.writeInt(pool.addString(declaringClass));
                out.writeInt(pool.addString(name));
            } else {
                out.writeByte('n');
                out.writeInt(pool.addString(name));
            }
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
            writeCount(out, pairs.size(), "annotation elements");
            for (MemberValuePair pair : pairs) {
                out.writeInt(pool.addString(pair.getNameAsString()));
                writeValue(out, pair.getValue(), cu);
            }
        }
    }

    /**
     * Fully qualifies a JavaParser type by resolving it against the compilation unit's imports.
     * Used only for class-literal types ({@code ClassExpr.getType()}), which JLS 15.8.2 requires to
     * be raw (no type arguments), so a scoped {@code ClassOrInterfaceType} never carries type
     * arguments at any level here.
     *
     * @param type the type to fully qualify
     * @param cu the compilation unit, used to resolve imports
     * @return the fully-qualified type
     */
    private Type fullyQualify(Type type, CompilationUnit cu) {
        if (type instanceof ClassOrInterfaceType) {
            ClassOrInterfaceType cit = (ClassOrInterfaceType) type;
            if (cit.getScope().isPresent()) {
                // A scoped name like "Map.Entry": only the outermost scope ("Map") can be an
                // imported or same-package simple name -- the nested parts ("Entry") are member
                // types resolved relative to it, not independently importable. Qualify the
                // outermost scope through the same tables as the unscoped case, then re-attach
                // the nested simple names unchanged ("Map.Entry" -> "java.util.Map.Entry").
                // Before this fix, a scoped ClassOrInterfaceType was never qualified at all, so
                // e.g. "Map.Entry.class" with "import java.util.Map;" in scope was written as
                // "Map.Entry" -- unresolvable by BinaryStubReader#resolveSingleValue's single
                // Elements.getTypeElement(fqName) call.
                ClassOrInterfaceType scope = cit;
                StringBuilder nestedNames = new StringBuilder();
                while (scope.getScope().isPresent()) {
                    nestedNames.insert(0, "." + scope.getNameAsString());
                    scope = scope.getScope().get();
                }
                String outerName = scope.getNameAsString();
                String fqOuter = fullyQualify(outerName, cu);
                if (!fqOuter.equals(outerName)) {
                    return StaticJavaParser.parseType(fqOuter + nestedNames);
                }
            } else {
                String name = cit.getNameAsString();
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
     * Fully qualifies a simple name, trying in order: the compilation unit's explicit imports, its
     * own package, {@code java.lang}, then its asterisk imports.
     *
     * <p>This mirrors the text parser's {@code AnnotationFileParser.findTypeOfName} lookup order
     * for the cases the two share (explicit imports, current package, {@code java.lang}), with two
     * known divergences, both accepted as build-time-classpath limitations of the stubifier (see
     * the class javadoc): {@code findTypeOfName} also walks enclosing classes, which does not apply
     * here since a class literal's unqualified name is never an inherited member type of the stub's
     * own enclosing class; and it resolves asterisk-imported types with the same priority as
     * explicit imports (both populate one lookup table as the imports are scanned), whereas this
     * method tries them only as a last resort, after {@code java.lang} -- a misqualification is
     * possible only when an asterisk-imported package and {@code java.lang} (or the current
     * package) both declare a class of the same simple name, which does not occur in the shipped
     * stub sources.
     *
     * @param name the simple name to fully qualify
     * @param cu the compilation unit, used to resolve imports and the current package
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
        // An unimported class in the stub file's own package (e.g. "Foo" in
        // "@Anno(Foo.class)" where Foo is declared elsewhere in the same package): the text
        // parser's AnnotationFileParser.findTypeOfName tries this same packagePrefix + name
        // fallback. Without it, such a class literal stays an unqualified simple name that
        // BinaryStubReader#resolveSingleValue's single Elements.getTypeElement(fqName) call
        // cannot resolve, silently dropping the class-literal annotation element.
        if (cu.getPackageDeclaration().isPresent()) {
            String currentPackage = cu.getPackageDeclaration().get().getNameAsString();
            String inCurrentPackage = classInPackage(currentPackage, name);
            if (inCurrentPackage != null) {
                return inCurrentPackage;
            }
        }
        String javaLang = classInPackage("java.lang", name);
        if (javaLang != null) {
            return javaLang;
        }
        for (String pkg : asteriskImportPackages) {
            String candidate = classInPackage(pkg, name);
            if (candidate != null) {
                return candidate;
            }
        }
        return name;
    }

    /**
     * Processes annotations on a declaration, routing them into type-use and/or declaration
     * annotations.
     *
     * @param annotations the annotations to process
     * @param typeAnnos the list to which type-use annotations are added, or null to skip
     * @param declAnnos the list to which declaration annotations are added, or null to skip
     * @param typePath the path used for type-use annotations
     * @param filterByTarget if true, annotations are checked with {@link #hasTypeUse} and {@link
     *     #isTypeUseOnly}; if false, all valid annotations are added to the provided list(s)
     *     unconditionally.
     * @param cu the compilation unit
     * @throws IOException if writing to the annotation pool fails
     */
    private void routeAnnotations(
            List<AnnotationExpr> annotations,
            @Nullable List<TypeAnno> typeAnnos,
            @Nullable List<Integer> declAnnos,
            List<TypePathStep> typePath,
            boolean filterByTarget,
            CompilationUnit cu)
            throws IOException {
        for (AnnotationExpr anno : annotations) {
            int idx = annosPool.addAnnotation(anno, cu, this);
            if (idx == IGNORED) {
                continue;
            }
            if (typeAnnos != null && (!filterByTarget || hasTypeUse(anno))) {
                typeAnnos.add(new TypeAnno(idx, typePath));
            }
            if (declAnnos != null && (!filterByTarget || !isTypeUseOnly(anno))) {
                declAnnos.add(idx);
            }
        }
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
     * @param kind one of this class's {@code KIND_*} constants, identifying which of the three
     *     callers this is
     * @param cu the compilation unit
     * @return the registered class record, or {@code null} if the declaration is skipped. Its
     *     {@link ClassRecord#fqn} and {@link ClassRecord#childOutermostFqn} fields carry the
     *     composed fqn and the outermost name to pass to this class's own members, so callers do
     *     not need to re-derive either.
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
        String childOutermostFqn = outermostFqn.isEmpty() ? fqn : outermostFqn;
        ClassRecord cr =
                new ClassRecord(pool.addString(fqn), outerNameIndex, kind, fqn, childOutermostFqn);
        classes.add(cr);
        try {
            routeAnnotations(annotations, null, cr.declAnnos, Collections.emptyList(), false, cu);
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
                        KIND_CLASS_OR_INTERFACE,
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

        if (typeDecl.isInterface()) {
            // See interfaceMethodSigs: record every method this interface directly declares, so a
            // fake-override candidate elsewhere in the tree that overrides one of them -- even
            // unannotated and without @Override -- is exempted from omission once writeTo makes
            // that decision.
            for (BodyDeclaration<?> m : typeDecl.getMembers()) {
                if (m instanceof MethodDeclaration) {
                    interfaceMethodSigs.add(
                            pool.addString(MethodSignaturePrinter.toString((MethodDeclaration) m)));
                }
            }
        }

        processMembers(typeDecl.getMembers(), cr, cr.fqn, cr.childOutermostFqn, cu);
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
                        KIND_ENUM,
                        cu);
        if (cr == null) {
            return;
        }
        try {
            // Enum constants are modeled as field records; the reader's field lookup
            // (ElementFilter.fieldsIn) includes enum constants. A record is emitted even for
            // unannotated constants, unless omitUnannotatedMembers: for built-in stub files, the
            // reader marks every matched member with @FromStubFile, exactly as the text parser
            // marks every enum constant it processes.
            for (EnumConstantDeclaration constant : enumDecl.getEntries()) {
                FieldRecord fr = new FieldRecord();
                fr.nameIndex = pool.addString(constant.getNameAsString());
                routeAnnotations(
                        constant.getAnnotations(),
                        fr.typeAnnos,
                        fr.declAnnos,
                        Collections.emptyList(),
                        true,
                        cu);
                addFieldRecord(cr, fr);
            }
        } catch (IOException e) {
            throw new RuntimeException(
                    "Serialization failure in enum constants: " + e.getMessage(), e);
        }

        processMembers(enumDecl.getMembers(), cr, cr.fqn, cr.childOutermostFqn, cu);
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
                        KIND_ANNOTATION_TYPE,
                        cu);
        if (cr == null) {
            return;
        }
        processMembers(annoDecl.getMembers(), cr, cr.fqn, cr.childOutermostFqn, cu);
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
                        KIND_RECORD,
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

        // Find an explicit (non-compact) canonical constructor: a ConstructorDeclaration (not
        // CompactConstructorDeclaration, which has no parameter list of its own to annotate) whose
        // parameter types match the record header's own components in count and order, ignoring
        // annotations and parameter names. Java forbids two constructors with the same erased
        // signature, so this syntactic match is equivalent to AnnotationFileUtil
        // .isCanonicalConstructor's real-type-based check, but computable from the writer's
        // unresolved JavaParser AST alone. If found, its own parameter annotations override the
        // record components' when propagated to the canonical constructor (matching
        // AnnotationFileParser's RecordStub#componentsInCanonicalConstructor).
        String headerSignature =
                MethodSignaturePrinter.parameterTypesSignature(recordDecl.getParameters());
        for (BodyDeclaration<?> m : recordDecl.getMembers()) {
            if (!(m instanceof ConstructorDeclaration)) {
                continue;
            }
            ConstructorDeclaration ctor = (ConstructorDeclaration) m;
            if (!MethodSignaturePrinter.parameterTypesSignature(ctor.getParameters())
                    .equals(headerSignature)) {
                continue;
            }
            try {
                List<List<TypeAnno>> paramAnnos = new ArrayList<>(ctor.getParameters().size());
                for (Parameter param : ctor.getParameters()) {
                    List<TypeAnno> annos = new ArrayList<>();
                    routeAnnotations(
                            param.getAnnotations(), annos, null, Collections.emptyList(), true, cu);
                    paramAnnos.add(annos);
                }
                cr.canonicalConstructorParamAnnos = paramAnnos;
            } catch (IOException e) {
                throw new RuntimeException(
                        "Serialization failure in record canonical constructor: " + e.getMessage(),
                        e);
            }
            break;
        }

        // Process each record component.
        for (Parameter comp : recordDecl.getParameters()) {
            try {
                ComponentRecord compRec = new ComponentRecord();
                compRec.nameIndex = pool.addString(comp.getNameAsString());

                // Declaration-position annotations: dual-route like method annotations.
                // Annotation in declaration position on a record component annotates
                // the element type of an array component (if any), matching
                // AnnotationFileParser.processRecordField's annotate() call.
                routeAnnotations(
                        comp.getAnnotations(),
                        compRec.typeAnnos,
                        compRec.declAnnos,
                        arrayElementPath(comp.getType()),
                        true,
                        cu);
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

        processMembers(recordDecl.getMembers(), cr, cr.fqn, cr.childOutermostFqn, cu);
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
            routeAnnotations(
                    member.getAnnotations(),
                    mr.returnTypeAnnos,
                    mr.declAnnos,
                    arrayElementPath(member.getType()),
                    true,
                    cu);
            extractTypeAnnotations(member.getType(), new ArrayList<>(), mr.returnTypeAnnos, cu);
            addMethodRecord(cr, mr);
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
            // Checked by simple name, matching JavaStubifier.MinimizerVisitor's identical
            // "Deprecated" check: both run over unresolved JavaParser ASTs with no Elements to
            // resolve a possibly-qualified annotation name against.
            mr.hasOverrideAnnotation = decl.isAnnotationPresent("Override");
            // Annotation has a declaration-position target: also a declaration annotation.
            routeAnnotations(
                    decl.getAnnotations(),
                    mr.returnTypeAnnos,
                    mr.declAnnos,
                    declPositionPath,
                    true,
                    cu);
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

            // BinaryStubReader.applyMethodRecords resolves every paramAnnos path against the
            // FULL parameter type, which for a vararg parameter is the implicit array type. The
            // three annotation sources of a vararg parameter anchor at different depths of that
            // type, so each needs its own path prefix relative to the array type:
            //
            //   source                            anchors at                    path prefix
            //   --------------------------------  ----------------------------  ----------------
            //   annotations embedded in the       the array's component type    one ARRAY step
            //   declared type (p.getType())       (p.getType() excludes the
            //                                     implicit array level)
            //   decl-position annotations         the innermost array           ARRAY +
            //   (p.getAnnotations())              component                     arrayElementPath
            //   annotations before "..."          the array type itself         none (empty)
            //   (p.getVarArgsAnnotations())
            //
            // The text parser anchors identically: AnnotationFileParser.processParameters
            // annotates the array's component type with the declared type, routes decl-position
            // type qualifiers to the innermost component, and applies getVarArgsAnnotations()
            // to the array type.
            for (Parameter p : decl.getParameters()) {
                List<TypeAnno> pAnnos = new ArrayList<>();
                List<TypePathStep> declaredTypePath = new ArrayList<>();
                if (p.isVarArgs()) {
                    // The vararg's implicit array level (see the table above).
                    declaredTypePath.add(new TypePathStep((byte) 0, (byte) 0)); // ARRAY
                }
                extractTypeAnnotations(p.getType(), declaredTypePath, pAnnos, cu);
                mr.paramAnnos.add(pAnnos);
                List<Integer> pdAnnos = new ArrayList<>();
                // For varargs, annotations on the parameter declaration apply to the
                // innermost array component (matching AnnotationFileParser.
                // annotateInnermostComponentType), which for a multidimensional vararg
                // (e.g. "String[]... args", equivalent to "String[][] args") is more than
                // one level below the overall parameter type: one ARRAY step for the
                // vararg's own implicit array level, plus one more per array level already
                // present in the declared type (p.getType(), "String[]" here). For
                // non-varargs, use only the element path of the declared type.
                List<TypePathStep> paramPath;
                if (p.isVarArgs()) {
                    paramPath = new ArrayList<>();
                    paramPath.add(new TypePathStep((byte) 0, (byte) 0));
                    paramPath.addAll(arrayElementPath(p.getType()));
                } else {
                    paramPath = arrayElementPath(p.getType());
                }
                routeAnnotations(p.getAnnotations(), pAnnos, pdAnnos, paramPath, true, cu);
                if (p.isVarArgs()) {
                    // Annotations written immediately before "..." (e.g. "Foo @Nullable ...
                    // args") apply to the array type itself, matching
                    // AnnotationFileParser.processParameters's
                    // annotate(paramType, param.getVarArgsAnnotations(), param). JLS does not
                    // permit declaration annotations in this position, so these are always
                    // type-use and apply directly to the parameter's array type (empty path).
                    routeAnnotations(
                            p.getVarArgsAnnotations(),
                            pAnnos,
                            null,
                            Collections.emptyList(),
                            false,
                            cu);
                }
                mr.paramDeclAnnos.add(pdAnnos);
            }
            mr.typeParams.addAll(extractTypeParams(decl.getTypeParameters(), cu));
            addMethodRecord(cr, mr);
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
                // Annotation in declaration position annotates the element type of an
                // array field (if any), not the array reference.
                // Annotation has a declaration-position target: also a declaration
                // annotation.
                routeAnnotations(
                        fd.getAnnotations(),
                        fr.typeAnnos,
                        fr.declAnnos,
                        arrayElementPath(vd.getType()),
                        true,
                        cu);
                extractTypeAnnotations(vd.getType(), new ArrayList<>(), fr.typeAnnos, cu);
                addFieldRecord(cr, fr);
            }
        } catch (IOException e) {
            throw new RuntimeException("Serialization failure in field: " + e.getMessage(), e);
        }
    }

    /**
     * Extracts type annotations from a JavaParser type, walking the type tree to build the
     * corresponding type paths.
     *
     * <p>A {@code ClassOrInterfaceType}'s own type arguments are traversed, but not its scope's: an
     * annotation on a type argument of an enclosing type -- the {@code @D} of {@code Outer<@D
     * X>.Inner<T>} -- is dropped rather than encoded with a JVMS nested-type path step. {@code
     * AnnotationFileParser#annotate}'s {@code DECLARED} case has the identical limitation (it reads
     * only {@code getTypeArguments()}, never {@code getScope()}), so the two paths agree and {@code
     * BinaryStubDiffChecker} sees no difference. Implementing this would mean emitting and
     * resolving kind-1 path steps on both sides.
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
                    if (i > 0xFF) {
                        // The index is stored in one byte, matching JVMS's u1
                        // type_argument_index; a 257th type argument would silently wrap to 1 and
                        // annotate the wrong type argument on the reading side.
                        throw new IOException(
                                "too many type arguments (" + (i + 1) + ") on " + cit);
                    }
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
     * Writes {@code count} as an unsigned 16-bit value, the width every length prefix in this
     * format uses ({@code BinaryStubData} reads them back with {@code readUnsignedShort}).
     *
     * <p>{@code DataOutputStream.writeShort} silently keeps only the low 16 bits, so an oversized
     * count would produce a file whose every subsequent field is misaligned, and the reader has no
     * way to notice. Refuse to write such a file instead. No stub file comes close to the limit;
     * this exists so that the day one does, the build says so.
     *
     * <p>Package-private so {@code BinaryStubWriterTest} can exercise the bound directly: reaching
     * it through a real stub file means parsing 65536 annotation values, which costs two minutes.
     *
     * @param out the output stream
     * @param count the count to write
     * @param what what is being counted, for the error message
     * @throws IOException if {@code count} does not fit in an unsigned 16-bit value
     */
    static void writeCount(DataOutputStream out, int count, String what) throws IOException {
        if (count > 0xFFFF) {
            throw new IOException(
                    "too many " + what + " (" + count + "); the binary stub format records 65535");
        }
        out.writeShort(count);
    }

    /**
     * Writes {@code count} as an unsigned 8-bit value, the width JVMS &sect;4.7.20.1 gives a type
     * path's length. The 16-bit counterpart of {@link #writeCount}; see it for why the truncation
     * {@code writeByte} would perform is not acceptable.
     *
     * @param out the output stream
     * @param count the count to write
     * @param what what is being counted, for the error message
     * @throws IOException if {@code count} does not fit in an unsigned 8-bit value
     */
    static void writeByteCount(DataOutputStream out, int count, String what) throws IOException {
        if (count > 0xFF) {
            throw new IOException(
                    "too many " + what + " (" + count + "); the binary stub format records 255");
        }
        out.writeByte(count);
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
        writeCount(out, annoIndices.size(), "declaration annotations");
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
        writeCount(out, typeAnnos.size(), "type annotations");
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
        writeCount(out, typeParams.size(), "type parameters");
        for (TypeParamRecord tp : typeParams) {
            writeAnnoIndices(out, tp.typeVarAnnos);
            writeCount(out, tp.boundAnnos.size(), "type-parameter bounds");
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
        if (omitUnannotatedMembers) {
            // Deferred from addMethodRecord: interfaceMethodSigs is only complete once every
            // compilation unit in the run has been processed, so the omission decision for a
            // method record that might be a fake override cannot be made until now.
            for (ClassRecord cr : classes) {
                cr.methods.removeIf(this::isOmittable);
            }
        }
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

                writeCount(out, cr.fields.size(), "fields");
                for (FieldRecord fr : cr.fields) {
                    out.writeInt(fr.nameIndex);
                    writeAnnoIndices(out, fr.declAnnos);
                    writeTypeAnnos(out, fr.typeAnnos);
                }

                writeCount(out, cr.methods.size(), "methods");
                for (MethodRecord mr : cr.methods) {
                    out.writeInt(mr.sigIndex);
                    writeAnnoIndices(out, mr.declAnnos);
                    writeTypeAnnos(out, mr.returnTypeAnnos);
                    writeTypeAnnos(out, mr.receiverAnnos);

                    writeCount(out, mr.paramAnnos.size(), "method parameters");
                    for (int p = 0; p < mr.paramAnnos.size(); p++) {
                        writeTypeAnnos(out, mr.paramAnnos.get(p));
                        writeAnnoIndices(out, mr.paramDeclAnnos.get(p));
                    }
                    writeTypeParams(out, mr.typeParams);
                }
                writeTypeParams(out, cr.typeParams);
                if (cr.kind == KIND_RECORD) {
                    writeCount(out, cr.components.size(), "record components");
                    for (ComponentRecord comp : cr.components) {
                        out.writeInt(comp.nameIndex);
                        writeAnnoIndices(out, comp.declAnnos);
                        writeTypeAnnos(out, comp.typeAnnos);
                        out.writeBoolean(comp.hasAccessor);
                    }
                    out.writeBoolean(cr.canonicalConstructorParamAnnos != null);
                    if (cr.canonicalConstructorParamAnnos != null) {
                        writeCount(
                                out,
                                cr.canonicalConstructorParamAnnos.size(),
                                "canonical constructor parameters");
                        for (List<TypeAnno> paramAnnos : cr.canonicalConstructorParamAnnos) {
                            writeTypeAnnos(out, paramAnnos);
                        }
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

        /**
         * Returns the type-erased, parenthesized parameter-type signature of {@code parameters}
         * alone (no method/constructor name prefix) -- e.g. {@code "(String,int)"}. Used to
         * structurally match a record's header components against a candidate explicit canonical
         * constructor's parameters, ignoring annotations and parameter names: since Java forbids
         * two constructors with the same erased signature, a constructor whose parameter types
         * (ignoring annotations) match the record's own components in count and order must be the
         * canonical constructor, mirroring {@code AnnotationFileUtil#isCanonicalConstructor}'s
         * real-type-based check (which compares resolved parameter types, not names) but computable
         * purely from the writer's unresolved JavaParser AST.
         *
         * @param parameters the parameters
         * @return the parenthesized, comma-separated, type-erased parameter-type signature
         */
        static String parameterTypesSignature(List<Parameter> parameters) {
            MethodSignaturePrinter printer = new MethodSignaturePrinter();
            printer.appendParameters(parameters, null);
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
     * @param anno the annotation expression
     * @return true if the annotation's {@code @Target} contains {@code TYPE_USE}
     * @throws IOException if the annotation's {@code @Target} cannot be determined; see {@link
     *     #annotationTargets(AnnotationExpr)}
     * @see #isTypeUseOnly
     */
    private boolean hasTypeUse(AnnotationExpr anno) throws IOException {
        for (ElementType et : annotationTargets(anno)) {
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
     * @param anno the annotation expression
     * @return true if the annotation's {@code @Target} contains only {@code TYPE_USE} and/or {@code
     *     TYPE_PARAMETER}
     * @throws IOException if the annotation's {@code @Target} cannot be determined; see {@link
     *     #annotationTargets(AnnotationExpr)}
     * @see #hasTypeUse
     */
    private boolean isTypeUseOnly(AnnotationExpr anno) throws IOException {
        ElementType[] targets = annotationTargets(anno);
        if (targets.length == 0) {
            // The annotation declares no @Target, or its simple name could not be resolved:
            // conservatively treat it as a declaration annotation.
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
     * Returns true if {@code mr} carries no annotation anywhere: not on the method, its return
     * type, its receiver, any parameter, or any type parameter.
     *
     * @param mr the method record to inspect
     * @return true if the record carries no annotations
     */
    private static boolean isUnannotated(MethodRecord mr) {
        if (!mr.declAnnos.isEmpty()
                || !mr.returnTypeAnnos.isEmpty()
                || !mr.receiverAnnos.isEmpty()) {
            return false;
        }
        for (List<TypeAnno> paramAnnos : mr.paramAnnos) {
            if (!paramAnnos.isEmpty()) {
                return false;
            }
        }
        for (List<Integer> paramDeclAnnos : mr.paramDeclAnnos) {
            if (!paramDeclAnnos.isEmpty()) {
                return false;
            }
        }
        for (TypeParamRecord tp : mr.typeParams) {
            if (!tp.typeVarAnnos.isEmpty()) {
                return false;
            }
            for (List<TypeAnno> boundAnnos : tp.boundAnnos) {
                if (!boundAnnos.isEmpty()) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * Returns true if {@code mr} is safe for {@link #writeTo} to omit: it carries no annotation
     * anywhere ({@link #isUnannotated(MethodRecord)}), AND it cannot be a fake override at read
     * time. Such a record is a genuine no-op for the annotated JDK; see {@link
     * #omitUnannotatedMembers}.
     *
     * <p>A method record that could be a fake override must be kept even when {@link
     * #isUnannotated} is true, because {@code BinaryStubReader#applyFakeOverride} resets the
     * member's type at this subtype from a fresh {@code getAnnotatedType(overridden)} whenever the
     * record matches nothing among the class's own real members, independent of whether the record
     * carries any annotations. Two syntactic signals approximate "could be a fake override", since
     * this writer cannot resolve the real class hierarchy the reader will see: {@link
     * MethodRecord#hasOverrideAnnotation} (an explicit {@code @Override}, which also covers a
     * superclass override) and {@link #interfaceMethodSigs} (the signature matches some processed
     * interface's own method, which covers an unmarked interface-default override like {@code
     * java.util.TreeMap.NavigableSubMap}'s {@code computeIfAbsent}/{@code computeIfPresent}/{@code
     * compute}/{@code merge}/{@code putIfAbsent}, none of which carry {@code @Override} in the
     * annotated JDK's own source).
     *
     * @param mr the method record to inspect
     * @return true if the record carries no annotations and cannot be a fake override
     */
    private boolean isOmittable(MethodRecord mr) {
        return isUnannotated(mr)
                && !mr.hasOverrideAnnotation
                && !interfaceMethodSigs.contains(mr.sigIndex);
    }

    /**
     * Returns true if {@code fr} carries no annotation, on the field or on its type. See {@link
     * #isUnannotated(MethodRecord)}.
     *
     * @param fr the field record to inspect
     * @return true if the record carries no annotations
     */
    private static boolean isUnannotated(FieldRecord fr) {
        return fr.declAnnos.isEmpty() && fr.typeAnnos.isEmpty();
    }

    /**
     * Adds {@code mr} to {@code cr}. Unlike {@link #addFieldRecord}, the omission decision for an
     * unannotated record is not made here: whether {@code mr} is exempt as a possible fake override
     * can depend on an interface processed later in the run (see {@link #interfaceMethodSigs}), so
     * every method record is kept until {@link #writeTo} makes the final decision.
     *
     * @param cr the class record to add to
     * @param mr the method record to add
     */
    private void addMethodRecord(ClassRecord cr, MethodRecord mr) {
        cr.methods.add(mr);
    }

    /**
     * Adds {@code fr} to {@code cr}, unless it is an unannotated record that this writer omits.
     *
     * @param cr the class record to add to
     * @param fr the field record to add
     */
    private void addFieldRecord(ClassRecord cr, FieldRecord fr) {
        if (omitUnannotatedMembers && isUnannotated(fr)) {
            return;
        }
        cr.fields.add(fr);
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
