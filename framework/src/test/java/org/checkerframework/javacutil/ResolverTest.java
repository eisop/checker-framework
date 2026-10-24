package org.checkerframework.javacutil;

import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.ParenthesizedTree;
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
 * Unit tests for how {@link Resolver} finds local variables at a path that ends with artificial
 * trees, such as the trees that the CFG builder creates for a compound assignment. Such a tree is
 * not part of the compilation unit, but it has a path: the path of the tree it was created for,
 * with the artificial tree appended.
 *
 * <p>In these tests the trees of the field initializer {@code (1 + 2)} stand in for artificial
 * trees: they are not in the method body that the paths lead into.
 */
public class ResolverTest {

    /** The source code that the tests look up names in. */
    private static final String SOURCE =
            String.join(
                    "\n",
                    "class R {",
                    "  int standIn = (1 + 2);",
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
                    "}");

    /** The compilation unit of {@link #SOURCE}. */
    private CompilationUnitTree cu;

    /** The resolver under test. */
    private Resolver resolver;

    /** The stand-in for an artificial tree that has an artificial child: {@code (1 + 2)}. */
    private ParenthesizedTree artificial;

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
        artificial = (ParenthesizedTree) find("(1 + 2)");
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

    /** At a tree in the compilation unit, the scope is that of the tree. */
    @Test
    public void treeInSource() {
        assertScope(pathOf("b + a"), new String[] {"p", "a", "b", "c"}, "later", "q", "g");
    }

    /**
     * At an artificial tree whose path is that of a tree in the compilation unit, the scope is that
     * of the tree in the compilation unit. That includes {@code b}, which is declared in the
     * innermost block, so the scope is neither that of the method nor that of the block.
     */
    @Test
    public void artificialTree() {
        TreePath path = new TreePath(pathOf("b + a"), artificial);
        assertScope(path, new String[] {"p", "a", "b", "c"}, "later", "q", "g");
    }

    /**
     * At an artificial tree that is a child of another artificial tree, the scope is that of the
     * tree in the compilation unit that the outer artificial tree was created for.
     */
    @Test
    public void artificialTreeInArtificialTree() {
        TreePath outer = new TreePath(pathOf("b + a"), artificial);
        TreePath inner = new TreePath(outer, artificial.getExpression());
        assertScope(inner, new String[] {"p", "a", "b", "c"}, "later", "q", "g");
    }

    /** At an artificial tree in the body of a lambda, the scope includes the lambda's variables. */
    @Test
    public void artificialTreeInLambda() {
        TreePath path = new TreePath(pathOf("e + a"), artificial);
        assertScope(path, new String[] {"p", "a", "q", "e"}, "b", "later", "g");
    }

    /**
     * At an artificial tree in a method of an anonymous class, the scope includes the variables of
     * the method and of the enclosing method.
     */
    @Test
    public void artificialTreeInAnonymousClass() {
        TreePath path = new TreePath(pathOf("g + p"), artificial);
        assertScope(path, new String[] {"p", "a", "g", "h"}, "b", "q", "e");
    }

    /**
     * At an artificial tree whose path is a path that the CFG builder makes for a statement in a
     * {@code switch}, without the {@code case}, the scope is that of the statement, which includes
     * the variables declared in the {@code case}.
     */
    @Test
    public void artificialTreeInSwitchWithoutCase() {
        TreePath real = pathOf("s += a");
        TreePath statement = real.getParentPath();
        TreePath caseTree = statement.getParentPath();
        TreePath withoutCase =
                new TreePath(
                        new TreePath(caseTree.getParentPath(), statement.getLeaf()),
                        real.getLeaf());
        assertScope(withoutCase, new String[] {"p", "a", "s"}, "b", "q", "g");
        assertScope(new TreePath(withoutCase, artificial), new String[] {"p", "a", "s"}, "b");
    }

    /**
     * An artificial tree has the same environment as the tree in the compilation unit that it was
     * created for, whichever of the two is looked up first.
     */
    @Test
    public void environmentIsShared() {
        TreePath real = pathOf("b + a");
        TreePath path = new TreePath(real, artificial);
        Assert.assertSame(resolver.getEnvForPath(path), resolver.getEnvForPath(real));
        Assert.assertSame(resolver.getEnvForPath(path), resolver.getEnvForPath(path));

        TreePath real2 = pathOf("e + a");
        TreePath path2 = new TreePath(real2, artificial.getExpression());
        Assert.assertSame(resolver.getEnvForPath(real2), resolver.getEnvForPath(path2));
    }
}
