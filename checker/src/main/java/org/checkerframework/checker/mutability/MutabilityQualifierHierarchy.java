package org.checkerframework.checker.mutability;

import org.checkerframework.framework.type.GenericAnnotatedTypeFactory;
import org.checkerframework.framework.type.NoElementQualifierHierarchy;

import java.lang.annotation.Annotation;
import java.util.Collection;

import javax.lang.model.util.Elements;

/** A qualifier hierarchy for the mutability checker. */
public class MutabilityQualifierHierarchy extends NoElementQualifierHierarchy {

    /**
     * Creates a MutabilityQualifierHierarchy from the given classes.
     *
     * @param qualifierClasses classes of annotations that are the qualifiers
     * @param elements element utils
     * @param atypeFactory the associated type factory
     */
    public MutabilityQualifierHierarchy(
            Collection<Class<? extends Annotation>> qualifierClasses,
            Elements elements,
            GenericAnnotatedTypeFactory<?, ?, ?, ?> atypeFactory) {
        super(qualifierClasses, elements, atypeFactory);
    }
}
