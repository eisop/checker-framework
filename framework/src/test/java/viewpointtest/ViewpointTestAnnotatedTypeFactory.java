package viewpointtest;

import com.sun.source.tree.MethodInvocationTree;

import org.checkerframework.common.basetype.BaseAnnotatedTypeFactory;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.framework.type.AbstractViewpointAdapter;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedExecutableType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedTypeVariable;
import org.checkerframework.framework.type.AnnotatedTypeParameterBounds;
import org.checkerframework.framework.type.QualifierHierarchy;
import org.checkerframework.javacutil.AnnotationBuilder;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.lang.model.element.AnnotationMirror;

import viewpointtest.quals.A;
import viewpointtest.quals.B;
import viewpointtest.quals.Bottom;
import viewpointtest.quals.Lost;
import viewpointtest.quals.PolyVP;
import viewpointtest.quals.ReceiverDependentQual;
import viewpointtest.quals.Top;

/** The annotated type factory for the Viewpoint Test Checker. */
public class ViewpointTestAnnotatedTypeFactory extends BaseAnnotatedTypeFactory {

    /** The {@link Top} annotation. */
    public final AnnotationMirror TOP = AnnotationBuilder.fromClass(elements, Top.class);

    /** The {@link Lost} annotation. */
    public final AnnotationMirror LOST = AnnotationBuilder.fromClass(elements, Lost.class);

    /**
     * Create a new ViewpointTestAnnotatedTypeFactory.
     *
     * @param checker the checker to which this annotated type factory belongs
     */
    @SuppressWarnings("this-escape")
    public ViewpointTestAnnotatedTypeFactory(BaseTypeChecker checker) {
        super(checker);
        this.postInit();
    }

    @Override
    protected Set<Class<? extends Annotation>> createSupportedTypeQualifiers() {
        return getBundledTypeQualifiers(
                A.class,
                B.class,
                Bottom.class,
                PolyVP.class,
                ReceiverDependentQual.class,
                Lost.class,
                Top.class);
    }

    @Override
    protected AbstractViewpointAdapter createViewpointAdapter() {
        return new ViewpointTestViewpointAdapter(this);
    }

    @Override
    public QualifierHierarchy createQualifierHierarchy() {
        return new ViewpointTestQualifierHierarchy(
                this.getSupportedTypeQualifiers(), elements, this);
    }

    /**
     * Returns the method type parameter bounds adapted to the viewpoint of a method invocation.
     *
     * @param tree a method invocation
     * @return the adapted method type parameter bounds
     */
    List<AnnotatedTypeParameterBounds> methodTypeVariablesFromUse(MethodInvocationTree tree) {
        AnnotatedExecutableType invokedMethod =
                methodFromUseWithoutTypeArgInference(tree).executableType;
        List<AnnotatedTypeVariable> typeVariables = invokedMethod.getTypeVariables();
        List<AnnotatedTypeParameterBounds> bounds = new ArrayList<>(typeVariables.size());
        for (AnnotatedTypeVariable typeVariable : typeVariables) {
            bounds.add(typeVariable.getBounds());
        }

        AnnotatedTypeMirror receiverType = getReceiverType(tree);
        if (viewpointAdapter != null && receiverType != null) {
            viewpointAdapter.viewpointAdaptTypeParameterBounds(receiverType, bounds);
        }
        return bounds;
    }
}
