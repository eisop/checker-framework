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
import java.util.Map;

import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeKind;
import javax.lang.model.util.ElementFilter;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.SimpleJavaFileObject;
import javax.tools.ToolProvider;

/**
 * Unit test for {@link AnnotationFileElementTypes#buildSigIndex}.
 *
 * <p>Regression test for a crash: {@code TypesUtils.simpleTypeName} throws {@link
 * org.checkerframework.javacutil.BugInCF} (a {@link RuntimeException}) for a parameter whose type
 * is {@link TypeKind#ERROR} -- which is exactly what happens for an unresolvable parameter type,
 * e.g. a JDK-internal type not exported to the annotation processor's module (this crashed {@code
 * checkNullness}/{@code checkInterning} on {@code sun.util.locale.provider.LocaleResources} while
 * annotating a real JDK class's method). {@code buildSigIndex} must catch that exception and skip
 * the method, not propagate it: a previous version of the surrounding catch block explicitly
 * rethrew {@code BugInCF} under the mistaken assumption that it always signals an unrelated
 * internal bug, which reintroduced the crash.
 *
 * <p>The JDK-internal trigger is specific to a particular JDK's module-export configuration and is
 * not reliably reproducible from ordinary test source; this test instead constructs a portable,
 * JDK-version-independent {@link TypeKind#ERROR} parameter directly, by compiling a method whose
 * parameter type does not exist. javac's error recovery still produces a real {@link
 * ExecutableElement} for such a method, with the unresolvable parameter's type attributed as {@link
 * TypeKind#ERROR}.
 */
public class AnnotationFileElementTypesTest {

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
     * TypeElement} for its first top-level class declaration. javac's error recovery attributes an
     * unresolvable referenced type as {@link TypeKind#ERROR} rather than failing to produce an
     * element, so this succeeds even for source that does not fully compile (e.g. a method
     * parameter of a nonexistent type).
     *
     * @param className the simple name of the class declared in {@code code}
     * @param code the source text to compile
     * @return the {@link TypeElement} for the first top-level class declared in {@code code}
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
     * Confirms the reproduction: a method parameter whose type is unresolvable is attributed as
     * {@link TypeKind#ERROR}, and {@code TypesUtils.simpleTypeName} (called by {@code
     * ElementUtils.getSimpleSignature}, which {@code buildSigIndex} uses) throws for it. This
     * documents why {@code buildSigIndex} must catch a {@link RuntimeException}, not just skip some
     * narrower exception type.
     *
     * @throws Exception if the test compilation cannot be created
     */
    @Test
    public void unresolvableParameterTypeIsErrorKind() throws Exception {
        TypeElement typeElt =
                compileToTypeElement(
                        "HasUnresolvableParam",
                        "class HasUnresolvableParam {" + " void m(NoSuchType x) {} }");
        List<ExecutableElement> methods = ElementFilter.methodsIn(typeElt.getEnclosedElements());
        Assert.assertEquals(1, methods.size());
        List<? extends VariableElement> params = methods.get(0).getParameters();
        Assert.assertEquals(1, params.size());
        Assert.assertEquals(TypeKind.ERROR, params.get(0).asType().getKind());
    }

    /**
     * The actual regression test: {@code buildSigIndex} must not throw when a method has an
     * unresolvable (ERROR-kind) parameter type; it must skip that method (it cannot be matched by
     * any stub record anyway) and still index the other, resolvable methods of the class.
     *
     * @throws Exception if the test compilation cannot be created
     */
    @Test
    public void buildSigIndexSkipsUnresolvableParameterType() throws Exception {
        TypeElement typeElt =
                compileToTypeElement(
                        "HasUnresolvableParam",
                        "class HasUnresolvableParam {"
                                + " void bad(NoSuchType x) {}"
                                + " void good(int x) {} }");
        List<ExecutableElement> methods = ElementFilter.methodsIn(typeElt.getEnclosedElements());
        Assert.assertEquals(2, methods.size());

        Map<String, ExecutableElement> index = AnnotationFileElementTypes.buildSigIndex(methods);

        Assert.assertEquals("the resolvable method must still be indexed", 1, index.size());
        Assert.assertTrue(index.containsKey("good(int)"));
        Assert.assertFalse(
                "the method with the unresolvable parameter must be skipped, not indexed",
                index.containsKey("bad(NoSuchType)"));
    }

    @Test
    public void testExtractFqClassName() {
        // Standard layout
        Assert.assertEquals(
                "java.lang.System",
                AnnotationFileElementTypes.extractFqClassName(
                        "annotated-jdk/src/java.base/share/classes/java/lang/System.java"));
        // Non-standard layout (no share/classes, relative to root)
        Assert.assertEquals(
                "java.lang.System",
                AnnotationFileElementTypes.extractFqClassName("java/lang/System.java"));
        // Non-standard layout with annotated-jdk prefix
        Assert.assertEquals(
                "java.lang.System",
                AnnotationFileElementTypes.extractFqClassName(
                        "annotated-jdk/java/lang/System.java"));
        // Modular non-standard layout
        Assert.assertEquals(
                "java.lang.System",
                AnnotationFileElementTypes.extractFqClassName(
                        "annotated-jdk/java.base/java/lang/System.java"));
        // package-info standard layout
        Assert.assertEquals(
                "java.lang.package-info",
                AnnotationFileElementTypes.extractFqClassName(
                        "annotated-jdk/src/java.base/share/classes/java/lang/package-info.java"));
        // package-info non-standard layout
        Assert.assertEquals(
                "java.lang.package-info",
                AnnotationFileElementTypes.extractFqClassName(
                        "annotated-jdk/java/lang/package-info.java"));
        // Unknown prefix
        Assert.assertNull(AnnotationFileElementTypes.extractFqClassName("foo/bar/Baz.java"));
        // Not a java file
        Assert.assertNull(AnnotationFileElementTypes.extractFqClassName("java/lang/System.class"));
    }
}
