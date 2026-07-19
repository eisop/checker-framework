package org.checkerframework.framework.testchecker.striplocation;

import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.common.basetype.BaseTypeVisitor;

/**
 * The visitor for the striplocation test checker. It opts in to stripping location-invalid
 * qualifiers when {@code -AstripInvalidLocationQualifiers} is passed, so that the same checker can
 * be exercised both with the opt-in off and on.
 */
public class StripLocationVisitor extends BaseTypeVisitor<StripLocationAnnotatedTypeFactory> {

    /**
     * Creates a new StripLocationVisitor.
     *
     * @param checker the checker
     */
    public StripLocationVisitor(BaseTypeChecker checker) {
        super(checker);
    }

    @Override
    protected boolean shouldStripInvalidLocationQualifiers() {
        return checker.hasOption("stripInvalidLocationQualifiers");
    }
}
