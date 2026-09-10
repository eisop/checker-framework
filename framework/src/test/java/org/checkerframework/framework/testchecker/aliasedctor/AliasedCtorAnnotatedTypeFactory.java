package org.checkerframework.framework.testchecker.aliasedctor;

import org.checkerframework.common.basetype.BaseAnnotatedTypeFactory;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.framework.qual.TypeUseLocation;
import org.checkerframework.framework.util.defaults.QualifierDefaults;
import org.checkerframework.javacutil.AnnotationBuilder;

import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import javax.lang.model.element.AnnotationMirror;

/**
 * Registers {@link AliasedCtorLegacyBottom} as an alias for {@link AliasedCtorBottom}. See {@code
 * framework/tests/aliasedctor} for what this is testing: that a constructor written with the alias,
 * not the canonical annotation, on its own declared type is still recognized when {@code
 * org.checkerframework.framework.util.AnnotatedTypes#copyOnlyExplicitConstructorAnnotations} copies
 * its explicit annotations to the type of a constructor reference ({@code Foo::new}).
 */
public class AliasedCtorAnnotatedTypeFactory extends BaseAnnotatedTypeFactory {

    /**
     * Creates a new AliasedCtorAnnotatedTypeFactory.
     *
     * @param checker the checker
     */
    @SuppressWarnings("this-escape")
    public AliasedCtorAnnotatedTypeFactory(BaseTypeChecker checker) {
        super(checker);
        addAliasedTypeAnnotation(
                AliasedCtorLegacyBottom.class,
                AnnotationBuilder.fromClass(elements, AliasedCtorBottom.class));
        this.postInit();
    }

    @Override
    protected void addCheckedCodeDefaults(QualifierDefaults defs) {
        AnnotationMirror top = AnnotationBuilder.fromClass(elements, AliasedCtorTop.class);
        defs.addCheckedCodeDefault(top, TypeUseLocation.OTHERWISE);
    }

    @Override
    protected Set<Class<? extends Annotation>> createSupportedTypeQualifiers() {
        return new HashSet<>(Arrays.asList(AliasedCtorTop.class, AliasedCtorBottom.class));
    }
}
