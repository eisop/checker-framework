package org.checkerframework.framework.testchecker.elementsuppression;

import org.checkerframework.common.basetype.BaseAnnotatedTypeFactory;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.framework.testchecker.util.SubQual;
import org.checkerframework.framework.testchecker.util.SuperQual;

import java.lang.annotation.Annotation;
import java.util.LinkedHashSet;
import java.util.Set;

public class ElementSuppressionAnnotatedTypeFactory extends BaseAnnotatedTypeFactory {
    @SuppressWarnings("this-escape")
    public ElementSuppressionAnnotatedTypeFactory(BaseTypeChecker checker) {
        super(checker);
        this.postInit();
    }

    @Override
    protected Set<Class<? extends Annotation>> createSupportedTypeQualifiers() {
        Set<Class<? extends Annotation>> qualSet = new LinkedHashSet<>();
        qualSet.add(SubQual.class);
        qualSet.add(SuperQual.class);
        return qualSet;
    }
}
