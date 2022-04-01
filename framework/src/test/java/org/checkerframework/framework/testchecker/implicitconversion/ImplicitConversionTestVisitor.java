package org.checkerframework.framework.testchecker.implicitconversion;

import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.common.basetype.BaseTypeVisitor;

public class ImplicitConversionTestVisitor
        extends BaseTypeVisitor<ImplicitConversionTestAnnotatedTypeFactory> {
    public ImplicitConversionTestVisitor(BaseTypeChecker checker) {
        super(checker);
    }

    @Override
    protected ImplicitConversionTestAnnotatedTypeFactory createTypeFactory() {
        return new ImplicitConversionTestAnnotatedTypeFactory(checker);
    }
}
