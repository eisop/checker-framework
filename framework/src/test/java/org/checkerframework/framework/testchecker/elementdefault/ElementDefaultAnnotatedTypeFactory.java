package org.checkerframework.framework.testchecker.elementdefault;

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
import javax.lang.model.element.PackageElement;

/**
 * Calls {@link QualifierDefaults#addElementDefault} directly on package {@code elementdefault.pkg},
 * defaulting its {@code FIELD} locations to {@link ElementDefaultBottom} -- the same way a written
 * {@code @DefaultQualifier(ElementDefaultBottom.class, TypeUseLocation.FIELD)} on that package's
 * package-info.java would, but through the programmatic API instead. See {@code
 * framework/tests/elementdefault} for what this is testing: that the default set this way still
 * reaches {@code elementdefault.pkg.sub}, a subpackage with no default of its own.
 */
public class ElementDefaultAnnotatedTypeFactory extends BaseAnnotatedTypeFactory {

    /**
     * Creates a new ElementDefaultAnnotatedTypeFactory.
     *
     * @param checker the checker
     */
    @SuppressWarnings("this-escape")
    public ElementDefaultAnnotatedTypeFactory(BaseTypeChecker checker) {
        super(checker);
        this.postInit();
    }

    @Override
    protected void addCheckedCodeDefaults(QualifierDefaults defs) {
        AnnotationMirror top = AnnotationBuilder.fromClass(elements, ElementDefaultTop.class);
        defs.addCheckedCodeDefault(top, TypeUseLocation.OTHERWISE);

        PackageElement pkg = elements.getPackageElement("elementdefault.pkg");
        if (pkg != null) {
            AnnotationMirror bottom =
                    AnnotationBuilder.fromClass(elements, ElementDefaultBottom.class);
            defs.addElementDefault(pkg, bottom, TypeUseLocation.FIELD);
        }
    }

    @Override
    protected Set<Class<? extends Annotation>> createSupportedTypeQualifiers() {
        return new HashSet<>(Arrays.asList(ElementDefaultTop.class, ElementDefaultBottom.class));
    }
}
