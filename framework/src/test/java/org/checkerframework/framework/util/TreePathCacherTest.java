package org.checkerframework.framework.util;

import com.sun.source.tree.ClassTree;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.Tree;
import com.sun.source.util.JavacTask;
import com.sun.source.util.TreePath;
import com.sun.source.util.TreeScanner;

import org.junit.Assert;
import org.junit.Test;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.SimpleJavaFileObject;
import javax.tools.ToolProvider;

/**
 * Unit tests for {@link TreePathCacher}, checked against the JDK reference implementation {@link
 * TreePath#getPath(CompilationUnitTree, Tree)}.
 *
 * <p>These exercise the structural cases that the checker test suites do not target directly:
 *
 * <ul>
 *   <li>the {@link TreePathCacher#getPath(TreePath, Tree)} overload finding a target that is
 *       <em>not</em> under the given path's leaf (the out-of-subtree case), and
 *   <li>finding a target deep in a class with many top-level members.
 * </ul>
 *
 * A previous "optimization" that narrowed that overload to scan only the leaf's subtree passed
 * NullnessTest but produced wrong results here; this test guards against that regression.
 */
public class TreePathCacherTest {

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
     * Parses {@code code} (no annotation processing, no attribution) and returns its compilation
     * unit.
     *
     * @param className the simple name of the class declared in {@code code}
     * @param code the source text to parse
     * @return the parsed {@link CompilationUnitTree} for {@code code}
     * @throws Exception if the compiler task cannot be created or parsing fails
     */
    private static CompilationUnitTree parse(String className, String code) throws Exception {
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        JavacTask task =
                (JavacTask)
                        compiler.getTask(
                                null,
                                null,
                                null,
                                List.of("-proc:none"),
                                null,
                                List.of(source(className, code)));
        return task.parse().iterator().next();
    }

    /**
     * Returns every tree in {@code root}'s subtree, including {@code root} itself, in pre-order.
     *
     * @param root the root of the subtree to collect
     * @return all trees under {@code root} (and {@code root} itself), in pre-order
     */
    private static List<Tree> treesUnder(Tree root) {
        List<Tree> trees = new ArrayList<>();
        new TreeScanner<Void, Void>() {
            @Override
            public Void scan(Tree tree, Void unused) {
                if (tree != null) {
                    trees.add(tree);
                }
                return super.scan(tree, unused);
            }
        }.scan(root, null);
        return trees;
    }

    /**
     * Returns the first top-level class's method with the given name.
     *
     * @param cu the compilation unit whose first type declaration is searched
     * @param name the name of the method to find
     * @return the {@link MethodTree} named {@code name} in the first top-level class of {@code cu}
     * @throws AssertionError if no such method exists
     */
    private static MethodTree method(CompilationUnitTree cu, String name) {
        ClassTree clazz = (ClassTree) cu.getTypeDecls().get(0);
        for (Tree member : clazz.getMembers()) {
            if (member instanceof MethodTree
                    && ((MethodTree) member).getName().contentEquals(name)) {
                return (MethodTree) member;
            }
        }
        throw new AssertionError("no method " + name);
    }

    /**
     * Returns the source text of a class {@code Many} with {@code n} methods, each with a small
     * expression body.
     *
     * @param n the number of methods to generate
     * @return the source text of the generated class
     */
    private static String manyMethods(int n) {
        StringBuilder sb = new StringBuilder("class Many {\n");
        for (int i = 0; i < n; i++) {
            sb.append("  int m").append(i).append("() { int v").append(i).append(" = ").append(i);
            sb.append("; return v").append(i).append(" + 1; }\n");
        }
        sb.append("}\n");
        return sb.toString();
    }

    /**
     * Asserts that two {@link TreePath}s have the same chain of leaves, compared by identity, up to
     * the compilation-unit root.
     *
     * @param message a prefix for the assertion-failure message
     * @param expected the expected path (the JDK reference result)
     * @param actual the actual path (the {@link TreePathCacher} result)
     */
    private static void assertSamePath(String message, TreePath expected, TreePath actual) {
        if (expected == null || actual == null) {
            Assert.assertSame(message, expected, actual);
            return;
        }
        TreePath e = expected;
        TreePath a = actual;
        while (e != null && a != null) {
            Assert.assertSame(message + ": leaf", e.getLeaf(), a.getLeaf());
            e = e.getParentPath();
            a = a.getParentPath();
        }
        Assert.assertSame(message + ": path length", e, a); // both must be null
    }

    /**
     * Checks that {@code getPath(CompilationUnitTree, Tree)} matches the JDK for every tree, both
     * cold and cached.
     *
     * @throws Exception if parsing the test source fails
     */
    @Test
    public void firstOverloadMatchesJdk() throws Exception {
        CompilationUnitTree cu = parse("Many", manyMethods(8));
        TreePathCacher cacher = new TreePathCacher();
        for (Tree target : treesUnder(cu)) {
            assertSamePath(
                    "cold getPath(cu, t)",
                    TreePath.getPath(cu, target),
                    cacher.getPath(cu, target));
        }
        // Second pass: everything is cached now; results must still match.
        for (Tree target : treesUnder(cu)) {
            assertSamePath(
                    "cached getPath(cu, t)",
                    TreePath.getPath(cu, target),
                    cacher.getPath(cu, target));
        }
    }

    /**
     * Checks that {@code getPath(TreePath, Tree)} finds a target that is NOT under the given path's
     * leaf: the search expands outward to the whole compilation unit. A fresh cacher is used for
     * every target so it is found by scanning rather than served from the cache.
     *
     * @throws Exception if parsing the test source fails
     */
    @Test
    public void secondOverloadFindsOutOfSubtreeTargets() throws Exception {
        CompilationUnitTree cu = parse("Many", manyMethods(20));
        // A path into the first method; most targets are in *other* methods (out of its subtree).
        TreePath firstMethodPath = TreePath.getPath(cu, method(cu, "m0"));
        for (Tree target : treesUnder(cu)) {
            TreePathCacher cacher = new TreePathCacher();
            assertSamePath(
                    "getPath(firstMethodPath, t)",
                    TreePath.getPath(cu, target),
                    cacher.getPath(firstMethodPath, target));
        }
    }

    /**
     * Checks that {@code getPath(TreePath, Tree)} returns the correct path for targets under the
     * leaf.
     *
     * @throws Exception if parsing the test source fails
     */
    @Test
    public void secondOverloadFindsInSubtreeTargets() throws Exception {
        CompilationUnitTree cu = parse("Many", manyMethods(8));
        MethodTree m3 = method(cu, "m3");
        TreePath m3Path = TreePath.getPath(cu, m3);
        for (Tree target : treesUnder(m3)) {
            TreePathCacher cacher = new TreePathCacher();
            assertSamePath(
                    "getPath(m3Path, tInM3)",
                    TreePath.getPath(cu, target),
                    cacher.getPath(m3Path, target));
        }
    }

    /**
     * Checks that a tree from a different compilation unit is reported (and cached) as not present.
     *
     * @throws Exception if parsing the test sources fails
     */
    @Test
    public void absentTreeIsNull() throws Exception {
        CompilationUnitTree cu1 = parse("A", "class A { int f() { return 1; } }");
        CompilationUnitTree cu2 = parse("B", "class B { int g() { return 2; } }");
        List<Tree> cu2Trees = treesUnder(cu2);
        Tree fromCu2 = cu2Trees.get(cu2Trees.size() - 1);

        TreePathCacher cacher = new TreePathCacher();
        Assert.assertNull("absent tree", cacher.getPath(cu1, fromCu2));
        Assert.assertTrue("null is cached", cacher.isCached(fromCu2));
        Assert.assertNull("absent tree, cached", cacher.getPath(cu1, fromCu2));
    }
}
