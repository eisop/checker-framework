package org.checkerframework.framework.stub;

import com.sun.source.tree.ClassTree;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.util.JavacTask;
import com.sun.source.util.Trees;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.io.StringWriter;
import java.net.URI;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

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

    /**
     * {@code extractFqClassName} handles both forms of the annotated JDK's layout -- a jar entry,
     * which includes the {@code annotated-jdk} root, and a file path relative to that root -- and
     * returns null for anything that does not follow the layout, so that the caller skips it
     * instead of crashing on an unguarded {@code substring}.
     */
    @Test
    public void testExtractFqClassName() {
        Assert.assertEquals(
                "java.lang.System",
                AnnotationFileElementTypes.extractFqClassName(
                        "annotated-jdk/src/java.base/share/classes/java/lang/System.java"));
        Assert.assertEquals(
                "java.lang.System",
                AnnotationFileElementTypes.extractFqClassName(
                        "src/java.base/share/classes/java/lang/System.java"));
        Assert.assertEquals(
                "java.lang.package-info",
                AnnotationFileElementTypes.extractFqClassName(
                        "annotated-jdk/src/java.base/share/classes/java/lang/package-info.java"));
        // Not the annotated JDK's layout: no "/share/classes/" segment.
        Assert.assertNull(AnnotationFileElementTypes.extractFqClassName("java/lang/System.java"));
        // Not a Java file.
        Assert.assertNull(
                AnnotationFileElementTypes.extractFqClassName(
                        "annotated-jdk/src/java.base/share/classes/java/lang/System.class"));
    }

    /**
     * {@code extractModuleName} returns the module directory segment of a {@code module-info.java}
     * entry, for both forms of the layout, and null for anything else.
     */
    @Test
    public void testExtractModuleName() {
        Assert.assertEquals(
                "java.base",
                AnnotationFileElementTypes.extractModuleName(
                        "annotated-jdk/src/java.base/share/classes/module-info.java"));
        Assert.assertEquals(
                "java.base",
                AnnotationFileElementTypes.extractModuleName(
                        "src/java.base/share/classes/module-info.java"));
        // Not the annotated JDK's layout: no "src/" segment, and no "/share/classes/" segment.
        Assert.assertNull(
                AnnotationFileElementTypes.extractModuleName(
                        "annotated-jdk/java.base/module-info.java"));
    }

    /**
     * {@code findAnnotatedJdk} returns the annotated JDK in the jar or directory of the factory
     * class if it has one, even when a jar earlier on the class path has an annotated JDK too, and
     * otherwise the first one on the class path.
     */
    @Test
    public void testFindAnnotatedJdk() throws Exception {
        Path dir = Files.createTempDirectory("findAnnotatedJdk");
        Path classes = dir.resolve("classes");
        Files.createDirectories(classes);
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        Assert.assertEquals(
                0, compiler.run(null, null, null, "-d", classes.toString(), writeFactory(dir)));

        Path first = jar(dir.resolve("first.jar"), null);
        Path own = jar(dir.resolve("own.jar"), classes);
        Path plain = jar(dir.resolve("plain.jar"), classes, false);
        Path ownDir = dir.resolve("ownDir");
        Files.createDirectories(ownDir.resolve("annotated-jdk"));
        Files.copy(classes.resolve("Factory.class"), ownDir.resolve("Factory.class"));

        // The jar that holds the factory class has an annotated JDK: it wins over the first one.
        Assert.assertTrue(findIn(first, own).contains("own.jar!/annotated-jdk"));
        // The directory that holds the factory class has one.
        Assert.assertTrue(findIn(first, ownDir).contains("ownDir/annotated-jdk"));
        // The jar that holds the factory class has none: the first one on the class path is used.
        Assert.assertTrue(findIn(first, plain).contains("first.jar!/annotated-jdk"));
    }

    /**
     * Writes the source of a class named {@code Factory} into {@code dir}.
     *
     * @param dir a directory
     * @return the path of the source file, as a string
     * @throws IOException if the file cannot be written
     */
    private static String writeFactory(Path dir) throws IOException {
        Path src = dir.resolve("Factory.java");
        Files.write(src, "public class Factory {}".getBytes(StandardCharsets.UTF_8));
        return src.toString();
    }

    /**
     * Writes a jar with an {@code annotated-jdk/} directory entry and, if {@code classes} is
     * non-null, the {@code Factory.class} in it.
     *
     * @param jar the jar to write
     * @param classes a directory with {@code Factory.class}, or null
     * @return {@code jar}
     * @throws IOException if the jar cannot be written
     */
    private static Path jar(Path jar, @Nullable Path classes) throws IOException {
        return jar(jar, classes, true);
    }

    /**
     * Writes a jar with {@code Factory.class} if {@code classes} is non-null, and with an {@code
     * annotated-jdk/} directory entry if {@code withJdk}.
     *
     * @param jar the jar to write
     * @param classes a directory with {@code Factory.class}, or null
     * @param withJdk whether the jar gets an annotated JDK
     * @return {@code jar}
     * @throws IOException if the jar cannot be written
     */
    private static Path jar(Path jar, @Nullable Path classes, boolean withJdk) throws IOException {
        try (JarOutputStream out = new JarOutputStream(Files.newOutputStream(jar))) {
            if (withJdk) {
                out.putNextEntry(new JarEntry("annotated-jdk/"));
                out.closeEntry();
            }
            if (classes != null) {
                out.putNextEntry(new JarEntry("Factory.class"));
                out.write(Files.readAllBytes(classes.resolve("Factory.class")));
                out.closeEntry();
            }
        }
        return jar;
    }

    /**
     * Loads {@code Factory} from the last of {@code path} and returns the annotated JDK that {@code
     * findAnnotatedJdk} finds for it, with the entries of {@code path} on the class path in order.
     *
     * @param path the class path, ending with the jar or directory that holds {@code Factory}
     * @return the URL of the annotated JDK, as a string
     * @throws Exception if loading fails
     */
    private static String findIn(Path... path) throws Exception {
        URL[] urls = new URL[path.length];
        for (int i = 0; i < path.length; i++) {
            urls[i] = path[i].toUri().toURL();
        }
        try (URLClassLoader loader = new URLClassLoader(urls, null)) {
            Class<?> factory = Class.forName("Factory", false, loader);
            return String.valueOf(AnnotationFileElementTypes.findAnnotatedJdk(factory));
        }
    }
}
