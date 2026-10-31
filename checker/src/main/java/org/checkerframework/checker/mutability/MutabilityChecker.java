package org.checkerframework.checker.mutability;

import org.checkerframework.checker.initialization.InitializationChecker;
import org.checkerframework.checker.initialization.InitializationVisitor;
import org.checkerframework.common.basetype.BaseTypeChecker;

import javax.annotation.processing.SupportedOptions;

/** The mutability checker. */
@SupportedOptions({"assumeInitialized"})
public class MutabilityChecker extends InitializationChecker {

    /** Default constructor for MutabilityChecker. */
    public MutabilityChecker() {}

    @Override
    public Class<? extends BaseTypeChecker> getTargetCheckerClass() {
        return MutabilityNoInitSubchecker.class;
    }

    @Override
    public boolean checkPrimitives() {
        return false;
    }

    @Override
    protected InitializationVisitor createSourceVisitor() {
        return new MutabilityInitializationVisitor(this);
    }
}
