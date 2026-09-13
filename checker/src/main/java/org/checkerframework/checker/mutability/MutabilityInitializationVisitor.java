package org.checkerframework.checker.mutability;

import org.checkerframework.checker.initialization.InitializationAnnotatedTypeFactory;
import org.checkerframework.checker.initialization.InitializationVisitor;
import org.checkerframework.common.basetype.BaseTypeChecker;

/**
 * The InitializationVisitor for the mutability type system. This class is mainly created to
 * override createTypeFactory() method for the purpose of using
 * MutabilityInitializationAnnotatedTypeFactory.
 */
public class MutabilityInitializationVisitor extends InitializationVisitor {
    /**
     * Constructor for MutabilityInitializationVisitor.
     *
     * @param checker the BaseTypeChecker this visitor works with
     */
    public MutabilityInitializationVisitor(BaseTypeChecker checker) {
        super(checker);
    }

    @Override
    protected InitializationAnnotatedTypeFactory createTypeFactory() {
        return new MutabilityInitializationAnnotatedTypeFactory(checker);
    }
}
