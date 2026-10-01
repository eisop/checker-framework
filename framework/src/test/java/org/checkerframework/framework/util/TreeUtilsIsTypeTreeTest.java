package org.checkerframework.framework.util;

import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.VariableTree;
import com.sun.source.util.JavacTask;
import com.sun.source.util.TreeScanner;

import org.checkerframework.javacutil.TreeUtils;
import org.junit.Assert;
import org.junit.Test;

import java.net.URI;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicReference;

import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.SimpleJavaFileObject;
import javax.tools.ToolProvider;

/** Tests for {@link TreeUtils#isTypeTree(Tree)}. */
public class TreeUtilsIsTypeTreeTest {

    /**
     * Creates a {@link JavaFileObject} with the given class name and code.
     *
     * @param className the class name
     * @param code the Java source code
     * @return a {@link JavaFileObject} representing the source code
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
     * Tests that {@link TreeUtils#isTypeTree(Tree)} returns true for uses of type variables (e.g.
     * {@code T field;}) and false for expressions (e.g. {@code field = null;}).
     *
     * @throws Exception if Java compilation fails
     */
    @Test
    public void testTypeVariableUseIsTypeTree() throws Exception {
        String code =
                "class Gen<T> {\n"
                        + "    T field;\n"
                        + "    String str;\n"
                        + "    void m() {\n"
                        + "        field = null;\n"
                        + "    }\n"
                        + "}\n";
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        JavaCompiler.CompilationTask compilationTask =
                compiler.getTask(
                        null,
                        null,
                        null,
                        Collections.singletonList("-proc:none"),
                        null,
                        Collections.singletonList(source("Gen", code)));
        JavacTask task = (JavacTask) compilationTask;
        CompilationUnitTree root = task.parse().iterator().next();
        task.analyze();

        AtomicReference<VariableTree> tField = new AtomicReference<>();
        AtomicReference<VariableTree> strField = new AtomicReference<>();
        AtomicReference<Tree> exprIdent = new AtomicReference<>();
        new TreeScanner<Void, Void>() {
            @Override
            public Void visitVariable(VariableTree node, Void unused) {
                if (node.getName().contentEquals("field")) {
                    tField.set(node);
                } else if (node.getName().contentEquals("str")) {
                    strField.set(node);
                }
                return super.visitVariable(node, unused);
            }

            @Override
            public Void visitAssignment(com.sun.source.tree.AssignmentTree node, Void unused) {
                exprIdent.set(node.getVariable());
                return super.visitAssignment(node, unused);
            }
        }.scan(root, null);

        Assert.assertNotNull(tField.get());
        Assert.assertNotNull(strField.get());
        Assert.assertNotNull(exprIdent.get());

        // String is a TypeElement use: isTypeTree returns true
        Assert.assertTrue(TreeUtils.isTypeTree(strField.get().getType()));

        // T is a TypeParameterElement use: isTypeTree returns true
        Assert.assertTrue(
                "Expected isTypeTree to be true for type variable use: "
                        + tField.get().getType()
                        + " (kind: "
                        + tField.get().getType().getKind()
                        + ")",
                TreeUtils.isTypeTree(tField.get().getType()));

        // field in `field = null` is an expression use: isTypeTree returns false
        Assert.assertFalse(TreeUtils.isTypeTree(exprIdent.get()));
    }
}
