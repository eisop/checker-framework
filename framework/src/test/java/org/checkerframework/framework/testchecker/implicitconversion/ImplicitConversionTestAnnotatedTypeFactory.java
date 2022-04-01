package org.checkerframework.framework.testchecker.implicitconversion;

import org.checkerframework.common.basetype.BaseAnnotatedTypeFactory;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.framework.testchecker.implicitconversion.quals.Bottom;
import org.checkerframework.framework.testchecker.implicitconversion.quals.Top;

import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class ImplicitConversionTestAnnotatedTypeFactory extends BaseAnnotatedTypeFactory {

    public ImplicitConversionTestAnnotatedTypeFactory(BaseTypeChecker checker) {
        super(checker);
        postInit();
    }

    @Override
    protected Set<Class<? extends Annotation>> createSupportedTypeQualifiers() {
        return new HashSet<>(Arrays.asList(Top.class, Bottom.class));
    }
}
