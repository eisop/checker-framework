package org.checkerframework.framework.type.poly;

import org.checkerframework.framework.type.AnnotatedTypeFactory;

import javax.annotation.processing.ProcessingEnvironment;

/**
 * A generic implementation of qualifier polymorphism that distinguishes poly qualifiers based on
 * their argument values. Instead of being tied to a specific domain like nullness, this
 * implementation accepts any poly qualifier and a mapping from its argument values to concrete
 * instantiations.
 */
public class DefaultQualifierPolymorphismWithArgs extends DefaultQualifierPolymorphism {

    /**
     * Constructs a GenericQualifierPolymorphismWithArgs instance.
     *
     * @param env the processing environment
     * @param factory the annotated type factory for the current checker
     */
    public DefaultQualifierPolymorphismWithArgs(
            ProcessingEnvironment env, AnnotatedTypeFactory factory) {
        super(env, factory);
    }
}
