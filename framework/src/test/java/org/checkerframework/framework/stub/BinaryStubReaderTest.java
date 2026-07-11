package org.checkerframework.framework.stub;

import com.sun.source.tree.ClassTree;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.util.JavacTask;
import com.sun.source.util.Trees;
import com.sun.tools.javac.processing.JavacProcessingEnvironment;
import com.sun.tools.javac.util.Context;

import org.junit.Assert;
import org.junit.Test;

import java.io.StringWriter;
import java.net.URI;
import java.util.Collections;
import java.util.List;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeKind;
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
     * Confirms {@code ElementUtils.getModuleElement} (used by {@code
     * AnnotationFileParser#processModule} to resolve a stub-declared module name to a real element,
     * mirroring {@code processPackage}'s {@code elements.getPackageElement}) resolves a real module
     * name to a non-null element, and returns {@code null} for a nonexistent one. {@code
     * Elements.getModuleElement(CharSequence)} itself is JDK 9+ only, so this also exercises that
     * {@code ElementUtils}'s reflective wrapper works on the JDK actually running this test.
     *
     * @throws Exception if the test compilation cannot be created
     */
    @Test
    public void getModuleElementResolvesRealModule() throws Exception {
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        JavaCompiler.CompilationTask compilationTask =
                compiler.getTask(
                        new StringWriter(),
                        null,
                        null,
                        Collections.singletonList("-proc:none"),
                        null,
                        Collections.singletonList(source("Empty", "class Empty {}")));
        JavacTask task = (JavacTask) compilationTask;
        task.analyze();
        Element javaBase =
                org.checkerframework.javacutil.ElementUtils.getModuleElement(
                        task.getElements(), "java.base");
        Assert.assertNotNull("java.base must resolve to a real module element", javaBase);
        Element noSuchModule =
                org.checkerframework.javacutil.ElementUtils.getModuleElement(
                        task.getElements(), "no.such.module");
        Assert.assertNull(
                "a nonexistent module name must resolve to null, not throw", noSuchModule);
    }

    /**
     * Returns a {@link ProcessingEnvironment} backed by a fresh javac {@link Context}, without
     * compiling or attributing any source. {@code java.lang.Integer} and other JDK classes are
     * still resolvable through it via the default (system) classpath, exactly as {@link
     * StubGenerator#main} sets up its {@code ProcessingEnvironment} for the same reason. Avoids the
     * heavier {@code JavaCompiler.getTask(...).analyze()} dance used elsewhere in this file, since
     * no test source needs to be parsed.
     *
     * @return a usable {@link ProcessingEnvironment} with no associated compilation
     */
    private static ProcessingEnvironment freshProcessingEnvironment() {
        Context context = new Context();
        com.sun.tools.javac.main.JavaCompiler javac =
                com.sun.tools.javac.main.JavaCompiler.instance(context);
        javac.initModules(com.sun.tools.javac.util.List.nil());
        javac.enterDone();
        return JavacProcessingEnvironment.instance(context);
    }

    /**
     * Regression test for the root cause of a bug in {@link BinaryStubReader#resolveSingleValue}:
     * the writer tags every {@code FieldAccessExpr} annotation value with the same {@code 'e'} tag
     * (see {@code BinaryStubWriter#writeValue}), regardless of whether the referenced member is an
     * enum constant or a plain static final field, so the reader cannot tell which one it is from
     * the tag alone. {@code resolveSingleValue} used to only look for an {@code ENUM_CONSTANT}
     * member and silently drop the value otherwise, so a stub value like {@code IntRange(to =
     * Integer.MAX_VALUE)} lost the member entirely. {@link BinaryStubReader#findFieldInType} is the
     * fallback lookup the fix added for exactly this case; this test confirms it finds a real,
     * non-enum constant field and that {@link BinaryStubReader#coerceToKind} then narrows its value
     * to the annotation member's declared kind, matching what {@code
     * AnnotationFileParser.getValueOfExpressionInAnnotation} does for the text parser.
     */
    @Test
    public void findFieldInTypeResolvesANonEnumStaticConstant() {
        ProcessingEnvironment env = freshProcessingEnvironment();
        TypeElement integerElt = env.getElementUtils().getTypeElement("java.lang.Integer");
        Assert.assertNotNull("java.lang.Integer must resolve", integerElt);

        VariableElement maxValue = BinaryStubReader.findFieldInType(integerElt, "MAX_VALUE", env);
        Assert.assertNotNull(
                "MAX_VALUE is a plain static final field, not an enum constant, but"
                        + " findFieldInType must still find it",
                maxValue);
        Assert.assertEquals(ElementKind.FIELD, maxValue.getKind());
        Assert.assertEquals(Integer.MAX_VALUE, maxValue.getConstantValue());
        Assert.assertEquals(
                "the resolved constant must narrow to the annotation member's declared kind, the"
                        + " same way a NameLiteralValue constant does",
                Integer.MAX_VALUE,
                BinaryStubReader.coerceToKind(maxValue.getConstantValue(), TypeKind.INT));
    }

    /**
     * Confirms {@link BinaryStubReader#findFieldInType} does not match a real enum constant: {@code
     * TimeUnit.SECONDS} is an {@code ENUM_CONSTANT}, not a {@code FIELD}, so {@code
     * resolveSingleValue}'s existing {@code ENUM_CONSTANT} loop -- not the fallback this task added
     * -- is the one that must resolve it. This pins the two-step split in {@code
     * resolveSingleValue}: try the enum-constant loop first, and only fall back to {@code
     * findFieldInType} when that loop does not find the member.
     */
    @Test
    public void findFieldInTypeDoesNotMatchAnEnumConstant() {
        ProcessingEnvironment env = freshProcessingEnvironment();
        TypeElement timeUnitElt =
                env.getElementUtils().getTypeElement("java.util.concurrent.TimeUnit");
        Assert.assertNotNull("java.util.concurrent.TimeUnit must resolve", timeUnitElt);

        VariableElement secondsAsField =
                BinaryStubReader.findFieldInType(timeUnitElt, "SECONDS", env);
        Assert.assertNull(
                "SECONDS is an ENUM_CONSTANT, not a FIELD, so findFieldInType must not match it",
                secondsAsField);
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
     * Regression test for {@link BinaryStubReader#coerceToKind}: an integral literal written for a
     * floating-point annotation member must be converted to that member's kind.
     *
     * <p>The writer records every integral literal as a long, so {@code @DoubleExample(0)} and
     * {@code @DoubleExample(0L)} (both legal Java when {@code DoubleExample.value()} is a {@code
     * double}; see {@code checker/jtreg/stubs/issue1542/Stub.astub}) reach the reader as a {@code
     * Long}. Returning an {@code Integer} for them, as the reader used to, makes {@code
     * AnnotationBuilder.setValue} throw and the whole annotation get silently dropped. The text
     * parser's {@code AnnotationFileParser.convert} has always had the {@code FLOAT} and {@code
     * DOUBLE} cases.
     */
    @Test
    public void integralValueIsWidenedToFloatingPointMember() {
        Assert.assertEquals(0.0d, BinaryStubReader.coerceToKind(0L, TypeKind.DOUBLE));
        Assert.assertEquals(0.0f, BinaryStubReader.coerceToKind(0L, TypeKind.FLOAT));
        Assert.assertEquals(3.0d, BinaryStubReader.coerceToKind(3L, TypeKind.DOUBLE));
        Assert.assertEquals(3.0f, BinaryStubReader.coerceToKind(3L, TypeKind.FLOAT));
    }

    /**
     * Confirms {@link BinaryStubReader#coerceToKind} narrows and widens between every numeric kind,
     * and converts a character value for a numeric member through its code point, matching the text
     * parser's {@code convert((int) charValue, valueKind)}.
     */
    @Test
    public void coerceToKindConvertsBetweenNumericKinds() {
        Assert.assertEquals((short) 7, BinaryStubReader.coerceToKind(7L, TypeKind.SHORT));
        Assert.assertEquals(7, BinaryStubReader.coerceToKind(7L, TypeKind.INT));
        Assert.assertEquals(7L, BinaryStubReader.coerceToKind(7L, TypeKind.LONG));
        Assert.assertEquals('\7', BinaryStubReader.coerceToKind(7L, TypeKind.CHAR));

        // A double literal for a float member; the writer records both as a double.
        Assert.assertEquals(1.5f, BinaryStubReader.coerceToKind(1.5d, TypeKind.FLOAT));
        Assert.assertEquals(1.5d, BinaryStubReader.coerceToKind(1.5d, TypeKind.DOUBLE));

        // A character value for a numeric member goes through its code point.
        Assert.assertEquals(97, BinaryStubReader.coerceToKind('a', TypeKind.INT));
        Assert.assertEquals(97L, BinaryStubReader.coerceToKind('a', TypeKind.LONG));
        Assert.assertEquals(97.0d, BinaryStubReader.coerceToKind('a', TypeKind.DOUBLE));
        Assert.assertEquals('a', BinaryStubReader.coerceToKind('a', TypeKind.CHAR));
    }

    /**
     * Confirms {@link BinaryStubReader#coerceToKind} leaves a value alone when the member's kind is
     * not a numeric primitive -- in particular {@code TypeKind.NONE}, which {@code
     * addValueToBuilder} uses when the annotation member cannot be found at all.
     */
    @Test
    public void coerceToKindLeavesNonNumericKindsAlone() {
        Assert.assertEquals(7L, BinaryStubReader.coerceToKind(7L, TypeKind.NONE));
        Assert.assertEquals('a', BinaryStubReader.coerceToKind('a', TypeKind.NONE));
        Assert.assertEquals("s", BinaryStubReader.coerceToKind("s", TypeKind.DECLARED));
        Assert.assertEquals(true, BinaryStubReader.coerceToKind(true, TypeKind.BOOLEAN));
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
