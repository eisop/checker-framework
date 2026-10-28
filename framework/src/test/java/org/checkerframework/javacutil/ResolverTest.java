package org.checkerframework.javacutil;

import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.VariableTree;
import com.sun.source.util.JavacTask;
import com.sun.source.util.TreePath;
import com.sun.source.util.TreeScanner;
import com.sun.tools.javac.api.BasicJavacTask;
import com.sun.tools.javac.code.Symbol.VarSymbol;
import com.sun.tools.javac.processing.JavacProcessingEnvironment;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.TreeInfo;

import org.junit.Assert;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.lang.reflect.Field;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.lang.model.element.VariableElement;
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
                    "  int fld;",
                    "  void v(Object o, java.util.List<String> list) throws Exception {",
                    "    for (int i = 0, i2 = i; i < 1; i++) { int fi = i + i2; }",
                    "    for (String str : list) { str.length(); }",
                    "    try (java.io.StringReader rd = new java.io.StringReader(\"\")) {",
                    "      rd.read();",
                    "    } catch (RuntimeException ex) {",
                    "      ex.getMessage();",
                    "    }",
                    "    int fld2 = 0;",
                    "    Object anon = new Object() { int fld2 = 1; int get() { return fld2 + 0; } };",
                    "  }",
                    "  void neg(int sel) throws Exception {",
                    "    { int sib = 1; }",
                    "    sel++;",
                    "    sel--;",
                    "    int aft = 0;",
                    "    switch (sel) {",
                    "      case 1:",
                    "        int y = 0;",
                    "        break;",
                    "      case 2:",
                    "        y = 3;",
                    "        break;",
                    "      default:",
                    "    }",
                    "    int hid = 1;",
                    "    class L { int hid; int get() { return hid + 1; } }",
                    "    java.util.function.IntBinaryOperator bin = (int l1, int l2) -> l1 + l2;",
                    "  }",
                    "  void w(int sw) {",
                    "    switch (sw) {",
                    "      case 1:",
                    "        sw++;",
                    "        sw--;",
                    "        int z = sw;",
                    "        z++;",
                    "        z--;",
                    "        break;",
                    "      default:",
                    "    }",
                    "  }",
                    "}");

    /** Source code with a pattern variable and case rules, which need JDK 16 or later. */
    private static final String JDK16_SOURCE =
            String.join(
                    "\n",
                    "class P {",
                    "  void m(Object o) {",
                    "    if (o instanceof String s) { s.length(); }",
                    "    o.hashCode();",
                    "    switch (o.hashCode()) {",
                    "      case 1 -> o.toString();",
                    "      default -> o.getClass();",
                    "    }",
                    "  }",
                    "}");

    /** The compilation unit of {@link #SOURCE}. */
    private CompilationUnitTree cu;

    /** The resolver under test. */
    private Resolver resolver;

    /**
     * Parses and attributes {@link #SOURCE}.
     *
     * @throws IOException if the source cannot be parsed
     */
    @Before
    public void setUp() throws IOException {
        compile("R", SOURCE);
    }

    /**
     * Parses and attributes {@code source}, and sets {@link #cu} and {@link #resolver} for it.
     *
     * @param className the name of the top-level class of {@code source}
     * @param source the source code of a compilation unit
     * @throws IOException if the source cannot be parsed
     */
    private void compile(String className, String source) throws IOException {
        JavaFileObject file =
                new SimpleJavaFileObject(
                        URI.create("string:///" + className + ".java"),
                        JavaFileObject.Kind.SOURCE) {
                    @Override
                    public CharSequence getCharContent(boolean ignoreEncodingErrors) {
                        return source;
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
     * The variables of a {@code for}, an enhanced {@code for}, a {@code try}-with-resources, and a
     * {@code catch} are in scope in their bodies, and the lookup returns the symbols of their
     * declarations. A field of an anonymous class hides a local variable of the same name around
     * the class.
     */
    @Test
    public void statementVariables() {
        assertDeclaration("i + i2", "i", "int i = 0");
        assertDeclaration("i + i2", "i2", "int i2 = i");
        assertDeclaration("str.length()", "str", "String str");
        // Find the declaration of the resource by its name: JDK 11 and 17 print it with its
        // implicit "final", and JDK 21 without.
        assertLookup("rd.read()", "rd", true, false);
        assertDeclaration("ex.getMessage()", "ex", "RuntimeException ex");
        assertDeclaration("ex.getMessage()", "o", "Object o");
        assertScope(pathOf("str.length()"), new String[] {"o", "list"}, "i", "fi", "rd", "fld");
        assertScope(pathOf("ex.getMessage()"), new String[] {"ex"}, "rd", "str", "fld2", "nope");
        assertScope(pathOf("fld2 + 0"), new String[] {"o", "list"}, "fld2", "fld", "i");
    }

    /**
     * Asserts that the local variable or parameter named {@code name} at the tree whose source text
     * is {@code text} is the variable declared by the tree whose source text is {@code decl}.
     *
     * @param text the source text of the tree at which to look up {@code name}
     * @param name the name of the variable
     * @param decl the source text of the declaration of the variable
     */
    private void assertDeclaration(String text, String name, String decl) {
        Assert.assertSame(
                name + " at " + text,
                TreeUtils.elementFromDeclaration((VariableTree) find(decl)),
                resolver.findLocalVariableOrParameter(name, pathOf(text)));
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

    /**
     * A variable declared in an earlier sibling block or later in the same block is not in scope.
     * The source lookup does not find it, and since a variable of that name is declared in the
     * method, javac decides.
     */
    @Test
    public void notInScopeInSameMethod() {
        assertLookup("sel++", "sib", false, true);
        assertLookup("sel--", "aft", false, true);
        assertLookup("sel--", "sel", true, false);
    }

    /**
     * A variable declared in an earlier {@code case} of an old-style {@code switch} is in scope in
     * the later cases. The source lookup does not search the earlier cases, so javac finds it.
     */
    @Test
    public void earlierCase() {
        assertLookup("y = 3", "y", true, true);
    }

    /**
     * A {@code try}-with-resources variable is in scope in the {@code try} block, where the source
     * lookup finds it, and not in the {@code catch} block, where javac decides.
     */
    @Test
    public void tryWithResources() {
        assertLookup("rd.read()", "rd", true, false);
        assertLookup("ex.getMessage()", "rd", false, true);
        assertLookup("ex.getMessage()", "ex", true, false);
    }

    /**
     * In a local class, a field hides a local variable of the same name around the class. The
     * source lookup stops at the class, and javac finds the field, so the result is null rather
     * than the local variable.
     */
    @Test
    public void localClassFieldHidesLocal() {
        assertLookup("hid + 1", "hid", false, true);
    }

    /**
     * The explicit and implicit parameters of a lambda and the local variables of its body are
     * found in the source.
     */
    @Test
    public void lambdaVariables() {
        assertLookup("l1 + l2", "l1", true, false);
        assertLookup("l1 + l2", "l2", true, false);
        assertLookup("e + a", "q", true, false);
        assertLookup("e + a", "e", true, false);
        assertLookup("e + a", "a", true, false);
    }

    /**
     * A pattern variable is in scope where the pattern matched, and javac finds it; it is not in
     * scope after the {@code if}, and javac decides that too.
     *
     * @throws IOException if the source cannot be parsed
     */
    @Test
    public void patternVariable() throws IOException {
        Assume.assumeTrue("pattern matching requires JDK 16 or later", SystemUtil.jreVersion >= 16);
        compile("P", JDK16_SOURCE);
        assertLookup("s.length()", "s", true, true);
        assertLookup("o.hashCode()", "s", false, true);
        assertLookup("o.hashCode()", "o", true, false);
    }

    /**
     * The expression statements of a run of expression statements in a {@code case} share the
     * environment of the first, and a declaration ends the run.
     */
    @Test
    public void runOfExpressionStatementsInCase() {
        assertScope(pathOf("sw++"), new String[] {"sw"}, "z");
        assertScope(pathOf("z--"), new String[] {"sw", "z"}, "p");
        Assert.assertSame(envAt("sw++"), envAt("sw--"));
        Assert.assertSame(envAt("z++"), envAt("z--"));
        Assert.assertNotSame(envAt("sw++"), envAt("z++"));
    }

    /**
     * The statement of a {@code case} rule is the only statement of the rule, so it does not share
     * the environment of another statement.
     *
     * @throws IOException if the source cannot be parsed
     */
    @Test
    public void caseRule() throws IOException {
        Assume.assumeTrue("the source requires JDK 16 or later", SystemUtil.jreVersion >= 16);
        compile("P", JDK16_SOURCE);
        assertScope(pathOf("o.toString()"), new String[] {"o"}, "s");
        assertScope(pathOf("o.getClass()"), new String[] {"o"}, "s");
        Assert.assertNotSame(envAt("o.toString()"), envAt("o.getClass()"));
    }

    /**
     * Asserts what {@link Resolver#findLocalVariableOrParameter} returns for {@code name} at the
     * tree whose source text is {@code text}, and whether it consulted javac, which it does exactly
     * when it computes an attribution environment.
     *
     * <p>If the lookup finds the declaration in the source, the result is the symbol of the
     * declaration. If javac finds it, the result is the symbol of javac's attributed copy of the
     * declaration, a different object, so it is compared by its name and position.
     *
     * @param text the source text of the tree at which to look up {@code name}
     * @param name the name of the variable
     * @param found whether the result should be the variable declared with the name {@code name},
     *     which must be the only one of that name in {@link #cu}, rather than null
     * @param viaJavac whether the lookup should consult javac
     */
    private void assertLookup(String text, String name, boolean found, boolean viaJavac) {
        Map<?, ?> envCache = envCache();
        envCache.clear();
        VariableElement result = resolver.findLocalVariableOrParameter(name, pathOf(text));
        String what = name + " at " + text;
        Assert.assertEquals(what + " consults javac", viaJavac, !envCache.isEmpty());
        if (!found) {
            Assert.assertNull(what, result);
            return;
        }
        Assert.assertNotNull(what, result);
        VariableTree decl = declarationNamed(name);
        if (viaJavac) {
            Assert.assertEquals(what, name, result.getSimpleName().toString());
            // The position of a variable is that of its name, but the position of a binding
            // variable is that of its pattern.
            int pos = ((VarSymbol) result).pos;
            Assert.assertTrue(
                    what + " at " + pos,
                    TreeInfo.getStartPos((JCTree) decl) <= pos && pos <= ((JCTree) decl).pos);
        } else {
            Assert.assertSame(what, TreeUtils.elementFromDeclaration(decl), result);
        }
    }

    /**
     * Returns the only variable declaration in {@link #cu} with the given name.
     *
     * @param name the name of the variable
     * @return the only variable declaration in {@link #cu} named {@code name}
     */
    private VariableTree declarationNamed(String name) {
        List<VariableTree> result = new ArrayList<>();
        new TreeScanner<Void, Void>() {
            @Override
            public Void visitVariable(VariableTree tree, Void unused) {
                if (tree.getName().contentEquals(name)) {
                    result.add(tree);
                }
                return super.visitVariable(tree, unused);
            }
        }.scan(cu, null);
        Assert.assertEquals("declarations of " + name, 1, result.size());
        return result.get(0);
    }

    /**
     * Returns the cache of attribution environments of {@link #resolver}. The cache is empty after
     * a lookup that started empty exactly when the lookup did not consult javac.
     *
     * @return the cache of attribution environments of {@link #resolver}
     */
    private Map<?, ?> envCache() {
        try {
            Field field = Resolver.class.getDeclaredField("envCache");
            field.setAccessible(true);
            return (Map<?, ?>) field.get(resolver);
        } catch (ReflectiveOperationException e) {
            throw new LinkageError(e.getMessage(), e);
        }
    }
}
