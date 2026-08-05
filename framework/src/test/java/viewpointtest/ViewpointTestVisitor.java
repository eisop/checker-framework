package viewpointtest;

import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.NewClassTree;
import com.sun.source.tree.Tree;

import org.checkerframework.checker.compilermsgs.qual.CompilerMessageKey;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.common.basetype.BaseTypeVisitor;
import org.checkerframework.framework.type.AbstractViewpointAdapter;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedExecutableType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedTypeVariable;
import org.checkerframework.framework.util.AnnotatedTypes;

/** The visitor for the Viewpoint Test Checker. */
public class ViewpointTestVisitor extends BaseTypeVisitor<ViewpointTestAnnotatedTypeFactory> {

    /** Error key for {@code @Lost} in assignment targets. */
    private static final @CompilerMessageKey String LOST_LHS = "viewpointtest.lost.lhs";

    /** Error key for {@code @Lost} in adapted parameter types. */
    private static final @CompilerMessageKey String LOST_PARAMETER = "viewpointtest.lost.parameter";

    /** Error key for {@code @Lost} in adapted type parameter bounds. */
    private static final @CompilerMessageKey String LOST_IN_BOUNDS = "viewpointtest.lost.in.bounds";

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
        Void result = super.visitNewClass(tree, p);
        checkLostTypeParameterBounds(atypeFactory.constructorFromUse(tree).executableType, tree);
        return result;
    }

    @Override
    public Void visitMethodInvocation(MethodInvocationTree tree, Void p) {
        Void result = super.visitMethodInvocation(tree, p);
        checkLostTypeParameterBounds(atypeFactory.methodFromUse(tree).executableType, tree);
        return result;
    }

    @Override
    protected boolean commonAssignmentCheck(
            Tree varTree,
            ExpressionTree valueExpTree,
            @CompilerMessageKey String errorKey,
            Object... extraArgs) {
        boolean result = super.commonAssignmentCheck(varTree, valueExpTree, errorKey, extraArgs);
        AnnotatedTypeMirror varType = atypeFactory.getAnnotatedTypeLhs(varTree);
        if (AnnotatedTypes.containsModifier(varType, atypeFactory.LOST)) {
            checker.reportError(valueExpTree, LOST_LHS);
            result = false;
        }
        return result;
    }

    @Override
    protected boolean commonAssignmentCheck(
            AnnotatedTypeMirror varType,
            AnnotatedTypeMirror valueType,
            Tree valueExpTree,
            @CompilerMessageKey String errorKey,
            Object... extraArgs) {
        boolean result =
                super.commonAssignmentCheck(varType, valueType, valueExpTree, errorKey, extraArgs);
        if (AnnotatedTypes.containsModifier(varType, atypeFactory.LOST)) {
            if (errorKey.equals("argument.type.incompatible")
                    || errorKey.equals("varargs.type.incompatible")) {
                checker.reportError(valueExpTree, LOST_PARAMETER);
            } else if (errorKey.equals("unary.increment.type.incompatible")
                    || errorKey.equals("unary.decrement.type.incompatible")) {
                checker.reportError(valueExpTree, LOST_LHS);
            }
            result = false;
        }
        return result;
    }

    /**
     * Report an error if an invocation viewpoint-adapts a type parameter bound to {@code @Lost}.
     *
     * <p>The bounds are already viewpoint-adapted to the receiver by {@link
     * AbstractViewpointAdapter#viewpointAdaptMethod} and {@link
     * AbstractViewpointAdapter#viewpointAdaptConstructor}.
     *
     * @param invokedExecutable the type of the invoked method or constructor
     * @param tree the invocation to report an error on
     */
    private void checkLostTypeParameterBounds(
            AnnotatedExecutableType invokedExecutable, ExpressionTree tree) {
        for (AnnotatedTypeVariable typeVariable : invokedExecutable.getTypeVariables()) {
            if (AnnotatedTypes.containsModifier(typeVariable, atypeFactory.LOST)) {
                checker.reportError(tree, LOST_IN_BOUNDS);
                return;
            }
        }
    }
}
