package org.checkerframework.framework.testchecker.errorcount;

import org.checkerframework.framework.testchecker.elementsuppression.ElementSuppressionChecker;

import javax.tools.Diagnostic;

/**
 * A checker that prints {@link #getErrorCount} when type processing is over, to test the count.
 *
 * <p>It extends an existing test checker to inherit its errors: one on each class whose name starts
 * with "ReportOnMe".
 */
public class ErrorCountChecker extends ElementSuppressionChecker {

    @Override
    public void typeProcessingOver() {
        message(Diagnostic.Kind.NOTE, "errors reported: %d", getErrorCount());
        super.typeProcessingOver();
    }
}
