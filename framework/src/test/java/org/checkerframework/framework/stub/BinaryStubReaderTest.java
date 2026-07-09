package org.checkerframework.framework.stub;

import com.sun.source.tree.ClassTree;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.util.JavacTask;
import com.sun.source.util.Trees;

import org.junit.Assert;
import org.junit.Test;

import java.io.StringWriter;
import java.net.URI;
import java.util.Collections;

import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.SimpleJavaFileObject;
import javax.tools.ToolProvider;

/**
 * Unit test for {@link BinaryStubReader#classRecordKind}.
 *
 * <p>Regression test for using the annotated JDK across JDK versions whose API can drift out from
 * under a fixed stub source: {@code java.nio.ByteOrder} became a real enum in JDK 26 after being a
 * plain class through JDK 25, while the annotated JDK's own {@code ByteOrder.java} stub still
 * declares it as a class. {@code AnnotationFileParser.processTypeDecl} has an existing, unrelated
 * defensive check for exactly this kind of drift on the text side (it warns and skips the whole
 * type when the real element's kind does not match the stub declaration's kind); {@code
 * classRecordKind} is the corresponding building block for the binary side, mapping a real {@code
 * TypeElement}'s kind to the {@code BinaryStubData.ClassRecord.KIND_*} constant it must match.
 */
public class BinaryStubReaderTest {

    /**
     * Returns an in-memory Java source file.
     *
     * @param className the simple name of the class, used only to form the file URI
     * @param code the source text of the file
     * @return a {@link JavaFileObject} that yields {@code code} as its content
     */
    private static JavaFileObject source(String className, String code) {
        return new SimpleJavaFileObject(
                URI.create("string:///" + className + ".java"), JavaFileObject.Kind.SOURCE) {
            @Override
            public CharSequence getCharContent(boolean ignoreEncodingErrors) {
                return code;
            }
        };
    }

    /**
     * Compiles {@code code} (with attribution, but no annotation processing) and returns the {@link
     * TypeElement} for its first top-level type declaration.
     *
     * @param className the simple name of the type declared in {@code code}
     * @param code the source text to compile
     * @return the {@link TypeElement} for the first top-level type declared in {@code code}
     * @throws Exception if the compiler task cannot be created
     */
    private static TypeElement compileToTypeElement(String className, String code)
            throws Exception {
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        JavaCompiler.CompilationTask compilationTask =
                compiler.getTask(
                        new StringWriter(),
                        null,
                        null,
                        Collections.singletonList("-proc:none"),
                        null,
                        Collections.singletonList(source(className, code)));
        JavacTask task = (JavacTask) compilationTask;
        CompilationUnitTree cu = task.parse().iterator().next();
        task.analyze();
        Trees trees = Trees.instance(task);
        ClassTree classTree = (ClassTree) cu.getTypeDecls().get(0);
        Element classElt = trees.getElement(trees.getPath(cu, classTree));
        return (TypeElement) classElt;
    }

    /**
     * Confirms a real class and a real interface both map to {@code KIND_CLASS_OR_INTERFACE}, and
     * so are treated as interchangeable with each other but not with an enum or annotation type.
     *
     * @throws Exception if a test compilation cannot be created
     */
    @Test
    public void classAndInterfaceMapToClassOrInterface() throws Exception {
        TypeElement classElt = compileToTypeElement("SomeClass", "class SomeClass {}");
        TypeElement interfaceElt =
                compileToTypeElement("SomeInterface", "interface SomeInterface {}");
        Assert.assertEquals(
                BinaryStubData.ClassRecord.KIND_CLASS_OR_INTERFACE,
                BinaryStubReader.classRecordKind(classElt.getKind()));
        Assert.assertEquals(
                BinaryStubData.ClassRecord.KIND_CLASS_OR_INTERFACE,
                BinaryStubReader.classRecordKind(interfaceElt.getKind()));
    }

    /**
     * Confirms a real enum maps to {@code KIND_ENUM}, not {@code KIND_CLASS_OR_INTERFACE} --
     * exactly the distinction that a stub still declaring {@code java.nio.ByteOrder} as a class
     * must trip on JDK 26, where the real type is now an enum.
     *
     * @throws Exception if the test compilation cannot be created
     */
    @Test
    public void enumMapsToEnumNotClassOrInterface() throws Exception {
        TypeElement enumElt = compileToTypeElement("SomeEnum", "enum SomeEnum { A, B }");
        Assert.assertEquals(
                BinaryStubData.ClassRecord.KIND_ENUM,
                BinaryStubReader.classRecordKind(enumElt.getKind()));
        Assert.assertNotEquals(
                BinaryStubData.ClassRecord.KIND_CLASS_OR_INTERFACE,
                BinaryStubReader.classRecordKind(enumElt.getKind()));
    }

    /**
     * Confirms a real annotation type maps to {@code KIND_ANNOTATION_TYPE}, not {@code
     * KIND_CLASS_OR_INTERFACE}.
     *
     * @throws Exception if the test compilation cannot be created
     */
    @Test
    public void annotationTypeMapsToAnnotationType() throws Exception {
        TypeElement annoElt = compileToTypeElement("SomeAnno", "@interface SomeAnno {}");
        Assert.assertEquals(
                BinaryStubData.ClassRecord.KIND_ANNOTATION_TYPE,
                BinaryStubReader.classRecordKind(annoElt.getKind()));
        Assert.assertNotEquals(
                BinaryStubData.ClassRecord.KIND_CLASS_OR_INTERFACE,
                BinaryStubReader.classRecordKind(annoElt.getKind()));
    }
}
