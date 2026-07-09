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
import java.util.List;

import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.util.ElementFilter;
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

    /**
     * Confirms a real record maps to {@code KIND_RECORD}, not {@code KIND_CLASS_OR_INTERFACE}.
     *
     * @throws Exception if the test compilation cannot be created
     */
    @Test
    public void recordMapsToRecordNotClassOrInterface() throws Exception {
        TypeElement recordElt = compileToTypeElement("SomeRecord", "record SomeRecord(int a) {}");
        Assert.assertEquals(
                BinaryStubData.ClassRecord.KIND_RECORD,
                BinaryStubReader.classRecordKind(recordElt.getKind()));
        Assert.assertNotEquals(
                BinaryStubData.ClassRecord.KIND_CLASS_OR_INTERFACE,
                BinaryStubReader.classRecordKind(recordElt.getKind()));
    }

    /**
     * Regression test for the root cause of a bug in {@code
     * BinaryStubReader.applyRecordComponents}: a record component's {@code RECORD_COMPONENT}-kind
     * element (returned by {@code TypeElement.getRecordComponents()}) is a distinct {@link Element}
     * from its compiler-generated backing {@code FIELD}-kind element (returned by {@code
     * ElementFilter.fieldsIn}), even though both share the same simple name. {@code
     * AnnotationFileParser.processRecordField} (the text parser) stores a component's annotated
     * type in {@code atypes} keyed by the {@code FIELD} element; {@code applyRecordComponents}
     * previously keyed it by the {@code RECORD_COMPONENT} element instead, which silently made a
     * component's type-use annotations unreachable through {@code
     * AnnotationFileElementTypes#getAnnotatedTypeMirror} (a bare {@code Element}-keyed lookup with
     * no name-based fallback). This test locks in the fact that motivated the fix, so a future
     * change cannot reintroduce the mismatch without also breaking this test.
     *
     * @throws Exception if the test compilation cannot be created
     */
    @Test
    public void recordComponentElementDiffersFromItsBackingField() throws Exception {
        TypeElement recordElt = compileToTypeElement("SomeRecord", "record SomeRecord(int a) {}");
        List<? extends Element> components = recordElt.getRecordComponents();
        Assert.assertEquals(1, components.size());
        Element componentElt = components.get(0);

        List<VariableElement> fields = ElementFilter.fieldsIn(recordElt.getEnclosedElements());
        Assert.assertEquals(1, fields.size());
        VariableElement fieldElt = fields.get(0);

        Assert.assertEquals(ElementKind.RECORD_COMPONENT, componentElt.getKind());
        Assert.assertEquals(ElementKind.FIELD, fieldElt.getKind());
        Assert.assertEquals(
                "the component and its backing field share the same simple name",
                componentElt.getSimpleName().toString(),
                fieldElt.getSimpleName().toString());
        Assert.assertNotEquals(
                "a record component's RECORD_COMPONENT element must NOT be treated as"
                        + " interchangeable with its backing FIELD element -- atypes must be keyed"
                        + " by the FIELD element, matching the text parser",
                componentElt,
                fieldElt);
    }
}
