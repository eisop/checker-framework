package viewpointtest;

import com.sun.source.tree.AssignmentTree;
import com.sun.source.tree.NewClassTree;

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
        AnnotatedTypeMirror variableType = atypeFactory.getAnnotatedType(tree.getVariable());
        if (AnnotatedTypes.containsModifier(variableType, atypeFactory.LOST)) {
            checker.reportError(tree, "viewpointtest.lost.lhs");
        }
        return super.visitAssignment(tree, p);
    }
}
