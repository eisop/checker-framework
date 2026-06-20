package viewpointtest;

import com.sun.source.tree.NewClassTree;

import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.common.basetype.BaseTypeVisitor;
import org.checkerframework.framework.type.AnnotatedTypeMirror;

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
        AnnotatedTypeMirror Type = atypeFactory.getAnnotatedType(tree);
        if (Type.hasAnnotation(atypeFactory.TOP) || Type.hasAnnotation(atypeFactory.LOST)) {
            checker.reportError(tree, "new.class.type.invalid", Type.getAnnotations());
        }
        return super.visitNewClass(tree, p);
    }
}
