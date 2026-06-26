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
 * In-memory representation of a binary JDK stub file ({@code annotated-jdk.bin.gz}). The binary
 * format stores annotation information extracted from the annotated JDK source files in a compact,
 * pre-parsed form that is faster to load than the original {@code .java} source stubs.
 *
 * <p>The binary format consists of a GZIP-compressed stream containing:
 *
 * <ol>
 *   <li>A 4-byte magic number ({@code 0xCF575542}).
 *   <li>A 2-byte version number.
 *   <li>A constant pool of UTF-8 strings (class names, field names, signatures, string literals).
 *   <li>An annotation pool of structural annotation records.
 *   <li>A sequence of {@link ClassRecord} entries, one per annotated class or interface.
 * </ol>
 *
 * @see BinaryStubReader
 * @see org.checkerframework.framework.stubifier.BinaryStubWriter
 */
public class BinaryStubData {

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
         * Constructs an AnnotationRecord.
         *
         * @param nameIndex index into the string pool of the fully-qualified annotation class name
         * @param elementValues mapping from element member name (string pool index) to its
         *     structured value
         */
        public AnnotationRecord(int nameIndex, Map<Integer, Object> elementValues) {
            this.nameIndex = nameIndex;
            this.elementValues = elementValues;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof AnnotationRecord)) return false;
            AnnotationRecord that = (AnnotationRecord) o;
            return nameIndex == that.nameIndex && Objects.equals(elementValues, that.elementValues);
        }

        @Override
        public int hashCode() {
            return Objects.hash(nameIndex, elementValues);
        }
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
         * wildcard bound, {@code 3} = type argument.
         */
        public final byte kind;

        /**
         * For {@link #kind} {@code 3} (type argument), the zero-based index of the type argument.
         * Unused for other kinds.
         */
        public final byte argIndex;

        /**
         * Constructs a TypePathStep.
         *
         * @param kind the kind of path step: 0 = array component, 1 = nested type, 2 = wildcard
         *     bound, 3 = type argument
         * @param argIndex for kind 3, the zero-based index of the type argument; unused for other
         *     kinds
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

    /** Annotation data for a single method or constructor. */
    public static class MethodRecord {
        /** Index into {@link BinaryStubData#stringPool} of the method's simple signature. */
        public int sigIndex;

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
    }

    /** Annotation data for a single field. */
    public static class FieldRecord {
        /** Index into {@link BinaryStubData#stringPool} of the field's simple name. */
        public int nameIndex;

        /** Annotation-pool indices of the declaration annotations on this field. */
        public int[] declAnnos;

        /** Type annotations on the field's type. */
        public TypeAnno[] typeAnnos;
    }

    /** Annotation data for a single class or interface, including its members. */
    public static class ClassRecord {
        /**
         * Index into {@link BinaryStubData#stringPool} of the fully-qualified class name (using
         * {@code '.'} as separator, e.g. {@code "com.sun.tools.javac.code.Symbol.Completer"}).
         */
        public int nameIndex;

        /** Annotation-pool indices of the declaration annotations on this class. */
        public int[] declAnnos;

        /** Field records for the annotated fields of this class. */
        public FieldRecord[] fields;

        /** Method and constructor records for the annotated methods of this class. */
        public MethodRecord[] methods;
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
            if (dataIn.readInt() != 0xCF575542) {
                throw new IOException("Invalid magic number");
            }
            short version = dataIn.readShort();
            if (version != 3) {
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
                short elementCount = dataIn.readShort();
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

                int declAnnoCount = dataIn.readShort();
                cr.declAnnos = new int[declAnnoCount];
                for (int j = 0; j < declAnnoCount; j++) {
                    cr.declAnnos[j] = dataIn.readInt();
                }

                int fieldCount = dataIn.readShort();
                cr.fields = new FieldRecord[fieldCount];
                for (int j = 0; j < fieldCount; j++) {
                    FieldRecord fr = new FieldRecord();
                    fr.nameIndex = dataIn.readInt();

                    int fdAnnoCount = dataIn.readShort();
                    fr.declAnnos = new int[fdAnnoCount];
                    for (int k = 0; k < fdAnnoCount; k++) {
                        fr.declAnnos[k] = dataIn.readInt();
                    }

                    int typeAnnoCount = dataIn.readShort();
                    fr.typeAnnos = new TypeAnno[typeAnnoCount];
                    for (int k = 0; k < typeAnnoCount; k++) {
                        fr.typeAnnos[k] = readTypeAnno(dataIn);
                    }
                    cr.fields[j] = fr;
                }

                int methodCount = dataIn.readShort();
                cr.methods = new MethodRecord[methodCount];
                for (int j = 0; j < methodCount; j++) {
                    MethodRecord mr = new MethodRecord();
                    mr.sigIndex = dataIn.readInt();

                    int mdAnnoCount = dataIn.readShort();
                    mr.declAnnos = new int[mdAnnoCount];
                    for (int k = 0; k < mdAnnoCount; k++) {
                        mr.declAnnos[k] = dataIn.readInt();
                    }

                    int retAnnoCount = dataIn.readShort();
                    mr.returnTypeAnnos = new TypeAnno[retAnnoCount];
                    for (int k = 0; k < retAnnoCount; k++) {
                        mr.returnTypeAnnos[k] = readTypeAnno(dataIn);
                    }

                    int recAnnoCount = dataIn.readShort();
                    mr.receiverAnnos = new TypeAnno[recAnnoCount];
                    for (int k = 0; k < recAnnoCount; k++) {
                        mr.receiverAnnos[k] = readTypeAnno(dataIn);
                    }

                    int paramCount = dataIn.readShort();
                    mr.paramAnnos = new TypeAnno[paramCount][];
                    mr.paramDeclAnnos = new int[paramCount][];
                    for (int p = 0; p < paramCount; p++) {
                        int pTypeAnnoCount = dataIn.readShort();
                        mr.paramAnnos[p] = new TypeAnno[pTypeAnnoCount];
                        for (int k = 0; k < pTypeAnnoCount; k++) {
                            mr.paramAnnos[p][k] = readTypeAnno(dataIn);
                        }
                        int pDeclAnnoCount = dataIn.readShort();
                        mr.paramDeclAnnos[p] = new int[pDeclAnnoCount];
                        for (int k = 0; k < pDeclAnnoCount; k++) {
                            mr.paramDeclAnnos[p][k] = dataIn.readInt();
                        }
                    }
                    cr.methods[j] = mr;
                }
                classes.put(stringPool[cr.nameIndex], cr);
            }

            int packageCount = dataIn.readInt();
            for (int i = 0; i < packageCount; i++) {
                String pkgName = stringPool[dataIn.readInt()];
                int declAnnoCount = dataIn.readShort();
                int[] annos = new int[declAnnoCount];
                for (int j = 0; j < declAnnoCount; j++) {
                    annos[j] = dataIn.readInt();
                }
                packages.put(pkgName, annos);
            }

            int moduleCount = dataIn.readInt();
            for (int i = 0; i < moduleCount; i++) {
                String modName = stringPool[dataIn.readInt()];
                int declAnnoCount = dataIn.readShort();
                int[] annos = new int[declAnnoCount];
                for (int j = 0; j < declAnnoCount; j++) {
                    annos[j] = dataIn.readInt();
                }
                modules.put(modName, annos);
            }
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
                    short elementCount = dataIn.readShort();
                    Map<Integer, Object> elements = new HashMap<>();
                    for (int j = 0; j < elementCount; j++) {
                        int memberIdx = dataIn.readInt();
                        elements.put(memberIdx, readAnnotationValue(dataIn));
                    }
                    return new AnnotationRecord(nameIdx, elements);
                }
            case '[':
                {
                    int len = dataIn.readShort();
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
     * Reads a single {@link TypeAnno} from the stream.
     *
     * @param dataIn the stream to read from
     * @return the type annotation record
     * @throws IOException if the stream cannot be read
     */
    private TypeAnno readTypeAnno(DataInputStream dataIn) throws IOException {
        int annoIndex = dataIn.readInt();
        int pathLength = dataIn.readByte();
        TypePathStep[] path = new TypePathStep[pathLength];
        for (int i = 0; i < pathLength; i++) {
            byte kind = dataIn.readByte();
            byte argIndex = 0;
            if (kind == 3) { // TYPE_ARGUMENT
                argIndex = dataIn.readByte();
            }
            path[i] = new TypePathStep(kind, argIndex);
        }
        return new TypeAnno(annoIndex, path);
    }
}
