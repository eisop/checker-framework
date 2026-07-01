package viewpointtest;

import com.sun.source.tree.ParameterizedTypeTree;

import org.checkerframework.checker.compilermsgs.qual.CompilerMessageKey;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.common.basetype.BaseTypeValidator;
import org.checkerframework.common.basetype.BaseTypeVisitor;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedDeclaredType;
import org.checkerframework.framework.type.AnnotatedTypeParameterBounds;
import org.checkerframework.framework.util.AnnotatedTypes;
import org.checkerframework.javacutil.TreeUtils;

import java.util.List;

import javax.lang.model.element.TypeElement;

/** The type validator for the Viewpoint Test Checker. */
public class ViewpointTestTypeValidator extends BaseTypeValidator {

    /** Error key for {@code @Lost} in adapted type parameter bounds. */
    private static final @CompilerMessageKey String LOST_IN_BOUNDS = "viewpointtest.lost.in.bounds";

    /** The annotated type factory for the Viewpoint Test Checker. */
    private final ViewpointTestAnnotatedTypeFactory viewpointTypeFactory;

    /**
     * Create a new ViewpointTestTypeValidator.
     *
     * @param checker the checker to which this validator belongs
     * @param visitor the visitor to which this validator belongs
     * @param atypeFactory the type factory to use
     */
    public ViewpointTestTypeValidator(
            BaseTypeChecker checker,
            BaseTypeVisitor<?> visitor,
            ViewpointTestAnnotatedTypeFactory atypeFactory) {
        super(checker, visitor, atypeFactory);
        viewpointTypeFactory = atypeFactory;
    }

    @Override
    protected Void visitParameterizedType(AnnotatedDeclaredType type, ParameterizedTypeTree tree) {
        if (TreeUtils.isDiamondTree(tree)) {
            return null;
        }
        TypeElement element = (TypeElement) type.getUnderlyingType().asElement();
        if (checker.shouldSkipUses(element)) {
            return null;
        }

        List<AnnotatedTypeParameterBounds> typeParamBounds =
                atypeFactory.typeVariablesFromUse(type, element);
        for (AnnotatedTypeParameterBounds atpb : typeParamBounds) {
            if (AnnotatedTypes.containsModifier(atpb.getUpperBound(), viewpointTypeFactory.LOST)
                    || AnnotatedTypes.containsModifier(
                            atpb.getLowerBound(), viewpointTypeFactory.LOST)) {
                checker.reportError(tree, LOST_IN_BOUNDS);
            }
        }

        return super.visitParameterizedType(type, tree);
    }
}
