package org.checkerframework.framework.testchecker.nodefaulttypevar;

import org.checkerframework.common.basetype.BaseAnnotatedTypeFactory;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.framework.type.AnnotatedTypeMirror;

/**
 * AnnotatedTypeFactory for NoDefaultTypeVarChecker. Disables default annotations to reproduce the
 * missing annotation scenario.
 */
public class NoDefaultTypeVarAnnotatedTypeFactory extends BaseAnnotatedTypeFactory {
    /**
     * Creates a new NoDefaultTypeVarAnnotatedTypeFactory.
     *
     * @param checker the checker
     */
    @SuppressWarnings("this-escape")
    public NoDefaultTypeVarAnnotatedTypeFactory(BaseTypeChecker checker) {
        super(checker);
        this.postInit();
    }

    @Override
    public void addDefaultAnnotations(AnnotatedTypeMirror type) {
        // Disable defaulting for this test checker so bare type variables reach GLB computation
        // without a primary annotation.
    }

    @Override
    protected java.util.Set<Class<? extends java.lang.annotation.Annotation>>
            createSupportedTypeQualifiers() {
        return new java.util.LinkedHashSet<>(
                java.util.Arrays.asList(
                        org.checkerframework.framework.testchecker.nodefaulttypevar.quals.Top.class,
                        org.checkerframework.framework.testchecker.nodefaulttypevar.quals.Bottom
                                .class));
    }
}
