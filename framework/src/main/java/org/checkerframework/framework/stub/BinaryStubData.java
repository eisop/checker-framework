package org.checkerframework.framework.stub;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.zip.GZIPInputStream;

/**
 * In-memory representation of a binary stub file: the annotated JDK ({@code annotated-jdk.bin.gz})
 * or the binary form of a built-in checker stub file ({@code *.astub.bin.gz}). The binary format
 * stores annotation information extracted from the stub sources in a compact, pre-parsed form that
 * is faster to load than the original text stubs.
 *
 * <p>Instances are immutable after construction and contain no javac objects (only strings and
 * primitives), so they are cached per JVM and shared across compilations; see {@code
 * AnnotationFileElementTypes#loadedBinaryStubData}.
 *
 * <p>The binary format consists of a GZIP-compressed stream containing:
 *
 * <ol>
 *   <li>A 4-byte magic number ({@code 0xCF4A444B}).
 *   <li>A 2-byte version number.
 *   <li>A constant pool of UTF-8 strings (class names, field names, signatures, string literals).
 *   <li>An annotation pool of structural annotation records.
 *   <li>A sequence of {@link ClassRecord} entries, one per class, interface, enum, or annotation
 *       type.
 *   <li>Package and module annotation records.
 * </ol>
 *
 * @see BinaryStubReader
 * @see org.checkerframework.framework.stubifier.BinaryStubWriter
 */
public class BinaryStubData {

    /**
     * Magic number identifying the Checker Framework binary stub format. The value is defined once
     * in {@link org.checkerframework.framework.stubifier.BinaryStubWriter#MAGIC} and referenced
     * here (the constant is inlined at compile time, so there is no runtime dependency on the
     * stubifier).
     */
    public static final int MAGIC = org.checkerframework.framework.stubifier.BinaryStubWriter.MAGIC;

    /**
     * Format version of the binary stub file. Defined once in {@link
     * org.checkerframework.framework.stubifier.BinaryStubWriter#VERSION}.
     */
    public static final short VERSION =
            org.checkerframework.framework.stubifier.BinaryStubWriter.VERSION;

    /**
     * File-name suffix appended to a source stub file's name to name its binary form (e.g. {@code
     * jdk.astub} → {@code jdk.astub.bin.gz}). Defined once in {@link
     * org.checkerframework.framework.stubifier.BinaryStubWriter#BIN_SUFFIX}.
     */
    public static final String BIN_SUFFIX =
            org.checkerframework.framework.stubifier.BinaryStubWriter.BIN_SUFFIX;

    /**
     * File name of the binary stub file. Defined once in {@link
     * org.checkerframework.framework.stubifier.BinaryStubWriter#OUTPUT_FILENAME}.
     */
    public static final String FILENAME =
            org.checkerframework.framework.stubifier.BinaryStubWriter.OUTPUT_FILENAME;

    /** Annotation data containing its class name and structural element value pairs. */
    public static class AnnotationRecord {
        /**
         * Index into {@link BinaryStubData#stringPool} of the fully-qualified annotation class
         * name.
         */
        public final int nameIndex;

        /** Mapping from element member name (string pool index) to its structured value. */
        public final Map<Integer, Object> elementValues;

        /**
         * True if any element value (recursively) is a {@link NameLiteralValue}. Such a record's
         * resolution depends on the enclosing class, so it must not be memoised in the
         * enclosing-class-independent annotation cache; precomputed here so the reader does not
         * re-scan the values on every application.
         */
        public final boolean hasNameLiteral;

        /**
         * Constructs an AnnotationRecord.
         *
         * @param nameIndex index into the string pool of the fully-qualified annotation class name
         * @param elementValues mapping from element member name (string pool index) to its
         *     structured value
         */
        public AnnotationRecord(int nameIndex, Map<Integer, Object> elementValues) {
            this.nameIndex = nameIndex;
            this.elementValues = elementValues;
            boolean found = false;
            for (Object value : elementValues.values()) {
                if (containsNameLiteral(value)) {
                    found = true;
                    break;
                }
            }
            this.hasNameLiteral = found;
        }

        /**
         * Returns true if the value (recursively) contains a {@link NameLiteralValue}. Nested
         * annotation records are read before their enclosing record is constructed, so their
         * precomputed flag is used rather than recursing into them.
         *
         * @param value the element value to inspect
         * @return true if a name literal is found
         */
        private static boolean containsNameLiteral(Object value) {
            if (value instanceof NameLiteralValue) {
                return true;
            }
            if (value instanceof List) {
                for (Object item : (List<?>) value) {
                    if (containsNameLiteral(item)) {
                        return true;
                    }
                }
                return false;
            }
            if (value instanceof AnnotationRecord) {
                return ((AnnotationRecord) value).hasNameLiteral;
            }
            return false;
        }

        // AnnotationRecords are canonical singletons within their BinaryStubData (the annotation
        // pool deduplicates), and pool indices are only meaningful within one file, so records
        // are compared by identity (see BinaryStubDataCache.annoCache); no equals/hashCode.
    }

    /** Represents a Class literal value. */
    public static class ClassLiteralValue {
        /** The fully-qualified class name. */
        public final String className;

        /**
         * Constructs a ClassLiteralValue.
         *
         * @param className the fully-qualified class name
         */
        public ClassLiteralValue(String className) {
            this.className = className;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof ClassLiteralValue)) return false;
            ClassLiteralValue that = (ClassLiteralValue) o;
            return Objects.equals(className, that.className);
        }

        @Override
        public int hashCode() {
            return Objects.hashCode(className);
        }
    }

    /** Represents an Enum constant value. */
    public static class EnumConstantValue {
        /** The fully-qualified name of the enum class. */
        public final String enumClassName;

        /** The name of the enum constant. */
        public final String constantName;

        /**
         * Constructs an EnumConstantValue.
         *
         * @param enumClassName the fully-qualified name of the enum class
         * @param constantName the name of the enum constant
         */
        public EnumConstantValue(String enumClassName, String constantName) {
            this.enumClassName = enumClassName;
            this.constantName = constantName;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof EnumConstantValue)) return false;
            EnumConstantValue that = (EnumConstantValue) o;
            return Objects.equals(enumClassName, that.enumClassName)
                    && Objects.equals(constantName, that.constantName);
        }

        @Override
        public int hashCode() {
            return Objects.hash(enumClassName, constantName);
        }
    }

    /** Represents a simple name reference constant value. */
    public static class NameLiteralValue {
        /** The simple name of the referenced constant. */
        public final String name;

        /**
         * Constructs a NameLiteralValue.
         *
         * @param name the simple name of the referenced constant
         */
        public NameLiteralValue(String name) {
            this.name = name;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof NameLiteralValue)) return false;
            NameLiteralValue that = (NameLiteralValue) o;
            return Objects.equals(name, that.name);
        }

        @Override
        public int hashCode() {
            return Objects.hashCode(name);
        }
    }

    /**
     * A single step in a type-annotation path, describing how to navigate from a base type to the
     * component type that carries the annotation. The encoding mirrors the JVM's type-annotation
     * path encoding defined in JVMS §4.7.20.2.
     */
    public static class TypePathStep {
        /**
         * The kind of path step: {@code 0} = array component, {@code 1} = nested type, {@code 2} =
         * wildcard bound, {@code 3} = type argument. Only 4 values are ever defined, so a signed
         * {@code byte} is used as-is: no value this field takes needs the sign bit, so there is no
         * unsigned/signed distinction to make here (contrast {@link #argIndex}).
         */
        public final byte kind;

        /**
         * For {@link #kind} {@code 3} (type argument), the zero-based index of the type argument.
         * For {@link #kind} {@code 2} (wildcard bound), repurposed to distinguish an extends bound
         * ({@code 0}) from a super bound ({@code 1}): JVMS leaves this byte unused for wildcard
         * bounds (a real wildcard has only one structurally possible bound), but CF's {@code
         * AnnotatedWildcardType} always synthesizes both bounds, so {@link BinaryStubReader
         * #resolvePath} needs this to know which one a given path step is for. Unused (0) for other
         * kinds. Stored as a signed {@code byte} to match the 1-byte width of JVMS's {@code
         * type_argument_index} (a {@code u1}, so its wire value ranges over 0-255); a value of 128
         * or greater is stored as a negative {@code byte} and must be reinterpreted as unsigned
         * ({@code & 0xFF}) wherever it is widened to {@code int} for use (see {@code
         * BinaryStubReader#resolvePath}). Left as {@code byte} rather than widened to {@code int}:
         * one {@code TypePathStep} exists per path step of every type annotation in the annotated
         * JDK, so widening every instance's fields would multiply that memory cost for a value that
         * is realistically always a small, single-digit index.
         */
        public final byte argIndex;

        /**
         * Constructs a TypePathStep.
         *
         * @param kind the kind of path step: 0 = array component, 1 = nested type, 2 = wildcard
         *     bound, 3 = type argument
         * @param argIndex for kind 3, the zero-based index of the type argument; for kind 2, 0 =
         *     extends bound, 1 = super bound; unused (0) for other kinds
         */
        public TypePathStep(byte kind, byte argIndex) {
            this.kind = kind;
            this.argIndex = argIndex;
        }
    }

    /** Represents a type annotation with its type path. */
    public static class TypeAnno {
        /** Index into {@link BinaryStubData#annotationPool} of the annotation record. */
        public final int annoIndex;

        /** Type-path steps locating the annotated component. */
        public final TypePathStep[] path;

        /**
         * Constructs a TypeAnno.
         *
         * @param annoIndex index into the annotation pool of the annotation record
         * @param path type-path steps locating the annotated component
         */
        public TypeAnno(int annoIndex, TypePathStep[] path) {
            this.annoIndex = annoIndex;
            this.path = path;
        }
    }

    /**
     * Annotation data for a single type parameter (class-level or method-level). Stores the
     * annotations on the type variable itself and type annotations on each of its bounds.
     *
     * <p>For {@code <T extends @X Bound>}: {@code typeVarAnnos} holds pool indices of annotations
     * on {@code T} itself (applied to the lower bound); {@code boundAnnos[0]} holds the type
     * annotations on {@code Bound} (path-encoded as for a type annotation).
     */
    public static class TypeParamRecord {
        /**
         * Annotation-pool indices of the annotations on the type variable itself (applied to its
         * lower bound, matching how {@link
         * org.checkerframework.framework.stub.AnnotationFileParser#annotateTypeParameters} applies
         * them).
         */
        public int[] typeVarAnnos;

        /**
         * Per-bound type annotations. Element {@code i} holds the type annotations for the {@code
         * i}-th bound of this type parameter.
         */
        public TypeAnno[][] boundAnnos;

        /** Creates an empty TypeParamRecord; fields are populated by the binary reader. */
        public TypeParamRecord() {}
    }

    /** Annotation data for a single method or constructor. */
    public static class MethodRecord {
        /** Index into {@link BinaryStubData#stringPool} of the method's simple signature. */
        public int sigIndex;

        /** Creates an empty MethodRecord; fields are populated by the binary reader. */
        public MethodRecord() {}

        /** Annotation-pool indices of the declaration annotations on this method. */
        public int[] declAnnos;

        /** Type annotations on the return type. */
        public TypeAnno[] returnTypeAnnos;

        /** Type annotations on the receiver type ({@code this}). */
        public TypeAnno[] receiverAnnos;

        /**
         * Per-parameter type annotations. Element {@code i} holds the type annotations for
         * parameter {@code i}.
         */
        public TypeAnno[][] paramAnnos;

        /**
         * Per-parameter declaration annotations. Element {@code i} holds the annotation-pool
         * indices of the declaration annotations for parameter {@code i}.
         */
        public int[][] paramDeclAnnos;

        /**
         * Per-type-parameter annotation records. Element {@code i} holds the annotations for the
         * {@code i}-th type parameter declared by this method or constructor.
         */
        public TypeParamRecord[] typeParams;
    }

    /**
     * Annotation data for a single record component (i.e., one entry in a {@code record Foo(A a, B
     * b)} header).
     */
    public static class ComponentRecord {
        /** Index into {@link BinaryStubData#stringPool} of the component's simple name. */
        public int nameIndex;

        /** Creates an empty ComponentRecord; fields are populated by the binary reader. */
        public ComponentRecord() {}

        /** Annotation-pool indices of the declaration annotations on this component. */
        public int[] declAnnos;

        /** Type annotations on the component's type. */
        public TypeAnno[] typeAnnos;

        /**
         * True if the stub file contains an explicit zero-argument accessor method with the same
         * name as this component. When true, annotation propagation from the component to the
         * accessor is suppressed (the accessor's own annotations take precedence).
         */
        public boolean hasAccessor;
    }

    /** Annotation data for a single field. */
    public static class FieldRecord {
        /** Index into {@link BinaryStubData#stringPool} of the field's simple name. */
        public int nameIndex;

        /** Creates an empty FieldRecord; fields are populated by the binary reader. */
        public FieldRecord() {}

        /** Annotation-pool indices of the declaration annotations on this field. */
        public int[] declAnnos;

        /** Type annotations on the field's type. */
        public TypeAnno[] typeAnnos;
    }

    /** Annotation data for a single class or interface, including its members. */
    public static class ClassRecord {
        /**
         * {@link #kind} value for a class or interface declaration: both {@code ElementKind.CLASS}
         * and {@code ElementKind.INTERFACE} map to this constant.
         */
        public static final byte KIND_CLASS_OR_INTERFACE = 0;

        /** {@link #kind} value for an enum declaration. */
        public static final byte KIND_ENUM = 1;

        /** {@link #kind} value for an annotation-type declaration. */
        public static final byte KIND_ANNOTATION_TYPE = 2;

        /** {@link #kind} value for a record declaration. */
        public static final byte KIND_RECORD = 3;

        /**
         * Index into {@link BinaryStubData#stringPool} of the fully-qualified class name (using
         * {@code '.'} as separator, e.g. {@code "com.sun.tools.javac.code.Symbol.Completer"}).
         */
        public int nameIndex;

        /**
         * Index into {@link BinaryStubData#stringPool} of the outermost enclosing class name, or
         * {@code 0} if this is a top-level class (i.e., it has no enclosing class recorded in the
         * binary stub). For inner classes this is the name of the top-level class that contains
         * them, used to efficiently build the inner-class map without a string-scan heuristic.
         */
        public int outerNameIndex;

        /**
         * One of the {@code KIND_*} constants, recording what kind of declaration this class record
         * was written from. The reader compares this against the real {@code TypeElement}'s kind
         * before applying the record, since the JDK being compiled against can differ in version
         * from the one the annotated JDK's stub sources were written against (e.g. {@code
         * java.nio.ByteOrder} became a real enum in JDK 26 after being a plain class through JDK
         * 25) -- see {@code BinaryStubReader#applyClassRecord}.
         */
        public byte kind;

        /** Annotation-pool indices of the declaration annotations on this class. */
        public int[] declAnnos;

        /** Field records for the annotated fields of this class. */
        public FieldRecord[] fields;

        /** Method and constructor records for the annotated methods of this class. */
        public MethodRecord[] methods;

        /**
         * Per-type-parameter annotation records for this class. Element {@code i} holds the
         * annotations for the {@code i}-th type parameter declared by this class.
         */
        public TypeParamRecord[] typeParams;

        /**
         * Per-component annotation records for a record declaration ({@code kind == KIND_RECORD}).
         * Empty (zero-length) for non-record classes; non-null but possibly empty for records that
         * declare no annotated components.
         */
        public ComponentRecord[] components;

        /**
         * For a record declaration ({@code kind == KIND_RECORD}) whose body declares an explicit
         * (non-compact) canonical constructor, one type-annotation array per constructor parameter,
         * in parameter order -- matching {@code AnnotationFileParser}'s {@code
         * RecordStub#componentsInCanonicalConstructor} override. {@code null} if the record has no
         * such explicit constructor.
         */
        public TypeAnno[][] canonicalConstructorParamAnnos;

        /** Creates an empty ClassRecord; fields are populated by the binary reader. */
        public ClassRecord() {}
    }

    /** All strings referenced by the binary data. */
    public final String[] stringPool;

    /** Pre-parsed structural annotations referenced by indices in the records. */
    public final AnnotationRecord[] annotationPool;

    /**
     * Map from fully-qualified class name to its annotation record. The key uses {@code '.'} as
     * separator and covers both top-level classes and inner classes (e.g. {@code
     * "com.sun.tools.javac.code.Symbol.Completer"}).
     */
    public final Map<String, ClassRecord> classes = new HashMap<>();

    /**
     * Map from fully-qualified package name to annotation-pool indices of its declaration
     * annotations.
     */
    public final Map<String, int[]> packages = new HashMap<>();

    /** Map from module name to annotation-pool indices of its declaration annotations. */
    public final Map<String, int[]> modules = new HashMap<>();

    /**
     * Reads binary stub data from the given stream. The stream must supply the GZIP-compressed
     * binary format written by {@code BinaryStubWriter}.
     *
     * @param in the input stream to read from; the stream is closed when this constructor returns
     * @throws IOException if the stream cannot be read or contains an invalid/unsupported format
     */
    public BinaryStubData(InputStream in) throws IOException {
        try (DataInputStream dataIn = new DataInputStream(new GZIPInputStream(in))) {
            if (dataIn.readInt() != MAGIC) {
                throw new IOException("Invalid magic number");
            }
            short version = dataIn.readShort();
            if (version != VERSION) {
                throw new IOException("Unsupported version: " + version);
            }

            int poolSize = dataIn.readInt();
            stringPool = new String[poolSize];
            for (int i = 0; i < poolSize; i++) {
                stringPool[i] = dataIn.readUTF().intern();
            }

            int annoPoolSize = dataIn.readInt();
            annotationPool = new AnnotationRecord[annoPoolSize];
            for (int i = 0; i < annoPoolSize; i++) {
                int nameIdx = dataIn.readInt();
                int elementCount = dataIn.readUnsignedShort();
                Map<Integer, Object> elements = new HashMap<>();
                for (int j = 0; j < elementCount; j++) {
                    int memberIdx = dataIn.readInt();
                    elements.put(memberIdx, readAnnotationValue(dataIn));
                }
                annotationPool[i] = new AnnotationRecord(nameIdx, elements);
            }

            int classCount = dataIn.readInt();
            for (int i = 0; i < classCount; i++) {
                ClassRecord cr = new ClassRecord();
                cr.nameIndex = dataIn.readInt();
                cr.outerNameIndex = dataIn.readInt();
                cr.kind = dataIn.readByte();
                cr.declAnnos = readAnnoIndices(dataIn);

                int fieldCount = dataIn.readUnsignedShort();
                cr.fields = new FieldRecord[fieldCount];
                for (int j = 0; j < fieldCount; j++) {
                    FieldRecord fr = new FieldRecord();
                    fr.nameIndex = dataIn.readInt();
                    fr.declAnnos = readAnnoIndices(dataIn);
                    fr.typeAnnos = readTypeAnnos(dataIn);
                    cr.fields[j] = fr;
                }

                int methodCount = dataIn.readUnsignedShort();
                cr.methods = new MethodRecord[methodCount];
                for (int j = 0; j < methodCount; j++) {
                    MethodRecord mr = new MethodRecord();
                    mr.sigIndex = dataIn.readInt();
                    mr.declAnnos = readAnnoIndices(dataIn);
                    mr.returnTypeAnnos = readTypeAnnos(dataIn);
                    mr.receiverAnnos = readTypeAnnos(dataIn);

                    int paramCount = dataIn.readUnsignedShort();
                    mr.paramAnnos = new TypeAnno[paramCount][];
                    mr.paramDeclAnnos = new int[paramCount][];
                    for (int p = 0; p < paramCount; p++) {
                        mr.paramAnnos[p] = readTypeAnnos(dataIn);
                        mr.paramDeclAnnos[p] = readAnnoIndices(dataIn);
                    }
                    mr.typeParams = readTypeParams(dataIn);
                    cr.methods[j] = mr;
                }
                cr.typeParams = readTypeParams(dataIn);
                if (cr.kind == ClassRecord.KIND_RECORD) {
                    int componentCount = dataIn.readUnsignedShort();
                    cr.components = new ComponentRecord[componentCount];
                    for (int j = 0; j < componentCount; j++) {
                        ComponentRecord comp = new ComponentRecord();
                        comp.nameIndex = dataIn.readInt();
                        comp.declAnnos = readAnnoIndices(dataIn);
                        comp.typeAnnos = readTypeAnnos(dataIn);
                        comp.hasAccessor = dataIn.readBoolean();
                        cr.components[j] = comp;
                    }
                    if (dataIn.readBoolean()) {
                        int paramCount = dataIn.readUnsignedShort();
                        cr.canonicalConstructorParamAnnos = new TypeAnno[paramCount][];
                        for (int j = 0; j < paramCount; j++) {
                            cr.canonicalConstructorParamAnnos[j] = readTypeAnnos(dataIn);
                        }
                    }
                } else {
                    cr.components = new ComponentRecord[0];
                }
                classes.put(stringPool[cr.nameIndex], cr);
            }

            readAnnotatedNames(dataIn, packages);
            readAnnotatedNames(dataIn, modules);
        }
    }

    /**
     * Reads a single structured annotation value from the stream.
     *
     * @param dataIn the stream to read from
     * @return the deserialized value object
     * @throws IOException if reading fails
     */
    private Object readAnnotationValue(DataInputStream dataIn) throws IOException {
        byte tag = dataIn.readByte();
        switch (tag) {
            case 'Z':
                return dataIn.readBoolean();
            case 'C':
                return dataIn.readChar();
            case 'J':
                return dataIn.readLong();
            case 'D':
                return dataIn.readDouble();
            case 's':
                return stringPool[dataIn.readInt()];
            case 'c':
                return new ClassLiteralValue(stringPool[dataIn.readInt()]);
            case 'e':
                return new EnumConstantValue(
                        stringPool[dataIn.readInt()], stringPool[dataIn.readInt()]);
            case 'n':
                return new NameLiteralValue(stringPool[dataIn.readInt()]);
            case '@':
                {
                    int nameIdx = dataIn.readInt();
                    int elementCount = dataIn.readUnsignedShort();
                    Map<Integer, Object> elements = new HashMap<>();
                    for (int j = 0; j < elementCount; j++) {
                        int memberIdx = dataIn.readInt();
                        elements.put(memberIdx, readAnnotationValue(dataIn));
                    }
                    return new AnnotationRecord(nameIdx, elements);
                }
            case '[':
                {
                    int len = dataIn.readUnsignedShort();
                    List<Object> list = new ArrayList<>(len);
                    for (int i = 0; i < len; i++) {
                        list.add(readAnnotationValue(dataIn));
                    }
                    return list;
                }
            default:
                throw new IOException("Invalid annotation value tag: " + (char) tag);
        }
    }

    /**
     * Reads a length-prefixed array of annotation-pool indices from the stream (the counterpart of
     * {@code BinaryStubWriter#writeAnnoIndices}).
     *
     * @param dataIn the stream to read from
     * @return the annotation-pool indices
     * @throws IOException if the stream cannot be read
     */
    private static int[] readAnnoIndices(DataInputStream dataIn) throws IOException {
        int count = dataIn.readUnsignedShort();
        int[] result = new int[count];
        for (int i = 0; i < count; i++) {
            result[i] = dataIn.readInt();
        }
        return result;
    }

    /**
     * Reads a length-prefixed array of type annotations from the stream (the counterpart of {@code
     * BinaryStubWriter#writeTypeAnnos}).
     *
     * @param dataIn the stream to read from
     * @return the type annotations
     * @throws IOException if the stream cannot be read
     */
    private TypeAnno[] readTypeAnnos(DataInputStream dataIn) throws IOException {
        int count = dataIn.readUnsignedShort();
        TypeAnno[] result = new TypeAnno[count];
        for (int i = 0; i < count; i++) {
            result[i] = readTypeAnno(dataIn);
        }
        return result;
    }

    /**
     * Reads a map from name (package or module) to annotation-pool indices from the stream (the
     * counterpart of {@code BinaryStubWriter#writeAnnotatedNames}).
     *
     * @param dataIn the stream to read from
     * @param target the map to read into
     * @throws IOException if the stream cannot be read
     */
    private void readAnnotatedNames(DataInputStream dataIn, Map<String, int[]> target)
            throws IOException {
        int count = dataIn.readInt();
        for (int i = 0; i < count; i++) {
            String name = stringPool[dataIn.readInt()];
            target.put(name, readAnnoIndices(dataIn));
        }
    }

    /**
     * Reads a single {@link TypeAnno} from the stream.
     *
     * @param dataIn the stream to read from
     * @return the type annotation record
     * @throws IOException if the stream cannot be read
     */
    private TypeAnno readTypeAnno(DataInputStream dataIn) throws IOException {
        int annoIndex = dataIn.readInt();
        // path_length (JVMS 4.7.20.1) is a u1; read unsigned since it sizes the path array below
        // (a plain readByte() would misread a length of 128 or greater as negative). kind and
        // argIndex are stored as signed bytes (see TypePathStep's field javadoc), so plain
        // readByte() is equivalent to reading unsigned and narrowing back to byte; BinaryStubReader
        // re-widens argIndex to its unsigned meaning where it actually matters, at point of use.
        int pathLength = dataIn.readUnsignedByte();
        TypePathStep[] path = new TypePathStep[pathLength];
        for (int i = 0; i < pathLength; i++) {
            byte kind = dataIn.readByte();
            byte argIndex = 0;
            // TYPE_ARGUMENT (3): the type argument index. WILDCARD_BOUND (2): repurposed to
            // distinguish an extends bound (0) from a super bound (1); see TypePathStep#argIndex
            // and BinaryStubWriter.TypeAnno#write, which writes it for exactly these two kinds.
            if (kind == 3 || kind == 2) {
                argIndex = dataIn.readByte();
            }
            path[i] = new TypePathStep(kind, argIndex);
        }
        return new TypeAnno(annoIndex, path);
    }

    /**
     * Reads an array of {@link TypeParamRecord}s from the stream.
     *
     * @param dataIn the stream to read from
     * @return the type parameter records
     * @throws IOException if the stream cannot be read
     */
    private TypeParamRecord[] readTypeParams(DataInputStream dataIn) throws IOException {
        int count = dataIn.readUnsignedShort();
        TypeParamRecord[] result = new TypeParamRecord[count];
        for (int i = 0; i < count; i++) {
            TypeParamRecord tp = new TypeParamRecord();
            tp.typeVarAnnos = readAnnoIndices(dataIn);
            int boundCount = dataIn.readUnsignedShort();
            tp.boundAnnos = new TypeAnno[boundCount][];
            for (int b = 0; b < boundCount; b++) {
                tp.boundAnnos[b] = readTypeAnnos(dataIn);
            }
            result[i] = tp;
        }
        return result;
    }
}
