package viewpointtest;

import org.checkerframework.common.basetype.BaseAnnotatedTypeFactory;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.framework.type.AbstractViewpointAdapter;
import org.checkerframework.framework.type.QualifierHierarchy;

import java.lang.annotation.Annotation;
import java.util.Set;

import viewpointtest.quals.A;
import viewpointtest.quals.B;
import viewpointtest.quals.Bottom;
import viewpointtest.quals.Lost;
import viewpointtest.quals.PolyVP;
import viewpointtest.quals.ReceiverDependentQual;
import viewpointtest.quals.Top;

public class ViewpointTestAnnotatedTypeFactory extends BaseAnnotatedTypeFactory {

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
}
