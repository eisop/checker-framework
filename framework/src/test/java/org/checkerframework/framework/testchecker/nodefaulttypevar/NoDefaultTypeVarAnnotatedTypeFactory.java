package org.checkerframework.framework.testchecker.nodefaulttypevar;

import org.checkerframework.common.basetype.BaseAnnotatedTypeFactory;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.framework.testchecker.nodefaulttypevar.quals.Bottom;
import org.checkerframework.framework.testchecker.nodefaulttypevar.quals.Top;
import org.checkerframework.framework.type.AnnotatedTypeMirror;

import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;

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
    protected Set<Class<? extends Annotation>> createSupportedTypeQualifiers() {
        return new LinkedHashSet<>(Arrays.asList(Top.class, Bottom.class));
    }
}
