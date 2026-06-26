package org.checkerframework.framework.stubifier;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.ImportDeclaration;
import com.github.javaparser.ast.body.BodyDeclaration;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.ConstructorDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.AnnotationExpr;
import com.github.javaparser.ast.type.ArrayType;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.ast.type.PrimitiveType;
import com.github.javaparser.ast.type.Type;
import com.github.javaparser.ast.type.WildcardType;
import com.github.javaparser.ast.visitor.SimpleVoidVisitor;

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
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPOutputStream;

/**
 * Utility to write parsed Java compilation units into a compressed binary stub format.
 *
 * <p>This extracts relevant annotations structurally from declarations, fields, and methods, and
 * writes them into a dense binary format optimized for rapid loading without parsing overhead at
 * compile time.
 */
public class BinaryStubWriter {
    /** Magic number identifying the Checker Framework binary stub format. */
    public static final int MAGIC = 0xCF575542;

    /**
     * Format version of the binary stub file. Must match {@code
     * org.checkerframework.framework.stub.BinaryStubData#VERSION}.
     */
    public static final short VERSION = 1;

    /** Constant pool for strings to minimize binary size. */
    private static class ConstantPool {
        /** Map from string content to its constant-pool index. */
        private final Map<String, Integer> stringToIndex = new LinkedHashMap<>();

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
            AnnotationExpr qualified = writer.qualifyAnnotation(anno, cu);
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
        /** The kind of path step (array component, wildcard bound, type argument, nested type). */
        final byte kind;

        /** The type argument index, or 0 if not applicable. */
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
                if (step.kind == 3) { // TYPE_ARGUMENT
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

    /** Represents the annotations and members of a single class. */
    private static class ClassRecord {
        /** Index of the fully-qualified class name in the constant pool. */
        int nameIndex;

        /**
         * Index of the outermost enclosing class name in the constant pool, or 0 if this is a
         * top-level class (i.e., {@code nameIndex} itself is the outermost).
         */
        int outerNameIndex;

        /** Annotation-pool indices of the declaration annotations on this class. */
        List<Integer> declAnnos = new ArrayList<>();

        /** Records for all annotated fields of this class. */
        List<FieldRecord> fields = new ArrayList<>();

        /** Records for all annotated methods of this class. */
        List<MethodRecord> methods = new ArrayList<>();

        /**
         * Constructs a ClassRecord.
         *
         * @param nameIndex index of the class name in the constant pool
         * @param outerNameIndex index of the outermost enclosing class name, or 0 if top-level
         */
        ClassRecord(int nameIndex, int outerNameIndex) {
            this.nameIndex = nameIndex;
            this.outerNameIndex = outerNameIndex;
        }
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
     * Processes a single compilation unit, extracting annotations for its classes, methods, and
     * fields.
     *
     * @param cu the compilation unit to process
     */
    public void process(CompilationUnit cu) {
        simpleToFqn.clear();
        simpleToFqn.put("SuppressWarnings", "java.lang.SuppressWarnings");
        simpleToFqn.put("Deprecated", "java.lang.Deprecated");
        simpleToFqn.put("Override", "java.lang.Override");
        simpleToFqn.put("Documented", "java.lang.annotation.Documented");
        simpleToFqn.put("Retention", "java.lang.annotation.Retention");
        simpleToFqn.put("Target", "java.lang.annotation.Target");

        for (ImportDeclaration imp : cu.getImports()) {
            if (!imp.isAsterisk() && !imp.isStatic()) {
                String fqn = imp.getNameAsString();
                String simple = fqn.substring(fqn.lastIndexOf('.') + 1);
                simpleToFqn.put(simple, fqn);
            }
        }

        cu.getPackageDeclaration()
                .ifPresent(
                        pkg -> {
                            try {
                                String pkgName = pkg.getNameAsString();
                                pool.addString(pkgName);
                                List<Integer> annos = new ArrayList<>();
                                for (AnnotationExpr anno : pkg.getAnnotations()) {
                                    annos.add(annosPool.addAnnotation(anno, cu, this));
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
                                    annos.add(annosPool.addAnnotation(anno, cu, this));
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
            if (typeDecl instanceof ClassOrInterfaceDeclaration) {
                processClass((ClassOrInterfaceDeclaration) typeDecl, pkg, cu);
            }
        }
    }

    /** Helper to fully qualify class and annotation names inside an AnnotationExpr. */
    private AnnotationExpr qualifyAnnotation(AnnotationExpr anno, CompilationUnit cu) {
        AnnotationExpr copy = anno.clone();
        copy.accept(
                new com.github.javaparser.ast.visitor.ModifierVisitor<Void>() {
                    @Override
                    public com.github.javaparser.ast.visitor.Visitable visit(
                            com.github.javaparser.ast.expr.ClassExpr n, Void arg) {
                        n.setType(fullyQualify(n.getType(), cu));
                        return super.visit(n, arg);
                    }

                    @Override
                    public com.github.javaparser.ast.visitor.Visitable visit(
                            com.github.javaparser.ast.expr.MarkerAnnotationExpr n, Void arg) {
                        n.setName(fullyQualify(n.getNameAsString(), cu));
                        return super.visit(n, arg);
                    }

                    @Override
                    public com.github.javaparser.ast.visitor.Visitable visit(
                            com.github.javaparser.ast.expr.NormalAnnotationExpr n, Void arg) {
                        n.setName(fullyQualify(n.getNameAsString(), cu));
                        return super.visit(n, arg);
                    }

                    @Override
                    public com.github.javaparser.ast.visitor.Visitable visit(
                            com.github.javaparser.ast.expr.SingleMemberAnnotationExpr n, Void arg) {
                        n.setName(fullyQualify(n.getNameAsString(), cu));
                        return super.visit(n, arg);
                    }

                    @Override
                    public com.github.javaparser.ast.visitor.Visitable visit(
                            com.github.javaparser.ast.expr.FieldAccessExpr n, Void arg) {
                        if (n.getScope() instanceof com.github.javaparser.ast.expr.NameExpr) {
                            com.github.javaparser.ast.expr.NameExpr scope =
                                    (com.github.javaparser.ast.expr.NameExpr) n.getScope();
                            scope.setName(fullyQualify(scope.getNameAsString(), cu));
                        }
                        return super.visit(n, arg);
                    }
                },
                null);
        return copy;
    }

    /** Serializes a single annotation value expression structurally to the stream. */
    private void writeValue(
            DataOutputStream out,
            com.github.javaparser.ast.expr.Expression expr,
            CompilationUnit cu)
            throws IOException {
        if (expr instanceof com.github.javaparser.ast.expr.BooleanLiteralExpr) {
            out.writeByte('Z');
            out.writeBoolean(((com.github.javaparser.ast.expr.BooleanLiteralExpr) expr).getValue());
        } else if (expr instanceof com.github.javaparser.ast.expr.CharLiteralExpr) {
            out.writeByte('C');
            out.writeChar(((com.github.javaparser.ast.expr.CharLiteralExpr) expr).asChar());
        } else if (expr instanceof com.github.javaparser.ast.expr.IntegerLiteralExpr) {
            out.writeByte('J');
            out.writeLong(
                    ((com.github.javaparser.ast.expr.IntegerLiteralExpr) expr)
                            .asNumber()
                            .longValue());
        } else if (expr instanceof com.github.javaparser.ast.expr.LongLiteralExpr) {
            out.writeByte('J');
            out.writeLong(
                    ((com.github.javaparser.ast.expr.LongLiteralExpr) expr).asNumber().longValue());
        } else if (expr instanceof com.github.javaparser.ast.expr.DoubleLiteralExpr) {
            out.writeByte('D');
            out.writeDouble(((com.github.javaparser.ast.expr.DoubleLiteralExpr) expr).asDouble());
        } else if (expr instanceof com.github.javaparser.ast.expr.UnaryExpr) {
            com.github.javaparser.ast.expr.UnaryExpr ue =
                    (com.github.javaparser.ast.expr.UnaryExpr) expr;
            if (ue.getOperator() == com.github.javaparser.ast.expr.UnaryExpr.Operator.MINUS) {
                com.github.javaparser.ast.expr.Expression inner = ue.getExpression();
                if (inner instanceof com.github.javaparser.ast.expr.IntegerLiteralExpr) {
                    out.writeByte('J');
                    out.writeLong(
                            -((com.github.javaparser.ast.expr.IntegerLiteralExpr) inner)
                                    .asNumber()
                                    .longValue());
                } else if (inner instanceof com.github.javaparser.ast.expr.LongLiteralExpr) {
                    out.writeByte('J');
                    out.writeLong(
                            -((com.github.javaparser.ast.expr.LongLiteralExpr) inner)
                                    .asNumber()
                                    .longValue());
                } else if (inner instanceof com.github.javaparser.ast.expr.DoubleLiteralExpr) {
                    out.writeByte('D');
                    out.writeDouble(
                            -((com.github.javaparser.ast.expr.DoubleLiteralExpr) inner).asDouble());
                } else {
                    throw new IOException("Unsupported unary operator expression: " + expr);
                }
            } else {
                throw new IOException("Unsupported unary operator: " + ue.getOperator());
            }
        } else if (expr instanceof com.github.javaparser.ast.expr.StringLiteralExpr) {
            out.writeByte('s');
            out.writeInt(
                    pool.addString(
                            ((com.github.javaparser.ast.expr.StringLiteralExpr) expr).getValue()));
        } else if (expr instanceof com.github.javaparser.ast.expr.ClassExpr) {
            out.writeByte('c');
            Type type = ((com.github.javaparser.ast.expr.ClassExpr) expr).getType();
            out.writeInt(pool.addString(fullyQualify(type, cu).toString()));
        } else if (expr instanceof com.github.javaparser.ast.expr.FieldAccessExpr) {
            out.writeByte('e');
            com.github.javaparser.ast.expr.FieldAccessExpr fae =
                    (com.github.javaparser.ast.expr.FieldAccessExpr) expr;
            out.writeInt(pool.addString(fullyQualify(fae.getScope().toString(), cu)));
            out.writeInt(pool.addString(fae.getNameAsString()));
        } else if (expr instanceof AnnotationExpr) {
            out.writeByte('@');
            writeAnnotationInline(out, (AnnotationExpr) expr, cu);
        } else if (expr instanceof com.github.javaparser.ast.expr.ArrayInitializerExpr) {
            out.writeByte('[');
            List<com.github.javaparser.ast.expr.Expression> vals =
                    ((com.github.javaparser.ast.expr.ArrayInitializerExpr) expr).getValues();
            out.writeShort(vals.size());
            for (com.github.javaparser.ast.expr.Expression val : vals) {
                writeValue(out, val, cu);
            }
        } else if (expr instanceof com.github.javaparser.ast.expr.NameExpr) {
            out.writeByte('n');
            out.writeInt(
                    pool.addString(
                            ((com.github.javaparser.ast.expr.NameExpr) expr).getNameAsString()));
        } else if (expr instanceof com.github.javaparser.ast.expr.BinaryExpr) {
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
    private String evaluateStringLiteralConcatenation(
            com.github.javaparser.ast.expr.Expression expr) throws IOException {
        if (expr instanceof com.github.javaparser.ast.expr.StringLiteralExpr) {
            return ((com.github.javaparser.ast.expr.StringLiteralExpr) expr).getValue();
        } else if (expr instanceof com.github.javaparser.ast.expr.BinaryExpr) {
            com.github.javaparser.ast.expr.BinaryExpr be =
                    (com.github.javaparser.ast.expr.BinaryExpr) expr;
            if (be.getOperator() == com.github.javaparser.ast.expr.BinaryExpr.Operator.PLUS) {
                return evaluateStringLiteralConcatenation(be.getLeft())
                        + evaluateStringLiteralConcatenation(be.getRight());
            }
        }
        throw new IOException("Cannot evaluate string concatenation for expression: " + expr);
    }

    /** Writes an AnnotationExpr inline (for nested annotations). */
    private void writeAnnotationInline(
            DataOutputStream out, AnnotationExpr anno, CompilationUnit cu) throws IOException {
        AnnotationExpr qualified = qualifyAnnotation(anno, cu);
        out.writeInt(pool.addString(qualified.getNameAsString()));
        if (qualified instanceof com.github.javaparser.ast.expr.MarkerAnnotationExpr) {
            out.writeShort(0);
        } else if (qualified instanceof com.github.javaparser.ast.expr.SingleMemberAnnotationExpr) {
            out.writeShort(1);
            out.writeInt(pool.addString("value"));
            writeValue(
                    out,
                    ((com.github.javaparser.ast.expr.SingleMemberAnnotationExpr) qualified)
                            .getMemberValue(),
                    cu);
        } else if (qualified instanceof com.github.javaparser.ast.expr.NormalAnnotationExpr) {
            List<com.github.javaparser.ast.expr.MemberValuePair> pairs =
                    ((com.github.javaparser.ast.expr.NormalAnnotationExpr) qualified).getPairs();
            out.writeShort(pairs.size());
            for (com.github.javaparser.ast.expr.MemberValuePair pair : pairs) {
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
                    return com.github.javaparser.StaticJavaParser.parseType(fq);
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
        if (simpleToFqn.containsKey(name)) {
            return simpleToFqn.get(name);
        }
        for (com.github.javaparser.ast.ImportDeclaration imp : cu.getImports()) {
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
        return name;
    }

    /**
     * Processes a class or interface declaration, extracting its annotations and members.
     *
     * @param typeDecl the class or interface declaration to process
     * @param enclosingFqn the fully-qualified name of the enclosing class, or the package name for
     *     top-level classes
     * @param cu the compilation unit
     */
    private void processClass(
            ClassOrInterfaceDeclaration typeDecl, String enclosingFqn, CompilationUnit cu) {
        processClass(typeDecl, enclosingFqn, "", cu);
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
        if (hasComplexAnnos(typeDecl)) {
            return;
        }
        String fqn =
                enclosingFqn.isEmpty()
                        ? typeDecl.getNameAsString()
                        : enclosingFqn + "." + typeDecl.getNameAsString();
        // For inner classes, outermost is the top-level class; for top-level classes it's empty.
        String myOutermost = outermostFqn.isEmpty() ? fqn : outermostFqn;
        int outerNameIndex = outermostFqn.isEmpty() ? 0 : pool.addString(outermostFqn);
        ClassRecord cr = new ClassRecord(pool.addString(fqn), outerNameIndex);
        classes.add(cr);

        try {
            for (AnnotationExpr anno : typeDecl.getAnnotations()) {
                cr.declAnnos.add(annosPool.addAnnotation(anno, cu, this));
            }
        } catch (IOException e) {
            throw new RuntimeException(
                    "Serialization failure in class annotations: " + e.getMessage(), e);
        }

        for (BodyDeclaration<?> m : typeDecl.getMembers()) {
            if (m instanceof MethodDeclaration) {
                processMethod((MethodDeclaration) m, cr, cu);
            } else if (m instanceof ConstructorDeclaration) {
                processConstructor((ConstructorDeclaration) m, cr, cu);
            } else if (m instanceof FieldDeclaration) {
                processField((FieldDeclaration) m, cr, cu);
            } else if (m instanceof ClassOrInterfaceDeclaration) {
                processClass((ClassOrInterfaceDeclaration) m, fqn, myOutermost, cu);
            }
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
        try {
            MethodRecord mr = new MethodRecord();
            mr.sigIndex = pool.addString(MethodSignaturePrinter.toString(md));
            for (AnnotationExpr anno : md.getAnnotations()) {
                int idx = annosPool.addAnnotation(anno, cu, this);
                if (hasTypeUse(anno, cu)) {
                    // Annotation in declaration position annotates the element type of an array
                    // return (if any), not the array reference. Build the correct type path.
                    mr.returnTypeAnnos.add(new TypeAnno(idx, arrayElementPath(md.getType())));
                }
                if (!isTypeUseOnly(anno, cu)) {
                    // Annotation has a declaration-position target: also a declaration annotation.
                    mr.declAnnos.add(idx);
                }
            }
            extractTypeAnnotations(md.getType(), new ArrayList<>(), mr.returnTypeAnnos, cu);

            if (md.getReceiverParameter().isPresent()) {
                com.github.javaparser.ast.body.ReceiverParameter rp =
                        md.getReceiverParameter().get();
                for (AnnotationExpr anno : rp.getAnnotations()) {
                    mr.receiverAnnos.add(new TypeAnno(annosPool.addAnnotation(anno, cu, this)));
                }
                extractTypeAnnotations(rp.getType(), new ArrayList<>(), mr.receiverAnnos, cu);
            }

            for (Parameter p : md.getParameters()) {
                List<TypeAnno> pAnnos = new ArrayList<>();
                extractTypeAnnotations(p.getType(), new ArrayList<>(), pAnnos, cu);
                mr.paramAnnos.add(pAnnos);
                List<Integer> pdAnnos = new ArrayList<>();
                for (AnnotationExpr anno : p.getAnnotations()) {
                    pdAnnos.add(annosPool.addAnnotation(anno, cu, this));
                }
                mr.paramDeclAnnos.add(pdAnnos);
            }
            cr.methods.add(mr);
        } catch (IOException e) {
            throw new RuntimeException("Serialization failure in method: " + e.getMessage(), e);
        }
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
        try {
            MethodRecord mr = new MethodRecord();
            mr.sigIndex = pool.addString(MethodSignaturePrinter.toString(cd));
            for (AnnotationExpr anno : cd.getAnnotations()) {
                int idx = annosPool.addAnnotation(anno, cu, this);
                if (hasTypeUse(anno, cu)) {
                    // Annotation has TYPE_USE in its target: describes the constructed object.
                    mr.returnTypeAnnos.add(new TypeAnno(idx));
                }
                if (!isTypeUseOnly(anno, cu)) {
                    // Annotation has a declaration-position target: also a declaration annotation.
                    mr.declAnnos.add(idx);
                }
            }

            if (cd.getReceiverParameter().isPresent()) {
                com.github.javaparser.ast.body.ReceiverParameter rp =
                        cd.getReceiverParameter().get();
                for (AnnotationExpr anno : rp.getAnnotations()) {
                    mr.receiverAnnos.add(new TypeAnno(annosPool.addAnnotation(anno, cu, this)));
                }
                extractTypeAnnotations(rp.getType(), new ArrayList<>(), mr.receiverAnnos, cu);
            }

            for (Parameter p : cd.getParameters()) {
                List<TypeAnno> pAnnos = new ArrayList<>();
                extractTypeAnnotations(p.getType(), new ArrayList<>(), pAnnos, cu);
                mr.paramAnnos.add(pAnnos);
                List<Integer> pdAnnos = new ArrayList<>();
                for (AnnotationExpr anno : p.getAnnotations()) {
                    pdAnnos.add(annosPool.addAnnotation(anno, cu, this));
                }
                mr.paramDeclAnnos.add(pdAnnos);
            }
            cr.methods.add(mr);
        } catch (IOException e) {
            throw new RuntimeException(
                    "Serialization failure in constructor: " + e.getMessage(), e);
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
        try {
            for (VariableDeclarator vd : fd.getVariables()) {
                FieldRecord fr = new FieldRecord();
                fr.nameIndex = pool.addString(vd.getNameAsString());
                for (AnnotationExpr anno : fd.getAnnotations()) {
                    int idx = annosPool.addAnnotation(anno, cu, this);
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
            result.add(new TypeAnno(annosPool.addAnnotation(ann, cu, this), currentPath));
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
                currentPath.add(new TypePathStep((byte) 2, (byte) 0)); // WILDCARD_BOUND
                extractTypeAnnotations(wt.getExtendedType().get(), currentPath, result, cu);
                currentPath.remove(currentPath.size() - 1);
            }
            if (wt.getSuperType().isPresent()) {
                currentPath.add(new TypePathStep((byte) 2, (byte) 0)); // WILDCARD_BOUND
                extractTypeAnnotations(wt.getSuperType().get(), currentPath, result, cu);
                currentPath.remove(currentPath.size() - 1);
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
                out.writeShort(cr.declAnnos.size());
                for (int annoIdx : cr.declAnnos) out.writeInt(annoIdx);

                out.writeShort(cr.fields.size());
                for (FieldRecord fr : cr.fields) {
                    out.writeInt(fr.nameIndex);
                    out.writeShort(fr.declAnnos.size());
                    for (int annoIdx : fr.declAnnos) out.writeInt(annoIdx);
                    out.writeShort(fr.typeAnnos.size());
                    for (TypeAnno ta : fr.typeAnnos) ta.write(out);
                }

                out.writeShort(cr.methods.size());
                for (MethodRecord mr : cr.methods) {
                    out.writeInt(mr.sigIndex);
                    out.writeShort(mr.declAnnos.size());
                    for (int annoIdx : mr.declAnnos) out.writeInt(annoIdx);

                    out.writeShort(mr.returnTypeAnnos.size());
                    for (TypeAnno ta : mr.returnTypeAnnos) ta.write(out);

                    out.writeShort(mr.receiverAnnos.size());
                    for (TypeAnno ta : mr.receiverAnnos) ta.write(out);

                    out.writeShort(mr.paramAnnos.size());
                    for (int p = 0; p < mr.paramAnnos.size(); p++) {
                        List<TypeAnno> ptAnnos = mr.paramAnnos.get(p);
                        out.writeShort(ptAnnos.size());
                        for (TypeAnno ta : ptAnnos) {
                            ta.write(out);
                        }
                        List<Integer> pdAnnos = mr.paramDeclAnnos.get(p);
                        out.writeShort(pdAnnos.size());
                        for (int idx : pdAnnos) {
                            out.writeInt(idx);
                        }
                    }
                }
            }

            out.writeInt(packages.size());
            for (Map.Entry<String, List<Integer>> entry : packages.entrySet()) {
                out.writeInt(pool.addString(entry.getKey()));
                out.writeShort(entry.getValue().size());
                for (int annoIdx : entry.getValue()) out.writeInt(annoIdx);
            }

            out.writeInt(modules.size());
            for (Map.Entry<String, List<Integer>> entry : modules.entrySet()) {
                out.writeInt(pool.addString(entry.getKey()));
                out.writeShort(entry.getValue().size());
                for (int annoIdx : entry.getValue()) out.writeInt(annoIdx);
            }
        }
    }

    /**
     * A visitor that prints the type-erased signature of a method or constructor. This ensures that
     * method signatures match the format expected by the binary stub reader.
     */
    private static class MethodSignaturePrinter extends SimpleVoidVisitor<Void> {
        /**
         * Returns the type-erased signature of a method declaration.
         *
         * @param md the method declaration
         * @return the type-erased signature
         */
        static String toString(MethodDeclaration md) {
            MethodSignaturePrinter printer = new MethodSignaturePrinter();
            md.accept(printer, null);
            return printer.getOutput();
        }

        /**
         * Returns the type-erased signature of a constructor declaration.
         *
         * @param cd the constructor declaration
         * @return the type-erased signature
         */
        static String toString(ConstructorDeclaration cd) {
            MethodSignaturePrinter printer = new MethodSignaturePrinter();
            cd.accept(printer, null);
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
            sb.append("(");
            if (n.getParameters() != null) {
                for (Iterator<Parameter> i = n.getParameters().iterator(); i.hasNext(); ) {
                    Parameter p = i.next();
                    p.accept(this, arg);
                    if (i.hasNext()) {
                        sb.append(",");
                    }
                }
            }
            sb.append(")");
        }

        @Override
        public void visit(MethodDeclaration n, Void arg) {
            sb.append(n.getName());
            sb.append("(");
            if (n.getParameters() != null) {
                for (Iterator<Parameter> i = n.getParameters().iterator(); i.hasNext(); ) {
                    Parameter p = i.next();
                    p.accept(this, arg);
                    if (i.hasNext()) {
                        sb.append(",");
                    }
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
     * Determines whether a class or interface declaration contains complex annotations (e.g.,
     * annotations on type parameters or bounds) that are not currently supported by the binary stub
     * format.
     *
     * @param typeDecl the class or interface declaration to check
     * @return {@code true} if complex annotations are found, {@code false} otherwise
     */
    private boolean hasComplexAnnos(ClassOrInterfaceDeclaration typeDecl) {
        for (com.github.javaparser.ast.type.TypeParameter tp : typeDecl.getTypeParameters()) {
            if (!tp.findAll(AnnotationExpr.class).isEmpty()) return true;
        }
        if (typeDecl.getExtendedTypes() != null) {
            for (com.github.javaparser.ast.type.ClassOrInterfaceType t :
                    typeDecl.getExtendedTypes()) {
                if (!t.findAll(AnnotationExpr.class).isEmpty()) return true;
            }
        }
        if (typeDecl.getImplementedTypes() != null) {
            for (com.github.javaparser.ast.type.ClassOrInterfaceType t :
                    typeDecl.getImplementedTypes()) {
                if (!t.findAll(AnnotationExpr.class).isEmpty()) return true;
            }
        }
        for (MethodDeclaration md : typeDecl.findAll(MethodDeclaration.class)) {
            for (com.github.javaparser.ast.type.TypeParameter tp : md.getTypeParameters()) {
                if (!tp.findAll(AnnotationExpr.class).isEmpty()) return true;
            }
        }
        return false;
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
        String fqn = fullyQualify(anno.getNameAsString(), cu);
        try {
            Class<?> cls = Class.forName(fqn);
            Target target = cls.getAnnotation(Target.class);
            if (target == null) {
                return false;
            }
            for (ElementType et : target.value()) {
                if (et == ElementType.TYPE_USE) {
                    return true;
                }
            }
            return false;
        } catch (ClassNotFoundException e) {
            return false;
        }
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
        String fqn = fullyQualify(anno.getNameAsString(), cu);
        try {
            Class<?> cls = Class.forName(fqn);
            Target target = cls.getAnnotation(Target.class);
            if (target == null) {
                return false;
            }
            for (ElementType et : target.value()) {
                if (et != ElementType.TYPE_USE && et != ElementType.TYPE_PARAMETER) {
                    return false;
                }
            }
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
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
