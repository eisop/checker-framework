package org.checkerframework.javacutil;

import com.sun.source.tree.BlockTree;
import com.sun.source.tree.Tree;
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Map;

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
     * Determine the environment for the given path.
     *
     * <p>If the path ends with artificial trees, which are not part of the compilation unit, the
     * environment is that of the nearest tree before them; see {@link #pathInSource}.
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
        TreePath sourcePath = pathInSource(path);
        Tree sourceLeaf = sourcePath.getLeaf();
        @SuppressWarnings("interning:not.interned") // reference equality of trees
        boolean isSourcePath = sourceLeaf == leaf;
        Env<AttrContext> result = isSourcePath ? null : envCache.get(sourceLeaf);
        if (result == null) {
            result = computeEnvForPath(sourcePath);
            envCache.put(sourceLeaf, result);
        }
        if (!isSourcePath) {
            envCache.put(leaf, result);
        }
        return result;
    }

    /**
     * Returns {@code path}, or the longest prefix of it whose leaf is in the compilation unit if
     * the leaf of {@code path} is an artificial tree.
     *
     * <p>An artificial tree, such as one that the CFG builder creates for a compound assignment, is
     * not part of the compilation unit, but it can have a path: the path of the tree it was created
     * for, with the artificial tree appended. {@link Trees#getScope} cannot handle such a path. It
     * attributes a copy of the method body (or field initializer) at the start of the path, up to
     * the copy of the leaf of the path; it does not find the leaf, and returns the scope of the
     * method, which lacks the method's local variables. The scope of the tree that the artificial
     * tree was created for is the right one.
     *
     * <p>{@code getScope} uses only the start of the path, up to the copied tree, and the leaf. A
     * path need not be a chain of children: an artificial tree can be a child of another artificial
     * tree (an artificial cast of an artificial binary operation), and the CFG builder leaves the
     * {@code case} out of the path of a statement in a {@code switch}. So if each tree on the path
     * below the copied tree is a child of the tree before it, the leaf is in the compilation unit
     * and the path is returned. Otherwise the copied tree is searched for the trees on the path,
     * which costs no more than the copy that {@code getScope} makes, and the path is cut after the
     * deepest one found.
     *
     * <p>Both checks rely on {@link TreeScanner} visiting every child of a tree. It does not visit
     * the children of an {@link com.sun.source.tree.ErroneousTree}, so a path through one is cut
     * above it and resolves in a wider scope. This is accepted: a compilation unit with errors is
     * normally not type-checked, and the result is best effort for it. The other visit methods of
     * {@code TreeScanner} that scan nothing are for trees without children and {@code visitOther},
     * which visits javac-internal trees such as the {@code LetExpr} that lowering creates (checked
     * for JDK 8, 11, 17, 21, 25, and 26).
     *
     * @param path a tree path
     * @return {@code path}, or the longest prefix of it whose leaf is in the compilation unit
     */
    private static TreePath pathInSource(TreePath path) {
        // The trees on the path, from the compilation unit to the leaf.
        ArrayList<Tree> trees = new ArrayList<>();
        for (TreePath p = path; p != null; p = p.getParentPath()) {
            trees.add(p.getLeaf());
        }
        Collections.reverse(trees);
        int copied = indexOfCopiedTree(trees);
        int last = trees.size() - 1;
        if (copied < 0 || copied == last || isChain(trees, copied)) {
            return path;
        }
        int found = indexOfDeepestInSubtree(trees, copied);
        TreePath result = path;
        for (int i = last; i > found; i--) {
            result = result.getParentPath();
            assert result != null : "@AssumeAssertion(nullness): found >= 0";
        }
        return result;
    }

    // This file imports javac's List, for the arguments of javac's resolution methods, so
    // java.util.List is written fully qualified here; do not import it instead.

    /**
     * Returns the index of the tree that {@link Trees#getScope} copies and attributes for a path
     * with the given trees: the first block, or the initializer of the last variable before it.
     * This follows {@code JavacTrees.getAttrContext}.
     *
     * @param trees the trees on a path, from the compilation unit to the leaf
     * @return the index of the tree that {@code getScope} copies, or -1 if there is none
     */
    @SuppressWarnings("interning:not.interned") // reference equality of trees
    private static int indexOfCopiedTree(java.util.List<Tree> trees) {
        VariableTree variable = null;
        for (int i = 0; i < trees.size(); i++) {
            Tree tree = trees.get(i);
            if (tree instanceof BlockTree) {
                return i;
            } else if (tree instanceof VariableTree) {
                variable = (VariableTree) tree;
            } else if (variable != null && variable.getInitializer() == tree) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Returns true if each tree in {@code trees} after index {@code start} is a child of the tree
     * before it.
     *
     * @param trees the trees on a path, from the compilation unit to the leaf
     * @param start the index of the first tree to check the children of
     * @return true if each tree after {@code start} is a child of the tree before it
     */
    private static boolean isChain(java.util.List<Tree> trees, int start) {
        for (int i = start + 1; i < trees.size(); i++) {
            if (!isChild(trees.get(i - 1), trees.get(i))) {
                return false;
            }
        }
        return true;
    }

    /**
     * Returns true if {@code child} is a child of {@code parent}, that is, one of the trees that a
     * {@link TreeScanner} visits directly from {@code parent}.
     *
     * @param parent a tree
     * @param child a tree
     * @return true if {@code child} is a child of {@code parent}
     */
    private static boolean isChild(Tree parent, Tree child) {
        return Boolean.TRUE.equals(parent.accept(IS_CHILD_SCANNER, child));
    }

    /**
     * Visits a tree and returns true if the parameter is one of its children. It does not visit
     * deeper, so its cost is the number of children.
     */
    private static final TreeScanner<@Nullable Boolean, Tree> IS_CHILD_SCANNER =
            new TreeScanner<@Nullable Boolean, Tree>() {
                @Override
                @SuppressWarnings("interning:not.interned") // reference equality of trees
                public @Nullable Boolean scan(@Nullable Tree tree, Tree child) {
                    return tree == child;
                }

                @Override
                public @Nullable Boolean reduce(@Nullable Boolean r1, @Nullable Boolean r2) {
                    return Boolean.TRUE.equals(r1) || Boolean.TRUE.equals(r2);
                }
            };

    /**
     * Returns the largest index of a tree in {@code trees} that is in the subtree of the tree at
     * index {@code root}.
     *
     * @param trees the trees on a path, from the compilation unit to the leaf
     * @param root the index of the tree whose subtree to search
     * @return the largest index, at least {@code root}, of a tree in the subtree of {@code
     *     trees.get(root)}
     */
    private static int indexOfDeepestInSubtree(java.util.List<Tree> trees, int root) {
        int last = trees.size() - 1;
        IdentityHashMap<Tree, Integer> indexes = new IdentityHashMap<>();
        for (int i = root + 1; i <= last; i++) {
            indexes.put(trees.get(i), i);
        }
        int[] deepest = {root};
        new TreeScanner<Void, Void>() {
            @Override
            public Void scan(@Nullable Tree tree, Void p) {
                if (tree == null || deepest[0] == last) {
                    return null;
                }
                Integer index = indexes.get(tree);
                if (index != null && index > deepest[0]) {
                    deepest[0] = index;
                }
                return super.scan(tree, p);
            }
        }.scan(trees.get(root), null);
        return deepest[0];
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
     * @param name the name of the local variable
     * @param path the tree path to the local scope
     * @return the element for the local variable, {@code null} otherwise
     */
    public @Nullable VariableElement findLocalVariableOrParameter(String name, TreePath path) {
        Log.DiagnosticHandler discardDiagnosticHandler = newDiagnosticHandler();
        try {
            Env<AttrContext> env = getEnvForPath(path);
            // Either a VariableElement or a SymbolNotFoundError.
            Element res;
            if (atLeastJava28) {
                DiagnosticPosition pos = (DiagnosticPosition) path.getLeaf();
                res = resolve(FIND_VAR, pos, env, names.fromString(name), false);
            } else if (atLeastJava23) {
                DiagnosticPosition pos = (DiagnosticPosition) path.getLeaf();
                res = resolve(FIND_VAR, pos, env, names.fromString(name));
            } else {
                res = resolve(FIND_VAR, env, names.fromString(name));
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
