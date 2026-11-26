package org.checkerframework.checker.initialization;

import com.sun.source.tree.ClassTree;
import com.sun.source.tree.Tree;
import com.sun.source.util.TreePath;
import com.sun.source.util.TreePathScanner;

import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.common.basetype.BaseTypeVisitor;

/** The visitor for the {@link InitializationFieldAccessSubchecker}. */
public class InitializationFieldAccessVisitor
        extends BaseTypeVisitor<InitializationFieldAccessAnnotatedTypeFactory> {

    /** The value of the assumeInitialized option. */
    private final boolean assumeInitialized;

    /**
     * Create an InitializationFieldAccessVisitor.
     *
     * @param checker the initialization field-access checker
     */
    public InitializationFieldAccessVisitor(BaseTypeChecker checker) {
        super(checker);
        assumeInitialized = checker.hasOption("assumeInitialized");
    }

    /**
     * Visits the classes nested in {@code classTree}, at any depth, and nothing else.
     *
     * <p>As stated in the documentation for the {@link InitializationFieldAccessSubchecker} and
     * {@link InitializationChecker}, this checker performs the flow analysis, which {@link
     * BaseTypeVisitor#visitClass} starts, but does not perform any type checking: every error and
     * warning that it could report is discarded by its checker and reported by the {@link
     * InitializationChecker} instead. So this method does not scan the members of the class with
     * this visitor, which would type-check them. It only looks for nested classes, because each of
     * them has to be visited too: the flow analysis of a class does not reach a class that is
     * nested in an annotation declaration.
     */
    @Override
    public void processClassTree(ClassTree classTree) {
        if (assumeInitialized) {
            return;
        }
        // BaseTypeVisitor#visitClass set the visitor path to the path of classTree.
        TreePath classPath = atypeFactory.getVisitorTreePath();
        NestedClassScanner scanner = new NestedClassScanner();
        for (Tree member : classTree.getMembers()) {
            scanner.scan(new TreePath(classPath, member), null);
        }
    }

    /**
     * Calls {@link BaseTypeVisitor#visitClass} of the enclosing visitor on the outermost classes in
     * the scanned tree. That call uses a new scanner for the classes nested in such a class, so a
     * scanner never visits a class from within itself.
     */
    private class NestedClassScanner extends TreePathScanner<Void, Void> {

        /** Creates a NestedClassScanner. */
        NestedClassScanner() {}

        @Override
        public Void visitClass(ClassTree tree, Void p) {
            // The visitor is not positioned at the class, so it looks up the path of the class.
            checker.getTreePathCacher().addPath(tree, getCurrentPath());
            InitializationFieldAccessVisitor.this.visitClass(tree, null);
            return null;
        }
    }
}
