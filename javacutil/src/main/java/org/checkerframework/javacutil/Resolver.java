package org.checkerframework.javacutil;

import com.sun.source.tree.BlockTree;
import com.sun.source.tree.CaseTree;
import com.sun.source.tree.CatchTree;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.EnhancedForLoopTree;
import com.sun.source.tree.ExpressionStatementTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.ForLoopTree;
import com.sun.source.tree.LambdaExpressionTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.StatementTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.TryTree;
import com.sun.source.tree.VariableTree;
import com.sun.source.util.TreePath;
import com.sun.source.util.TreeScanner;
import com.sun.source.util.Trees;
import com.sun.tools.javac.api.JavacScope;
import com.sun.tools.javac.code.Kinds;
import com.sun.tools.javac.code.Kinds.KindSelector;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Symbol.ClassSymbol;
import com.sun.tools.javac.code.Symbol.PackageSymbol;
import com.sun.tools.javac.code.Symbol.TypeSymbol;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.comp.AttrContext;
import com.sun.tools.javac.comp.DeferredAttr;
import com.sun.tools.javac.comp.Env;
import com.sun.tools.javac.comp.Resolve;
import com.sun.tools.javac.processing.JavacProcessingEnvironment;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.TreeInfo;
import com.sun.tools.javac.util.Context;
import com.sun.tools.javac.util.JCDiagnostic.DiagnosticPosition;
import com.sun.tools.javac.util.List;
import com.sun.tools.javac.util.ListBuffer;
import com.sun.tools.javac.util.Log;
import com.sun.tools.javac.util.Log.DiscardDiagnosticHandler;
import com.sun.tools.javac.util.Name;
import com.sun.tools.javac.util.Names;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.plumelib.util.MapsP;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeMirror;

/** A utility class to find symbols corresponding to string references (identifiers). */
// This class reflectively accesses jdk.compiler/com.sun.tools.javac.comp.
// This is why --add-opens=jdk.compiler/com.sun.tools.javac.comp=ALL-UNNAMED is required when
// running the Checker Framework.  If this class is re-written, then that --add-opens should be
// removed.
public class Resolver {

    /** Instance of {@link Resolve} for name resolution. */
    private final Resolve resolve;

    /** Instance of {@link Names} for access to the compiler's name table. */
    private final Names names;

    /** Instance of {@link Trees}. */
    private final Trees trees;

    /** Instance of {@link Log} for error logs. */
    private final Log log;

    /**
     * The key of {@link #envCache} in the compiler's {@link Context}, which makes the cache
     * per-compilation and shared by all {@code Resolver}s of the compilation.
     */
    private static final Context.Key<Map<Tree, Env<AttrContext>>> ENV_CACHE_KEY =
            new Context.Key<>();

    /** The maximum number of entries in {@link #envCache}. */
    private static final int ENV_CACHE_SIZE = 300;

    /**
     * Maps the leaf of a tree path to the attribution environment that {@link #getEnvForPath}
     * returned for it. Computing the environment of a path inside a method body re-attributes a
     * copy of the method, and the same location is often queried repeatedly, such as when the
     * expression strings in an annotation are parsed at the same location in several dataflow
     * iterations or by several type factories. The environment is the same at each query.
     */
    private final Map<Tree, Env<AttrContext>> envCache;

    /**
     * The member whose variable names {@link #declaredNames} holds, or null. Queries usually come
     * in runs for the same method, so a single entry suffices.
     */
    private @Nullable Tree declaredNamesMember = null;

    /** The names of all variables declared in {@link #declaredNamesMember}, including nested. */
    private @Nullable Set<javax.lang.model.element.Name> declaredNames = null;

    /** {@code Resolve#findMethod} method. */
    private static final Method FIND_METHOD;

    /** {@code Resolve#findVar} method. */
    private static final Method FIND_VAR;

    /** {@code Resolve#findIdent} method. */
    private static final Method FIND_IDENT;

    /** {@code Resolve#findIdentInType} method. */
    private static final Method FIND_IDENT_IN_TYPE;

    /** {@code Resolve#findIdentInPackage} method. */
    private static final Method FIND_IDENT_IN_PACKAGE;

    /** {@code Resolve#findType} method. */
    private static final Method FIND_TYPE;

    /** {@code com.sun.tools.javac.comp.Resolve$AccessError} class. */
    private static final Class<?> ACCESSERROR;

    /** {@code com.sun.tools.javac.comp.Resolve$AccessError#access} method. */
    // Note that currently access(...) is defined in InvalidSymbolError, a superclass of AccessError
    private static final Method ACCESSERROR_ACCESS;

    /**
     * {@code com.sun.tools.javac.comp.Resolve$MethodResolutionContext} no-arg-style constructor.
     */
    private static final Constructor<?> METHOD_RESOLUTION_CONTEXT_CTOR;

    /** {@code Resolve$MethodResolutionContext#attrMode} field. */
    private static final Field METHOD_RESOLUTION_CONTEXT_ATTR_MODE;

    /** {@code Resolve$MethodResolutionContext#step} field. */
    private static final Field METHOD_RESOLUTION_CONTEXT_STEP;

    /** {@code Resolve#currentResolutionContext} field. */
    private static final Field RESOLVE_CURRENT_RESOLUTION_CONTEXT;

    /** {@code Resolve#methodResolutionSteps} field. */
    private static final Field RESOLVE_METHOD_RESOLUTION_STEPS;

    /**
     * Method for new Log.DiscardDiagnosticHandler. Before JDK 25, DiscardDiagnosticHandler was a
     * static inner class of Log and an instance of log was passed as the first argument. Starting
     * with JDK 25, DiscardDiagnosticHandler is an inner class of log.
     */
    private static final Constructor<DiscardDiagnosticHandler> NEW_DIAGNOSTIC_HANDLER;

    /** The latest source version supported by this compiler. */
    private static final int sourceVersionNumber =
            Integer.parseInt(SourceVersion.latest().toString().substring("RELEASE_".length()));

    /** True if we are running on at least Java 13. */
    private static final boolean atLeastJava13 = sourceVersionNumber >= 13;

    /** True if we are running on at least Java 23. */
    private static final boolean atLeastJava23 = sourceVersionNumber >= 23;

    /** Whether we are running on at least Java 28. */
    private static final boolean atLeastJava28 = sourceVersionNumber >= 28;

    static {
        try {
            FIND_METHOD =
                    Resolve.class.getDeclaredMethod(
                            "findMethod",
                            Env.class,
                            Type.class,
                            Name.class,
                            List.class,
                            List.class,
                            boolean.class,
                            boolean.class);
            FIND_METHOD.setAccessible(true);

            if (atLeastJava28) {
                // Changed in
                // https://github.com/openjdk/jdk/commit/f1c7c3e9bc0c8794dbf16b4fdc31464d5abe473b
                FIND_VAR =
                        Resolve.class.getDeclaredMethod(
                                "findVar",
                                DiagnosticPosition.class,
                                Env.class,
                                Name.class,
                                boolean.class);
            } else if (atLeastJava23) {
                // Changed in
                // https://github.com/openjdk/jdk/commit/e227c7e37d4de0656f013f3a936b1acfa56cc2e0
                FIND_VAR =
                        Resolve.class.getDeclaredMethod(
                                "findVar", DiagnosticPosition.class, Env.class, Name.class);
            } else {
                FIND_VAR = Resolve.class.getDeclaredMethod("findVar", Env.class, Name.class);
            }
            FIND_VAR.setAccessible(true);

            if (atLeastJava13) {
                FIND_IDENT =
                        Resolve.class.getDeclaredMethod(
                                "findIdent",
                                DiagnosticPosition.class,
                                Env.class,
                                Name.class,
                                KindSelector.class);
            } else {
                FIND_IDENT =
                        Resolve.class.getDeclaredMethod(
                                "findIdent", Env.class, Name.class, KindSelector.class);
            }
            FIND_IDENT.setAccessible(true);

            if (atLeastJava28) {
                // Changed in
                // https://github.com/openjdk/jdk/commit/850c789a827e21cda3096276d9638f56fae76c3e
                FIND_IDENT_IN_TYPE =
                        Resolve.class.getDeclaredMethod(
                                "findIdentInType",
                                DiagnosticPosition.class,
                                Env.class,
                                Type.class,
                                Name.class,
                                KindSelector.class,
                                JCTree.class);
            } else if (atLeastJava13) {
                FIND_IDENT_IN_TYPE =
                        Resolve.class.getDeclaredMethod(
                                "findIdentInType",
                                DiagnosticPosition.class,
                                Env.class,
                                Type.class,
                                Name.class,
                                KindSelector.class);
            } else {
                FIND_IDENT_IN_TYPE =
                        Resolve.class.getDeclaredMethod(
                                "findIdentInType",
                                Env.class,
                                Type.class,
                                Name.class,
                                KindSelector.class);
            }
            FIND_IDENT_IN_TYPE.setAccessible(true);

            if (atLeastJava13) {
                FIND_IDENT_IN_PACKAGE =
                        Resolve.class.getDeclaredMethod(
                                "findIdentInPackage",
                                DiagnosticPosition.class,
                                Env.class,
                                TypeSymbol.class,
                                Name.class,
                                KindSelector.class);
            } else {
                FIND_IDENT_IN_PACKAGE =
                        Resolve.class.getDeclaredMethod(
                                "findIdentInPackage",
                                Env.class,
                                TypeSymbol.class,
                                Name.class,
                                KindSelector.class);
            }
            FIND_IDENT_IN_PACKAGE.setAccessible(true);

            FIND_TYPE = Resolve.class.getDeclaredMethod("findType", Env.class, Name.class);
            FIND_TYPE.setAccessible(true);

            // Pre JDK 25:
            //   new Log.DiscardDiagnosticHandler(log)
            // JDK 25:
            //   log.new DiscardDiagnosticHandler()
            // But both of those are reflectively accessed the same way.
            NEW_DIAGNOSTIC_HANDLER = Log.DiscardDiagnosticHandler.class.getConstructor(Log.class);
            NEW_DIAGNOSTIC_HANDLER.setAccessible(true);
        } catch (Exception e) {
            Error err =
                    new AssertionError(
                            "Compiler 'Resolve' class doesn't contain required 'find*' method");
            err.initCause(e);
            throw err;
        }

        try {
            ACCESSERROR = Class.forName("com.sun.tools.javac.comp.Resolve$AccessError");
            ACCESSERROR_ACCESS = ACCESSERROR.getMethod("access", Name.class, TypeSymbol.class);
            ACCESSERROR_ACCESS.setAccessible(true);
        } catch (ClassNotFoundException e) {
            throw new BugInCF("Compiler 'Resolve$AccessError' class could not be retrieved.", e);
        } catch (NoSuchMethodException e) {
            throw new BugInCF(
                    "Compiler 'Resolve$AccessError' class doesn't contain required 'access' method",
                    e);
        }

        try {
            Class<?> methCtxClss =
                    Class.forName("com.sun.tools.javac.comp.Resolve$MethodResolutionContext");
            // TODO: this assumes only one constructor. Could be improved.
            METHOD_RESOLUTION_CONTEXT_CTOR = methCtxClss.getDeclaredConstructors()[0];
            METHOD_RESOLUTION_CONTEXT_CTOR.setAccessible(true);

            METHOD_RESOLUTION_CONTEXT_ATTR_MODE = methCtxClss.getDeclaredField("attrMode");
            METHOD_RESOLUTION_CONTEXT_ATTR_MODE.setAccessible(true);

            METHOD_RESOLUTION_CONTEXT_STEP = methCtxClss.getDeclaredField("step");
            METHOD_RESOLUTION_CONTEXT_STEP.setAccessible(true);

            RESOLVE_CURRENT_RESOLUTION_CONTEXT =
                    Resolve.class.getDeclaredField("currentResolutionContext");
            RESOLVE_CURRENT_RESOLUTION_CONTEXT.setAccessible(true);

            RESOLVE_METHOD_RESOLUTION_STEPS =
                    Resolve.class.getDeclaredField("methodResolutionSteps");
            RESOLVE_METHOD_RESOLUTION_STEPS.setAccessible(true);
        } catch (ClassNotFoundException | NoSuchFieldException e) {
            throw new BugInCF(
                    "Compiler 'Resolve$MethodResolutionContext' class or its expected fields could"
                            + " not be retrieved.",
                    e);
        }
    }

    public Resolver(ProcessingEnvironment env) {
        Context context = ((JavacProcessingEnvironment) env).getContext();
        this.resolve = Resolve.instance(context);
        this.names = Names.instance(context);
        this.trees = Trees.instance(env);
        this.log = Log.instance(context);
        Map<Tree, Env<AttrContext>> cache = context.get(ENV_CACHE_KEY);
        if (cache == null) {
            cache = MapsP.createLruCache(ENV_CACHE_SIZE);
            context.put(ENV_CACHE_KEY, cache);
        }
        this.envCache = cache;
    }

    /**
     * Determine the environment for the given path. The environment is cached per leaf, and the
     * expression statements of a run of expression statements share one environment (see {@link
     * #pathWithSameScope}).
     *
     * <p>The leaf of {@code path} must be a tree of the compilation unit; for an artificial tree,
     * pass the path of the tree it was created for (see {@code StringToJavaExpression#atPath}).
     *
     * @param path the tree path to the local scope
     * @return the corresponding attribution environment
     */
    public Env<AttrContext> getEnvForPath(TreePath path) {
        Tree leaf = path.getLeaf();
        Env<AttrContext> cached = envCache.get(leaf);
        if (cached != null) {
            return cached;
        }
        TreePath scopePath = pathWithSameScope(path);
        @SuppressWarnings("interning:not.interned") // pathWithSameScope returns path itself
        boolean ownScope = scopePath == path;
        Env<AttrContext> result = ownScope ? computeEnvForPath(path) : getEnvForPath(scopePath);
        envCache.put(leaf, result);
        return result;
    }

    /**
     * Returns a path whose attribution environment is the same as that of {@code path}, and that is
     * shared by as many other paths as possible, so that {@link #getEnvForPath} computes the
     * environment once for all of them. Computing an environment re-attributes the method body up
     * to the leaf, so computing it separately at each statement of a long method body is quadratic
     * in the length of the body.
     *
     * <p>An expression statement adds nothing to the scope of the statements after it: unlike an
     * {@code if} or a loop, it introduces no pattern binding into them. The exception is a {@code
     * this(...)} or {@code super(...)} call, which ends a constructor prologue. So the environment
     * at the expression of an expression statement in a block, or in the statements of an old-style
     * {@code case}, is the same as at the expression of the first statement of the run of such
     * statements that the statement is in. If the leaf of {@code path} is such an expression,
     * returns the path of the expression of that first statement. Otherwise, including for the
     * statement of a {@code case} rule ({@code case L -> ...}), which is the only statement of its
     * rule, or a path whose statement is not in the block or {@code case}, returns {@code path}
     * itself.
     *
     * @param path the tree path to the local scope
     * @return a path with the same attribution environment as {@code path}
     */
    private static TreePath pathWithSameScope(TreePath path) {
        TreePath stmtPath = path.getParentPath();
        if (stmtPath == null || !(stmtPath.getLeaf() instanceof ExpressionStatementTree)) {
            return path;
        }
        TreePath blockPath = stmtPath.getParentPath();
        if (blockPath == null) {
            return path;
        }
        Tree block = blockPath.getLeaf();
        java.util.List<? extends StatementTree> statements;
        if (block instanceof BlockTree) {
            statements = ((BlockTree) block).getStatements();
        } else if (block instanceof CaseTree) {
            // getStatements is null for a case rule.
            statements = ((CaseTree) block).getStatements();
            if (statements == null) {
                return path;
            }
        } else {
            return path;
        }
        ExpressionStatementTree stmt = (ExpressionStatementTree) stmtPath.getLeaf();
        @SuppressWarnings("interning:not.interned") // AST node comparison
        boolean leafIsExpression = stmt.getExpression() == path.getLeaf();
        if (!leafIsExpression || !isScopeTransparent(stmt)) {
            return path;
        }
        ExpressionStatementTree runStart = null;
        for (StatementTree s : statements) {
            @SuppressWarnings("interning:not.interned") // AST node comparison
            boolean isStmt = s == stmt;
            if (isStmt) {
                return runStart == null
                        ? path
                        : new TreePath(new TreePath(blockPath, runStart), runStart.getExpression());
            }
            if (isScopeTransparent(s)) {
                if (runStart == null) {
                    runStart = (ExpressionStatementTree) s;
                }
            } else {
                runStart = null;
            }
        }
        // The block or case does not contain the statement, so the path is not a path in the
        // source.
        return path;
    }

    /**
     * Returns true if the given statement adds nothing to the scope of the statements after it in
     * its block or {@code case}: it is an expression statement that is not a {@code this(...)} or
     * {@code super(...)} call.
     *
     * @param stmt a statement
     * @return true if {@code stmt} is an expression statement that does not change the scope of the
     *     statements after it
     */
    private static boolean isScopeTransparent(StatementTree stmt) {
        if (!(stmt instanceof ExpressionStatementTree)) {
            return false;
        }
        ExpressionTree expr = ((ExpressionStatementTree) stmt).getExpression();
        return !(expr instanceof MethodInvocationTree)
                || (!TreeUtils.isSuperConstructorCall((MethodInvocationTree) expr)
                        && !TreeUtils.isThisConstructorCall((MethodInvocationTree) expr));
    }

    /**
     * Determine the environment for the given path. Does not use a cache.
     *
     * @param path the tree path to the local scope
     * @return the corresponding attribution environment
     */
    private Env<AttrContext> computeEnvForPath(TreePath path) {
        TreePath iter = path;
        JavacScope scope = null;
        while (scope == null && iter != null) {
            try {
                scope = (JavacScope) trees.getScope(iter);
            } catch (NullPointerException t) {
                // This statement fixes https://github.com/typetools/checker-framework/issues/1059 .
                // It work around the crash by skipping through the TreePath until something doesn't
                // crash. This probably returns the class scope, so users might not get the
                // variables they expect. But that is better than crashing.
                iter = iter.getParentPath();
            }
        }
        if (scope != null) {
            return scope.getEnv();
        } else {
            throw new BugInCF("Could not determine any possible scope for path: " + path.getLeaf());
        }
    }

    /**
     * Finds the package with name {@code name}.
     *
     * @param name the name of the package
     * @param path the tree path to the local scope
     * @return the {@code PackageSymbol} for the package if it is found, {@code null} otherwise
     */
    public @Nullable PackageSymbol findPackage(String name, TreePath path) {
        Log.DiagnosticHandler discardDiagnosticHandler = newDiagnosticHandler();
        try {
            Env<AttrContext> env = getEnvForPath(path);
            final Element res;
            if (atLeastJava13) {
                res =
                        resolve(
                                FIND_IDENT,
                                null,
                                env,
                                names.fromString(name),
                                Kinds.KindSelector.PCK);
            } else {
                res = resolve(FIND_IDENT, env, names.fromString(name), Kinds.KindSelector.PCK);
            }

            // findIdent will return a PackageSymbol even for a symbol that is not a package,
            // such as a.b.c.MyClass.myStaticField. "exists()" must be called on it to ensure
            // that it exists.
            if (res.getKind() == ElementKind.PACKAGE) {
                PackageSymbol ps = (PackageSymbol) res;
                return ps.exists() ? ps : null;
            } else {
                return null;
            }
        } finally {
            log.popDiagnosticHandler(discardDiagnosticHandler);
        }
    }

    /**
     * Finds the field with name {@code name} in {@code type} or a superclass or superinterface of
     * {@code type}.
     *
     * <p>The method adheres to all the rules of Java's scoping (while also considering the imports)
     * for name resolution.
     *
     * @param name the name of the field
     * @param type the type of the receiver (i.e., the type in which to look for the field)
     * @param path the tree path to the local scope
     * @return the element for the field, {@code null} otherwise
     */
    public @Nullable VariableElement findField(String name, TypeMirror type, TreePath path) {
        Log.DiagnosticHandler discardDiagnosticHandler = newDiagnosticHandler();
        try {
            Env<AttrContext> env = getEnvForPath(path);
            final Element res;
            if (atLeastJava28) {
                res =
                        resolve(
                                FIND_IDENT_IN_TYPE,
                                null,
                                env,
                                type,
                                names.fromString(name),
                                Kinds.KindSelector.VAR,
                                null);
            } else if (atLeastJava13) {
                res =
                        resolve(
                                FIND_IDENT_IN_TYPE,
                                null,
                                env,
                                type,
                                names.fromString(name),
                                Kinds.KindSelector.VAR);
            } else {
                res =
                        resolve(
                                FIND_IDENT_IN_TYPE,
                                env,
                                type,
                                names.fromString(name),
                                Kinds.KindSelector.VAR);
            }

            if (res.getKind().isField()) {
                return (VariableElement) res;
            } else if (res.getKind() == ElementKind.OTHER && ACCESSERROR.isInstance(res)) {
                // Return the inaccessible field that was found
                return (VariableElement) invokeNoException(res, ACCESSERROR_ACCESS, null, null);
            } else {
                // Most likely didn't find the field and the Element is a SymbolNotFoundError
                return null;
            }
        } finally {
            log.popDiagnosticHandler(discardDiagnosticHandler);
        }
    }

    /**
     * Finds the local variable (including formal parameters) with name {@code name} in the given
     * scope.
     *
     * <p>If the declaration of the variable is found in the source (see {@link
     * #localDeclarationInScope}), the result is the symbol of that declaration. Otherwise, as for a
     * pattern variable, a variable of an earlier {@code case} of an old-style {@code switch}, or a
     * local variable of an enclosing method used in a local or anonymous class, the result is the
     * symbol from javac's attributed copy of the enclosing code, which {@code Trees#getScope}
     * creates. The two are different objects, and they differ in flags: the copy can lack {@code
     * EFFECTIVELY_FINAL}, so that {@link ElementUtils#isEffectivelyFinal} is false for it, which
     * makes the Lock Checker report {@code lock.expression.not.final} for an effectively final
     * variable (eisop#2375). A caller that compares the result with another symbol by identity, or
     * that tests its flags, must allow for either. {@code LocalVariable} compares variables by
     * name, position, and owner, so both give the same {@code LocalVariable}.
     *
     * @param name the name of the local variable
     * @param path the tree path to the local scope
     * @return the element for the local variable, {@code null} otherwise
     */
    public @Nullable VariableElement findLocalVariableOrParameter(String name, TreePath path) {
        Name varName = names.fromString(name);
        // Computing the environment re-attributes the enclosing method up to the leaf, so first
        // look for the declaration in the source.
        VariableTree decl = localDeclarationInScope(varName, path);
        if (decl != null) {
            Symbol sym = TreeInfo.symbolFor((JCTree) decl);
            if (sym != null) {
                return (VariableElement) sym;
            }
        } else if (!mayBeLocalVariableInScope(varName, path)) {
            return null;
        }
        Log.DiagnosticHandler discardDiagnosticHandler = newDiagnosticHandler();
        try {
            Env<AttrContext> env = getEnvForPath(path);
            // Either a VariableElement or a SymbolNotFoundError.
            Element res;
            if (atLeastJava28) {
                DiagnosticPosition pos = (DiagnosticPosition) path.getLeaf();
                res = resolve(FIND_VAR, pos, env, varName, false);
            } else if (atLeastJava23) {
                DiagnosticPosition pos = (DiagnosticPosition) path.getLeaf();
                res = resolve(FIND_VAR, pos, env, varName);
            } else {
                res = resolve(FIND_VAR, env, varName);
            }
            // Every kind in the documentation of Element.getKind() is explicitly tested, possibly
            // in the "default:" case.
            switch (res.getKind()) {
                case EXCEPTION_PARAMETER:
                case LOCAL_VARIABLE:
                case PARAMETER:
                case RESOURCE_VARIABLE:
                    return (VariableElement) res;
                case ENUM_CONSTANT:
                case FIELD:
                    return null;
                default:
                    if (ElementUtils.isBindingVariable(res)) {
                        return (VariableElement) res;
                    }
                    if (res instanceof VariableElement) {
                        throw new BugInCF("unhandled variable ElementKind " + res.getKind());
                    }
                    // The Element might be a SymbolNotFoundError.
                    return null;
            }
        } finally {
            log.popDiagnosticHandler(discardDiagnosticHandler);
        }
    }

    /**
     * Returns the declaration of a local variable or parameter named {@code name} that the source
     * shows to be in scope at the leaf of {@code path}, or null if there is none or the scope is
     * not recognized.
     *
     * <p>The declarations recognized are: those of the statements before the leaf in an enclosing
     * block or {@code case}, the variables of an enclosing {@code for}, enhanced {@code for},
     * {@code catch}, and {@code try}-with-resources, and the parameters of an enclosing lambda or
     * method. Each of these is in scope at the leaf, and no other variable in scope can have the
     * same name: a local variable or parameter must not be redeclared within its scope (JLS 6.4).
     * That holds only up to the innermost enclosing class, whose fields may hide the local
     * variables around the class, so the search stops there. Pattern variables, whose scope depends
     * on the flow of the enclosing code, are not recognized; {@link #mayBeLocalVariableInScope}
     * sends their names to javac.
     *
     * @param name the name of the variable
     * @param path the tree path to the local scope
     * @return the declaration of the variable named {@code name} in scope at the leaf of {@code
     *     path}, or null
     */
    private static @Nullable VariableTree localDeclarationInScope(Name name, TreePath path) {
        Tree child = path.getLeaf();
        for (TreePath p = path.getParentPath(); p != null; p = p.getParentPath()) {
            Tree parent = p.getLeaf();
            VariableTree decl = null;
            switch (parent.getKind()) {
                case BLOCK:
                    decl = declarationBefore(name, ((BlockTree) parent).getStatements(), child);
                    break;
                case CASE:
                    // getStatements is null for a case rule.
                    java.util.List<? extends StatementTree> caseStatements =
                            ((CaseTree) parent).getStatements();
                    if (caseStatements != null) {
                        decl = declarationBefore(name, caseStatements, child);
                    }
                    break;
                case FOR_LOOP:
                    java.util.List<? extends StatementTree> init =
                            ((ForLoopTree) parent).getInitializer();
                    decl = declarationBefore(name, init, child);
                    if (decl == null && !init.contains(child)) {
                        decl = declarationBefore(name, init, null);
                    }
                    break;
                case ENHANCED_FOR_LOOP:
                    EnhancedForLoopTree forEach = (EnhancedForLoopTree) parent;
                    if (isSameTree(forEach.getStatement(), child)) {
                        decl = ifNamed(forEach.getVariable(), name);
                    }
                    break;
                case CATCH:
                    CatchTree catchTree = (CatchTree) parent;
                    if (isSameTree(catchTree.getBlock(), child)) {
                        decl = ifNamed(catchTree.getParameter(), name);
                    }
                    break;
                case TRY:
                    TryTree tryTree = (TryTree) parent;
                    java.util.List<? extends Tree> resources = tryTree.getResources();
                    if (isSameTree(tryTree.getBlock(), child)) {
                        decl = declarationBefore(name, resources, null);
                    } else {
                        decl = declarationBefore(name, resources, child);
                    }
                    break;
                case LAMBDA_EXPRESSION:
                    LambdaExpressionTree lambda = (LambdaExpressionTree) parent;
                    if (isSameTree(lambda.getBody(), child)) {
                        decl = declarationBefore(name, lambda.getParameters(), null);
                    }
                    break;
                case METHOD:
                    MethodTree method = (MethodTree) parent;
                    if (isSameTree(method.getBody(), child)) {
                        decl = declarationBefore(name, method.getParameters(), null);
                    }
                    break;
                default:
                    if (parent instanceof ClassTree) {
                        return null;
                    }
            }
            if (decl != null) {
                return decl;
            }
            child = parent;
        }
        return null;
    }

    /**
     * Returns the variable declaration named {@code name} among the trees of {@code trees} before
     * {@code end}, or null if there is none or {@code end} is not in {@code trees}. If {@code end}
     * is null, searches all of {@code trees}.
     *
     * @param name the name of the variable
     * @param trees the trees to search
     * @param end the tree before which to search, or null to search all of {@code trees}
     * @return the declaration of the variable named {@code name} before {@code end}, or null
     */
    private static @Nullable VariableTree declarationBefore(
            Name name, Iterable<? extends Tree> trees, @Nullable Tree end) {
        VariableTree result = null;
        for (Tree t : trees) {
            if (isSameTree(t, end)) {
                return result;
            }
            if (result == null && t instanceof VariableTree) {
                result = ifNamed((VariableTree) t, name);
            }
        }
        return end == null ? result : null;
    }

    /**
     * Returns false if no local variable or parameter named {@code name} can be in scope at the
     * leaf of {@code path}: the leaf is in a member of a class that is not in a method or other
     * code, and no variable of that name is declared anywhere in the member, which includes the
     * pattern variables. Otherwise, returns true.
     *
     * @param name the name of the variable
     * @param path the tree path to the local scope
     * @return false if no local variable or parameter named {@code name} is in scope at the leaf of
     *     {@code path}
     */
    private boolean mayBeLocalVariableInScope(Name name, TreePath path) {
        Tree member = null;
        TreePath p = path;
        while (p != null && !(p.getLeaf() instanceof ClassTree)) {
            member = p.getLeaf();
            p = p.getParentPath();
        }
        if (member == null || p == null) {
            return true;
        }
        for (TreePath q = p.getParentPath(); q != null; q = q.getParentPath()) {
            Tree.Kind kind = q.getLeaf().getKind();
            if (kind != Tree.Kind.COMPILATION_UNIT && !(q.getLeaf() instanceof ClassTree)) {
                // The class is a local or anonymous class, in the scope of local variables.
                return true;
            }
        }
        Set<javax.lang.model.element.Name> namesInMember = declaredNames;
        if (namesInMember == null || !isSameTree(member, declaredNamesMember)) {
            Set<javax.lang.model.element.Name> newNames = new HashSet<>();
            new TreeScanner<Void, Void>() {
                @Override
                public Void visitVariable(VariableTree tree, Void p) {
                    newNames.add(tree.getName());
                    return super.visitVariable(tree, p);
                }
            }.scan(member, null);
            namesInMember = newNames;
            declaredNamesMember = member;
            declaredNames = newNames;
        }
        return namesInMember.contains(name);
    }

    /**
     * Returns true if the given trees are the same tree.
     *
     * @param t1 a tree
     * @param t2 a tree
     * @return true if {@code t1} and {@code t2} are the same tree
     */
    @SuppressWarnings("interning:not.interned") // AST node comparison
    private static boolean isSameTree(@Nullable Tree t1, @Nullable Tree t2) {
        return t1 == t2;
    }

    /**
     * Returns the given variable declaration if it declares a variable with the given name.
     *
     * @param tree a variable declaration
     * @param name a name
     * @return {@code tree} if it declares a variable named {@code name}, otherwise null
     */
    private static @Nullable VariableTree ifNamed(VariableTree tree, Name name) {
        return tree.getName() == name ? tree : null;
    }

    /**
     * Finds the class literal with name {@code name}.
     *
     * <p>The method adheres to all the rules of Java's scoping (while also considering the imports)
     * for name resolution.
     *
     * @param name the name of the class
     * @param path the tree path to the local scope
     * @return the element for the class
     */
    public Element findClass(String name, TreePath path) {
        Log.DiagnosticHandler discardDiagnosticHandler = newDiagnosticHandler();
        try {
            Env<AttrContext> env = getEnvForPath(path);
            return resolve(FIND_TYPE, env, names.fromString(name));
        } finally {
            log.popDiagnosticHandler(discardDiagnosticHandler);
        }
    }

    /**
     * Finds the class with name {@code name} in a given package.
     *
     * @param name the name of the class
     * @param pck the PackageSymbol for the package
     * @param path the tree path to the local scope
     * @return the {@code ClassSymbol} for the class if it is found, {@code null} otherwise
     */
    public @Nullable ClassSymbol findClassInPackage(String name, PackageSymbol pck, TreePath path) {
        Log.DiagnosticHandler discardDiagnosticHandler = newDiagnosticHandler();
        try {
            Env<AttrContext> env = getEnvForPath(path);
            final Element res;
            if (atLeastJava13) {
                res =
                        resolve(
                                FIND_IDENT_IN_PACKAGE,
                                null,
                                env,
                                pck,
                                names.fromString(name),
                                Kinds.KindSelector.TYP);
            } else {
                res =
                        resolve(
                                FIND_IDENT_IN_PACKAGE,
                                env,
                                pck,
                                names.fromString(name),
                                Kinds.KindSelector.TYP);
            }

            if (ElementUtils.isTypeElement(res)) {
                return (ClassSymbol) res;
            } else {
                return null;
            }
        } finally {
            log.popDiagnosticHandler(discardDiagnosticHandler);
        }
    }

    /**
     * Finds the method element for a given name and list of expected parameter types.
     *
     * <p>The method adheres to all the rules of Java's scoping (while also considering the imports)
     * for name resolution.
     *
     * <p>(This method takes into account autoboxing.)
     *
     * <p>This method is a wrapper around {@code com.sun.tools.javac.comp.Resolve.findMethod}.
     *
     * @param methodName name of the method to find
     * @param receiverType type of the receiver of the method
     * @param path tree path
     * @param argumentTypes types of arguments passed to the method call
     * @return the method element (if found)
     */
    // TODO: handle set calls of reflection API.
    @SuppressWarnings({
        "interning:argument.type.incompatible",
        "nullness:argument.type.incompatible"
    })
    public @Nullable ExecutableElement findMethod(
            String methodName,
            TypeMirror receiverType,
            TreePath path,
            java.util.List<TypeMirror> argumentTypes) {
        Log.DiagnosticHandler discardDiagnosticHandler = newDiagnosticHandler();
        try {
            Env<AttrContext> env = getEnvForPath(path);

            Type site = (Type) receiverType;
            Name name = names.fromString(methodName);
            ListBuffer<Type> argtypesBuf = new ListBuffer<>();
            for (TypeMirror a : argumentTypes) {
                argtypesBuf.append((Type) a);
            }
            List<Type> argtypes = argtypesBuf.toList();
            List<Type> typeargtypes = List.nil();
            boolean allowBoxing = true;
            boolean useVarargs = false;

            try {
                // For some reason we have to set our own method context, which is rather ugly.
                // TODO: find a nicer way to do this.
                Object methodContext = buildMethodContext();
                Object oldContext = RESOLVE_CURRENT_RESOLUTION_CONTEXT.get(resolve);
                RESOLVE_CURRENT_RESOLUTION_CONTEXT.set(resolve, methodContext);
                Element resolveResult =
                        resolve(
                                FIND_METHOD,
                                env,
                                site,
                                name,
                                argtypes,
                                typeargtypes,
                                allowBoxing,
                                useVarargs);
                RESOLVE_CURRENT_RESOLUTION_CONTEXT.set(resolve, oldContext);
                ExecutableElement methodResult;
                if (resolveResult.getKind() == ElementKind.METHOD
                        || resolveResult.getKind() == ElementKind.CONSTRUCTOR) {
                    methodResult = (ExecutableElement) resolveResult;
                } else if (resolveResult.getKind() == ElementKind.OTHER
                        && ACCESSERROR.isInstance(resolveResult)) {
                    // Return the inaccessible method that was found.
                    methodResult =
                            (ExecutableElement)
                                    invokeNoException(
                                            resolveResult, ACCESSERROR_ACCESS, null, null);
                } else {
                    methodResult = null;
                }
                return methodResult;
            } catch (Throwable t) {
                Error err =
                        new AssertionError(
                                String.format(
                                        "Unexpected reflection error in findMethod(%s, %s, ...,"
                                                + " %s)",
                                        methodName,
                                        receiverType,
                                        // path
                                        argumentTypes));
                err.initCause(t);
                throw err;
            }
        } finally {
            log.popDiagnosticHandler(discardDiagnosticHandler);
        }
    }

    /**
     * Build an instance of {@code Resolve$MethodResolutionContext}.
     *
     * @return a MethodResolutionContext
     * @throws InstantiationException if there is trouble constructing the instance
     * @throws IllegalAccessException if there is trouble constructing the instance
     * @throws InvocationTargetException if there is trouble constructing the instance
     */
    // TODO: handle set calls of reflection API.
    @SuppressWarnings("interning:argument.type.incompatible")
    protected Object buildMethodContext()
            throws InstantiationException, IllegalAccessException, InvocationTargetException {
        // Class is not accessible, instantiate reflectively.
        Object methodContext = METHOD_RESOLUTION_CONTEXT_CTOR.newInstance(resolve);
        // we need to also initialize the fields attrMode and step
        METHOD_RESOLUTION_CONTEXT_ATTR_MODE.set(methodContext, DeferredAttr.AttrMode.CHECK);
        @SuppressWarnings("rawtypes")
        List<?> phases = (List) RESOLVE_METHOD_RESOLUTION_STEPS.get(resolve);
        assert phases != null : "@AssumeAssertion(nullness): assumption";
        METHOD_RESOLUTION_CONTEXT_STEP.set(methodContext, phases.get(1));
        return methodContext;
    }

    /**
     * Creates a new {@code DiscardDiagnosticHandler} with the current {@code log}.
     *
     * @return a new {@code DiscardDiagnosticHandler} with the current {@code log}
     */
    private Log.DiscardDiagnosticHandler newDiagnosticHandler() {
        try {
            return NEW_DIAGNOSTIC_HANDLER.newInstance(log);
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
            throw new BugInCF(e);
        }
    }

    /**
     * Invoke the given method on the {@code resolve} field.
     *
     * @param method the method to called
     * @param args the arguments to the call
     * @return the result of invoking the method on {@code resolve} (as the receiver) and the
     *     arguments
     */
    private Symbol resolve(Method method, @Nullable Object... args) {
        return invokeNoException(resolve, method, args);
    }

    /**
     * Invoke a method reflectively. This is like {@code Method.invoke()}, but it throws no checked
     * exceptions.
     *
     * @param receiver the receiver
     * @param method the method to called
     * @param args the arguments to the call
     * @return the result of invoking the method on the receiver and arguments
     */
    private Symbol invokeNoException(Object receiver, Method method, @Nullable Object... args) {
        try {
            @SuppressWarnings("nullness") // assume arguments are OK
            @NonNull Symbol res = (Symbol) method.invoke(receiver, args);
            return res;
        } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
            throw new BugInCF(
                    e,
                    "Unexpected reflection error in invokeNoException(%s, %s, %s)",
                    receiver,
                    method,
                    Arrays.toString(args));
        }
    }
}
