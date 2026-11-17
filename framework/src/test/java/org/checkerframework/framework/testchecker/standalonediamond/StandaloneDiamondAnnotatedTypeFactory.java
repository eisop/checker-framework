package org.checkerframework.framework.testchecker.standalonediamond;

import org.checkerframework.common.basetype.BaseAnnotatedTypeFactory;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.common.subtyping.qual.Unqualified;
import org.checkerframework.framework.qual.TypeUseLocation;
import org.checkerframework.framework.testchecker.util.Encrypted;
import org.checkerframework.framework.testchecker.util.PolyEncrypted;
import org.checkerframework.framework.util.defaults.QualifierDefaults;
import org.checkerframework.javacutil.AnnotationBuilder;

import java.lang.annotation.Annotation;
import java.util.Set;

/** The annotated type factory for the standalone diamond test checker. */
public class StandaloneDiamondAnnotatedTypeFactory extends BaseAnnotatedTypeFactory {

    /**
     * Creates a new StandaloneDiamondAnnotatedTypeFactory.
     *
     * @param checker the checker
     */
    @SuppressWarnings("this-escape")
    public StandaloneDiamondAnnotatedTypeFactory(BaseTypeChecker checker) {
        super(checker);
        this.postInit();
    }

    @Override
    protected Set<Class<? extends Annotation>> createSupportedTypeQualifiers() {
        return getBundledTypeQualifiers(Unqualified.class, Encrypted.class, PolyEncrypted.class);
    }

    @Override
    protected void addCheckedCodeDefaults(QualifierDefaults defs) {
        // As in the Subtyping Checker: @Unqualified, except for the lower bounds of @Encrypted.
        defs.addCheckedCodeDefault(
                AnnotationBuilder.fromClass(elements, Unqualified.class),
                TypeUseLocation.OTHERWISE);
        defs.addCheckedCodeDefault(
                AnnotationBuilder.fromClass(elements, Encrypted.class),
                TypeUseLocation.LOWER_BOUND);
    }
}
