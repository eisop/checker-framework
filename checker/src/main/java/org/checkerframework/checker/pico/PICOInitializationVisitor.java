package org.checkerframework.checker.pico;

import org.checkerframework.checker.initialization.InitializationAnnotatedTypeFactory;
import org.checkerframework.checker.initialization.InitializationVisitor;
import org.checkerframework.common.basetype.BaseTypeChecker;

/**
 * The InitializationVisitor for the PICO type system. This class is mainly created to override
 * createTypeFactory() method for the purpose of using PICOInitializationAnnotatedTypeFactory.
 */
public class PICOInitializationVisitor extends InitializationVisitor {
    /**
     * Constructor for PICOInitializationVisitor.
     *
     * @param checker the BaseTypeChecker this visitor works with
     */
    public PICOInitializationVisitor(BaseTypeChecker checker) {
        super(checker);
    }

    @Override
    protected InitializationAnnotatedTypeFactory createTypeFactory() {
        return new PICOInitializationAnnotatedTypeFactory(checker);
    }
}
