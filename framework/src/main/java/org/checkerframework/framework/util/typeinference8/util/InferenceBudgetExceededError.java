package org.checkerframework.framework.util.typeinference8.util;

/**
 * Thrown when a single inference problem performs more bound-incorporation work than {@link
 * Java8InferenceContext#MAX_INCORPORATION_WORK} allows.
 *
 * <p>Java 8 type-argument inference incorporates bounds to a fixed point (JLS 18.3). For deeply
 * nested generic invocations this fixed point is reached only after work that grows roughly with
 * the cube of the nesting depth, so a single pathological (often machine-generated) invocation can
 * take many seconds or effectively hang the compiler. The budget caps that work and abandons
 * inference soundly instead.
 *
 * <p>This extends {@link Error} rather than {@link RuntimeException} on purpose: the abort has to
 * unwind past the {@code catch (Exception)} and {@code catch (FalseBoundException)} blocks in the
 * incorporation/resolution machinery (which would otherwise swallow it or retry the blowup) and be
 * handled only at the {@link
 * org.checkerframework.framework.util.typeinference8.DefaultTypeArgumentInference#inferTypeArgs
 * inferTypeArgs} entry point.
 */
public class InferenceBudgetExceededError extends Error {

    /** serialVersionUID */
    private static final long serialVersionUID = 1;

    /**
     * Creates an InferenceBudgetExceededError.
     *
     * @param work the amount of incorporation work performed when the budget was exceeded
     * @param budget the budget that was exceeded
     */
    public InferenceBudgetExceededError(int work, int budget) {
        super(
                "Type argument inference exceeded its work budget ("
                        + work
                        + " > "
                        + budget
                        + "); abandoning inference for this invocation.");
    }
}
