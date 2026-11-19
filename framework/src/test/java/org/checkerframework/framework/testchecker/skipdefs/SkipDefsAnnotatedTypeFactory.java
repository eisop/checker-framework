package org.checkerframework.framework.testchecker.skipdefs;

import org.checkerframework.common.basetype.BaseAnnotatedTypeFactory;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.framework.testchecker.util.SubQual;
import org.checkerframework.framework.testchecker.util.SuperQual;

import java.lang.annotation.Annotation;
import java.util.LinkedHashSet;
import java.util.Set;

/** The annotated type factory for {@link SkipDefsChecker}. */
public class SkipDefsAnnotatedTypeFactory extends BaseAnnotatedTypeFactory {

    /**
     * Creates a new SkipDefsAnnotatedTypeFactory.
     *
     * @param checker the checker
     */
    @SuppressWarnings("this-escape")
    public SkipDefsAnnotatedTypeFactory(BaseTypeChecker checker) {
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
