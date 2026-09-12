package org.checkerframework.framework.testchecker.aliasedctor;

import org.checkerframework.common.basetype.BaseAnnotatedTypeFactory;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.framework.qual.TypeUseLocation;
import org.checkerframework.framework.util.defaults.QualifierDefaults;
import org.checkerframework.javacutil.AnnotationBuilder;

import java.lang.annotation.Annotation;
import java.lang.annotation.Documented;
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
     * Command-line option that makes this factory register an alias with an unsupported canonical
     * AnnotationMirror.
     */
    public static final String UNSUPPORTED_CANONICAL_MIRROR_OPTION =
            "aliasedCtorUnsupportedCanonicalMirror";

    /**
     * Command-line option that makes this factory register an alias with an unsupported canonical
     * Class.
     */
    public static final String UNSUPPORTED_CANONICAL_CLASS_OPTION =
            "aliasedCtorUnsupportedCanonicalClass";

    /**
     * Command-line option that makes this factory register an alias where the alias name is a
     * supported qualifier.
     */
    public static final String ALIAS_IS_QUALIFIER_NAME_OPTION = "aliasedCtorAliasIsQualifierName";

    /**
     * Command-line option that makes this factory register an alias where the alias class is a
     * supported qualifier.
     */
    public static final String ALIAS_IS_QUALIFIER_CLASS_OPTION = "aliasedCtorAliasIsQualifierClass";

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
        if (checker.hasOption(UNSUPPORTED_CANONICAL_MIRROR_OPTION)) {
            addAliasedTypeAnnotation(
                    "aliasedctor.UnusedAlias",
                    AnnotationBuilder.fromClass(elements, Documented.class));
        }
        if (checker.hasOption(UNSUPPORTED_CANONICAL_CLASS_OPTION)) {
            addAliasedTypeAnnotation("aliasedctor.UnusedAlias", Documented.class, true);
        }
        if (checker.hasOption(ALIAS_IS_QUALIFIER_NAME_OPTION)) {
            addAliasedTypeAnnotation(
                    AliasedCtorTop.class.getCanonicalName(),
                    AnnotationBuilder.fromClass(elements, AliasedCtorBottom.class));
        }
        if (checker.hasOption(ALIAS_IS_QUALIFIER_CLASS_OPTION)) {
            addAliasedTypeAnnotation(
                    AliasedCtorTop.class,
                    AnnotationBuilder.fromClass(elements, AliasedCtorBottom.class));
        }
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
