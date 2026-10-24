package org.checkerframework.javacutil;

import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.Tree;
import com.sun.source.util.JavacTask;
import com.sun.source.util.TreePath;
import com.sun.source.util.TreeScanner;
import com.sun.tools.javac.api.BasicJavacTask;
import com.sun.tools.javac.processing.JavacProcessingEnvironment;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.net.URI;
import java.util.Collections;

import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.SimpleJavaFileObject;
import javax.tools.ToolProvider;

/**
 * Unit tests for how {@link Resolver} finds local variables at a path to a tree of the compilation
 * unit. A path that ends with an artificial tree, which is not in the compilation unit, must not be
 * passed to {@code Resolver}; {@code StringToJavaExpression#atPath} resolves it in the scope of the
 * tree it was created for.
 */
public class ResolverTest {

    /** The source code that the tests look up names in. */
    private static final String SOURCE =
            String.join(
                    "\n",
                    "class R {",
                    "  void m(int p) {",
                    "    int a = 1;",
                    "    {",
                    "      int b = 2;",
                    "      int c = b + a;",
                    "      int later = 0;",
                    "    }",
                    "    java.util.function.IntUnaryOperator op = q -> { int e = q; return e + a; };",
                    "    Runnable r = new Runnable() {",
                    "      public void run() { int g = 1; int h = g + p; }",
                    "    };",
                    "    switch (p) {",
                    "      case 0:",
                    "        int s = 1;",
                    "        s += a;",
                    "        break;",
                    "      default:",
                    "    }",
                    "  }",
                    "  void n(int u) {",
                    "    u++;",
                    "    u--;",
                    "    int x = u;",
                    "    x++;",
                    "    x--;",
                    "  }",
                    "  R(int k) {",
                    "    this(k, k);",
                    "    k++;",
                    "    k--;",
                    "  }",
                    "  R(int i, int j) {}",
                    "}");

    /** The compilation unit of {@link #SOURCE}. */
    private CompilationUnitTree cu;

    /** The resolver under test. */
    private Resolver resolver;

    /**
     * Parses and attributes {@link #SOURCE}.
     *
     * @throws Exception if the compiler task cannot be created
     */
    @Before
    public void setUp() throws Exception {
        JavaFileObject file =
                new SimpleJavaFileObject(
                        URI.create("string:///R.java"), JavaFileObject.Kind.SOURCE) {
                    @Override
                    public CharSequence getCharContent(boolean ignoreEncodingErrors) {
                        return SOURCE;
                    }
                };
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        // Assign to CompilationTask before downcasting: a direct cast on getTask(...) is flagged
        // as a redundant cast by compilers whose getTask is declared to return JavacTask.
        JavaCompiler.CompilationTask compilationTask =
                compiler.getTask(
                        null,
                        null,
                        null,
                        Collections.singletonList("-proc:none"),
                        null,
                        Collections.singletonList(file));
        JavacTask task = (JavacTask) compilationTask;
        cu = task.parse().iterator().next();
        task.analyze();
        resolver =
                new Resolver(
                        JavacProcessingEnvironment.instance(((BasicJavacTask) task).getContext()));
    }

    /**
     * Returns the tree in {@link #cu} whose source text is {@code text}.
     *
     * @param text the source text of the tree, as {@code Tree.toString()} prints it
     * @return the tree whose source text is {@code text}
     */
    private Tree find(String text) {
        Tree[] result = new Tree[1];
        new TreeScanner<Void, Void>() {
            @Override
            public Void scan(Tree tree, Void unused) {
                if (tree != null && result[0] == null && tree.toString().equals(text)) {
                    result[0] = tree;
                }
                return super.scan(tree, unused);
            }
        }.scan(cu, null);
        if (result[0] == null) {
            throw new AssertionError("no tree " + text);
        }
        return result[0];
    }

    /**
     * Returns the path of the tree in {@link #cu} whose source text is {@code text}.
     *
     * @param text the source text of the tree
     * @return the path of the tree whose source text is {@code text}
     */
    private TreePath pathOf(String text) {
        return TreePath.getPath(cu, find(text));
    }

    /**
     * Asserts which of the given names are in scope at {@code path}.
     *
     * @param path a path
     * @param inScope the names of local variables and parameters in scope at {@code path}
     * @param notInScope the names of local variables that are not in scope at {@code path}
     */
    private void assertScope(TreePath path, String[] inScope, String... notInScope) {
        for (String name : inScope) {
            Assert.assertNotNull(
                    name + " in scope at " + path.getLeaf(),
                    resolver.findLocalVariableOrParameter(name, path));
        }
        for (String name : notInScope) {
            Assert.assertNull(
                    name + " not in scope at " + path.getLeaf(),
                    resolver.findLocalVariableOrParameter(name, path));
        }
    }

    /**
     * At a tree in the compilation unit, the scope is that of the tree. That includes {@code b},
     * which is declared in the innermost block, so the scope is neither that of the method nor that
     * of the block.
     */
    @Test
    public void treeInSource() {
        assertScope(pathOf("b + a"), new String[] {"p", "a", "b", "c"}, "later", "q", "g");
    }

    /** At a tree in the body of a lambda, the scope includes the lambda's variables. */
    @Test
    public void treeInLambda() {
        assertScope(pathOf("e + a"), new String[] {"p", "a", "q", "e"}, "b", "later", "g");
    }

    /**
     * At a tree in a method of an anonymous class, the scope includes the variables of the method
     * and of the enclosing method.
     */
    @Test
    public void treeInAnonymousClass() {
        assertScope(pathOf("g + p"), new String[] {"p", "a", "g", "h"}, "b", "q", "e");
    }

    /**
     * The expression statements of a run of expression statements share the environment of the
     * first, but a declaration ends the run: {@code x} is not in scope before its declaration, even
     * after the scope at {@code u++} has been computed, and it is in scope after the declaration.
     */
    @Test
    public void runOfExpressionStatements() {
        assertScope(pathOf("u++"), new String[] {"u"}, "x");
        assertScope(pathOf("u--"), new String[] {"u"}, "x");
        assertScope(pathOf("x++"), new String[] {"u", "x"}, "p");
        assertScope(pathOf("x--"), new String[] {"u", "x"}, "p");
        Assert.assertSame(envAt("u++"), envAt("u--"));
        Assert.assertSame(envAt("x++"), envAt("x--"));
        Assert.assertNotSame(envAt("u++"), envAt("x++"));
    }

    /**
     * A {@code this(...)} call ends a constructor prologue, so it is not part of the run of
     * expression statements after it.
     */
    @Test
    public void constructorCallEndsRun() {
        Assert.assertSame(envAt("k++"), envAt("k--"));
        Assert.assertNotSame(envAt("this(k, k)"), envAt("k++"));
    }

    /**
     * Returns the attribution environment at the tree in {@link #cu} whose source text is {@code
     * text}.
     *
     * @param text the source text of the tree
     * @return the attribution environment at the tree whose source text is {@code text}
     */
    private Object envAt(String text) {
        return resolver.getEnvForPath(pathOf(text));
    }

    /**
     * At a path that the CFG builder makes for a statement in a {@code switch}, without the {@code
     * case}, the scope is that of the statement, which includes the variables declared in the
     * {@code case}.
     */
    @Test
    public void treeInSwitchWithoutCase() {
        TreePath real = pathOf("s += a");
        TreePath statement = real.getParentPath();
        TreePath caseTree = statement.getParentPath();
        TreePath withoutCase =
                new TreePath(
                        new TreePath(caseTree.getParentPath(), statement.getLeaf()),
                        real.getLeaf());
        assertScope(withoutCase, new String[] {"p", "a", "s"}, "b", "q", "g");
    }
}
