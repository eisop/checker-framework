package org.checkerframework.framework.type.poly;

import org.checkerframework.framework.type.AnnotatedTypeFactory;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.javacutil.AnnotationMirrorMap;

import java.util.Map;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.AnnotationMirror;

/**
 * Default implementation of {@link AbstractQualifierPolymorphism}. The polymorphic qualifiers for a
 * checker that uses this class are found by searching all supported qualifiers. Instantiations of a
 * polymorphic qualifier are combined using lub.
 */
public class DefaultQualifierPolymorphism extends AbstractQualifierPolymorphism {

    /**
     * Creates a {@link DefaultQualifierPolymorphism} instance that uses {@code factory} for
     * querying type qualifiers and for getting annotated types.
     *
     * @param env the processing environment
     * @param factory the factory for the current checker
     */
    public DefaultQualifierPolymorphism(ProcessingEnvironment env, AnnotatedTypeFactory factory) {
        super(env, factory);

        for (AnnotationMirror top : topQuals) {
            AnnotationMirror poly = qualHierarchy.getPolymorphicAnnotation(top);
            if (poly != null) {
                polyQuals.put(poly, top);
            }
        }
    }

    @Override
    protected void replace(
            AnnotatedTypeMirror type, AnnotationMirrorMap<AnnotationMirror> replacements) {
        if (replacements.isEmpty()) {
            // If the 'replacements' map is empty, it is likely a case where a method with
            // a varargs parameter was invoked with zero varargs actuals.
            // In this case, the polymorphic qualifiers should be replaced with the top type in
            // the qualifier hierarchy, since there is no further information to deduce.
            for (AnnotationMirror top : topQuals) {
                AnnotationMirror effectiveAnno = type.getEffectiveAnnotationInHierarchy(top);
                if (effectiveAnno != null && qualHierarchy.isPolymorphicQualifier(effectiveAnno)) {
                    replacements.put(effectiveAnno, top);
                }
            }
        }
        for (Map.Entry<AnnotationMirror, AnnotationMirror> pqentry : replacements.entrySet()) {
            AnnotationMirror poly = pqentry.getKey();
            if (type.hasAnnotation(poly)) {
                type.removeAnnotation(poly);
                AnnotationMirror qual;
                if (polyInstantiationForQualifierParameter.containsKey(poly)) {
                    qual = polyInstantiationForQualifierParameter.get(poly);
                } else {
                    qual = pqentry.getValue();
                }
                type.replaceAnnotation(qual);
            }
        }
    }

    /**
     * This implementation combines the two annotations using the least upper bound.
     *
     * <p>{@inheritDoc}
     */
    @Override
    protected AnnotationMirror combine(
            AnnotationMirror polyQual, AnnotationMirror a1, AnnotationMirror a2) {
        if (a1 == null) {
            return a2;
        } else if (a2 == null) {
            return a1;
        }
        return qualHierarchy.leastUpperBoundQualifiersOnly(a1, a2);
    }
}
