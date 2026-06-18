package org.checkerframework.framework.type;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.framework.util.QualifierKind;
import org.checkerframework.javacutil.AnnotationBuilder;
import org.checkerframework.javacutil.AnnotationUtils;
import org.checkerframework.javacutil.TreeUtils;

import java.lang.annotation.Annotation;
import java.util.Collection;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.util.Elements;

/**
 * A qualifier hierarchy that uses the element values of annotations to distinguish different poly
 * qualifier. For example, @PolyNull("1") and @PolyNull("2") are different poly qualifiers and
 * should be substituted with the corresponding instantiation.
 */
public class ElementForPolyQualifierHierarchy extends MostlyNoElementQualifierHierarchy {

    /**
     * Creates a MostlyNoElementQualifierHierarchy from the given classes.
     *
     * @param qualifierClasses classes of annotations that are the qualifiers for this hierarchy
     * @param elements element utils
     * @param atypeFactory the associated type factory
     */
    public ElementForPolyQualifierHierarchy(
            Collection<Class<? extends Annotation>> qualifierClasses,
            Elements elements,
            GenericAnnotatedTypeFactory<?, ?, ?, ?> atypeFactory) {
        super(qualifierClasses, elements, atypeFactory);
    }

    @Override
    public @Nullable AnnotationMirror getPolymorphicAnnotation(AnnotationMirror start) {
        QualifierKind polyKind = getQualifierKind(start).getPolymorphic();
        if (polyKind == null) {
            return null;
        }
        return AnnotationBuilder.fromClass(elements, polyKind.getAnnotationClass());
    }

    @Override
    protected boolean isSubtypeWithElements(
            AnnotationMirror subAnno,
            QualifierKind subKind,
            AnnotationMirror superAnno,
            QualifierKind superKind) {

        // Extract the "value" element from each poly qualifier.
        String subValue =
                AnnotationUtils.getElementValue(
                        subAnno,
                        TreeUtils.getMethod(
                                subKind.getPolymorphic().getAnnotationClass(),
                                "value",
                                0,
                                atypeFactory.getProcessingEnv()),
                        String.class,
                        "");
        String superValue =
                AnnotationUtils.getElementValue(
                        superAnno,
                        TreeUtils.getMethod(
                                subKind.getPolymorphic().getAnnotationClass(),
                                "value",
                                0,
                                atypeFactory.getProcessingEnv()),
                        String.class,
                        "");
        return subValue.equals(superValue);
    }

    @Override
    protected AnnotationMirror leastUpperBoundWithElements(
            AnnotationMirror a1,
            QualifierKind qualifierKind1,
            AnnotationMirror a2,
            QualifierKind qualifierKind2,
            QualifierKind lubKind) {
        return atypeFactory.getQualifierHierarchy().getTopAnnotation(a1);
    }

    @Override
    protected AnnotationMirror greatestLowerBoundWithElements(
            AnnotationMirror a1,
            QualifierKind qualifierKind1,
            AnnotationMirror a2,
            QualifierKind qualifierKind2,
            QualifierKind glbKind) {
        return atypeFactory.getQualifierHierarchy().getBottomAnnotation(a1);
    }
}
