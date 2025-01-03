package org.checkerframework.checker.pico;

import org.checkerframework.checker.initialization.InitializationChecker;
import org.checkerframework.checker.initialization.InitializationVisitor;
import org.checkerframework.common.basetype.BaseTypeChecker;

import javax.annotation.processing.SupportedOptions;

/** The PICO checker. */
@SupportedOptions({"abstractStateOnly", "immutableDefault"})
public class PICOChecker extends InitializationChecker {

    /** Default constructor for PICOChecker. */
    public PICOChecker() {}

    @Override
    public Class<? extends BaseTypeChecker> getTargetCheckerClass() {
        return PICONoInitSubchecker.class;
    }

    @Override
    public boolean checkPrimitives() {
        return true;
    }

    @Override
    protected InitializationVisitor createSourceVisitor() {
        return new PICOInitializationVisitor(this);
    }
}
