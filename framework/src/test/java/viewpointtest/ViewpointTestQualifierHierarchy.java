package viewpointtest;

import org.checkerframework.framework.type.GenericAnnotatedTypeFactory;
import org.checkerframework.framework.type.NoElementQualifierHierarchy;

import java.lang.annotation.Annotation;
import java.util.Collection;

import javax.lang.model.util.Elements;

/** The qualifier hierarchy for the Viewpoint Test Checker. */
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
}
