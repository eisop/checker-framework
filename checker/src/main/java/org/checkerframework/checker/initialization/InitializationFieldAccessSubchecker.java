package org.checkerframework.checker.initialization;

import org.checkerframework.checker.compilermsgs.qual.CompilerMessageKey;
import org.checkerframework.common.basetype.BaseTypeChecker;

/**
 * Part of the freedom-before-commitment type system.
 *
 * <p>This checker does not actually do any type checking. It exists to provide its parent checker
 * (the {@link InitializationChecker#getTargetCheckerClass()}) with declared initialization
 * qualifiers via the {@link InitializationFieldAccessTreeAnnotator}.
 *
 * <p>Additionally, this checker performs the flow-sensitive type refinement for the fbc type
 * system, which is necessary to avoid reporting follow-up errors related to initialization (see the
 * AssignmentDuringInitialization test case). To avoid performing the same type refinement twice,
 * the InitializationChecker performs no refinement, instead reusing the results from this checker.
 *
 * @see InitializationChecker
 */
public class InitializationFieldAccessSubchecker extends BaseTypeChecker {

    /** Default constructor for InitializationFieldAccessSubchecker. */
    public InitializationFieldAccessSubchecker() {}

    // Suppress all errors and warnings, since they are also reported by the InitializationChecker

    @Override
    public void reportError(Object source, @CompilerMessageKey String messageKey, Object... args) {
        // do nothing
    }

    @Override
    public void reportWarning(
            Object source, @CompilerMessageKey String messageKey, Object... args) {
        // do nothing
    }
}
