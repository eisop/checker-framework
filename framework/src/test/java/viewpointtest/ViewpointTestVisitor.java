package viewpointtest;

import com.sun.source.tree.AssignmentTree;
import com.sun.source.tree.CompoundAssignmentTree;
import com.sun.source.tree.NewClassTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.UnaryTree;

import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.common.basetype.BaseTypeVisitor;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.util.AnnotatedTypes;

/** The visitor for the Viewpoint Test Checker. */
public class ViewpointTestVisitor extends BaseTypeVisitor<ViewpointTestAnnotatedTypeFactory> {
    /**
     * Create a new ViewpointTestVisitor.
     *
     * @param checker the checker to which this visitor belongs
     */
    public ViewpointTestVisitor(BaseTypeChecker checker) {
        super(checker);
    }

    @Override
    public Void visitNewClass(NewClassTree tree, Void p) {
        AnnotatedTypeMirror type = atypeFactory.getAnnotatedType(tree);
        if (type.hasAnnotation(atypeFactory.TOP) || type.hasAnnotation(atypeFactory.LOST)) {
            checker.reportError(tree, "new.class.type.invalid", type.getAnnotations());
        }
        return super.visitNewClass(tree, p);
    }

    @Override
    public Void visitAssignment(AssignmentTree tree, Void p) {
        checkLostLhs(tree.getVariable(), tree);
        return super.visitAssignment(tree, p);
    }

    @Override
    public Void visitCompoundAssignment(CompoundAssignmentTree tree, Void p) {
        checkLostLhs(tree.getVariable(), tree);
        return super.visitCompoundAssignment(tree, p);
    }

    @Override
    public Void visitUnary(UnaryTree tree, Void p) {
        Tree.Kind treeKind = tree.getKind();
        if (treeKind == Tree.Kind.PREFIX_DECREMENT
                || treeKind == Tree.Kind.PREFIX_INCREMENT
                || treeKind == Tree.Kind.POSTFIX_DECREMENT
                || treeKind == Tree.Kind.POSTFIX_INCREMENT) {
            checkLostLhs(tree.getExpression(), tree);
        }
        return super.visitUnary(tree, p);
    }

    /**
     * Report an error if {@code variableTree}, interpreted as an assignment left-hand side,
     * contains {@code @Lost}.
     *
     * @param variableTree the assignment target to check
     * @param errorTree the tree on which to report the error
     */
    private void checkLostLhs(Tree variableTree, Tree errorTree) {
        AnnotatedTypeMirror variableType = atypeFactory.getAnnotatedTypeLhs(variableTree);
        if (AnnotatedTypes.containsModifier(variableType, atypeFactory.LOST)) {
            checker.reportError(errorTree, "viewpointtest.lost.lhs");
        }
    }
}
