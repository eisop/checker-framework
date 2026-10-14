package org.checkerframework.framework.testchecker.processingover;

import org.checkerframework.framework.source.SupportedOptions;
import org.checkerframework.framework.testchecker.variablenamedefault.VariableNameDefaultChecker;
import org.checkerframework.javacutil.UserError;

/**
 * A checker whose {@link #typeProcessingOver} throws, to test that the throwable is reported as a
 * compiler diagnostic instead of propagating out of the annotation processor.
 *
 * <p>It extends an existing test checker only to inherit a working qualifier hierarchy; the type
 * system it implements is irrelevant to the test.
 */
@SupportedOptions("throwInTypeProcessingOver")
public class ThrowingTypeProcessingOverChecker extends VariableNameDefaultChecker {

    @Override
    public void typeProcessingOver() {
        if (hasOption("throwInTypeProcessingOver")) {
            throw new UserError("Simulated error from typeProcessingOver.");
        }
        super.typeProcessingOver();
    }
}
