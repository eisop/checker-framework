package viewpointtest;

import org.checkerframework.framework.type.GenericAnnotatedTypeFactory;
import org.checkerframework.framework.type.NoElementQualifierHierarchy;
import org.checkerframework.framework.type.QualifierHierarchy;

import java.lang.annotation.Annotation;
import java.util.Collection;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.util.Elements;

import viewpointtest.quals.Bottom;
import viewpointtest.quals.Lost;

/** The {@link QualifierHierarchy} for the Viewpoint Test Checker. */
public class ViewpointTestQualifierHierarchy extends NoElementQualifierHierarchy {
    /**
     * Creates a ViewpointTestQualifierHierarchy from the given classes.
     *
     * @param qualifierClasses classes of annotations that are the qualifiers
     * @param elements element utils
     * @param atypeFactory the associated type factory
     */
    public ViewpointTestQualifierHierarchy(
            Collection<Class<? extends Annotation>> qualifierClasses,
            Elements elements,
            GenericAnnotatedTypeFactory<?, ?, ?, ?> atypeFactory) {
        super(qualifierClasses, elements, atypeFactory);
    }

    @Override
    public boolean isSubtypeQualifiers(AnnotationMirror subAnno, AnnotationMirror superAnno) {
        // Lost is not reflexive and the only subtype is Bottom.
        if (atypeFactory.areSameByClass(superAnno, Lost.class)
                && !atypeFactory.areSameByClass(subAnno, Bottom.class)) {
            return false;
        }
        return super.isSubtypeQualifiers(subAnno, superAnno);
    }
}
