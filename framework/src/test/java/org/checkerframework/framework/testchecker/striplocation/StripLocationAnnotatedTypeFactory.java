package org.checkerframework.framework.testchecker.striplocation;

import org.checkerframework.common.basetype.BaseAnnotatedTypeFactory;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.framework.testchecker.striplocation.quals.StripBottom;
import org.checkerframework.framework.testchecker.striplocation.quals.StripTop;
import org.checkerframework.framework.testchecker.striplocation.quals.StripUpperOnly;

import java.lang.annotation.Annotation;
import java.util.Set;

/** The annotated type factory for the striplocation test checker. */
public class StripLocationAnnotatedTypeFactory extends BaseAnnotatedTypeFactory {

    /**
     * Creates a new StripLocationAnnotatedTypeFactory.
     *
     * @param checker the checker
     */
    @SuppressWarnings("this-escape")
    public StripLocationAnnotatedTypeFactory(BaseTypeChecker checker) {
        super(checker);
        this.postInit();
    }

    @Override
    protected Set<Class<? extends Annotation>> createSupportedTypeQualifiers() {
        return getBundledTypeQualifiers(StripTop.class, StripUpperOnly.class, StripBottom.class);
    }
}
