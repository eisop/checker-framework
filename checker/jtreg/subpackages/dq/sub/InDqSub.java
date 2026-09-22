package dq.sub;

/**
 * Package dq.sub's own FIELD default (NonNull, applyToSubpackages = false) shadows package dq's
 * FIELD default (Nullable) here. This is unchanged by fixing eisop#2037: shadowing for a package's
 * own elements is not in question, only whether a shadowed default still propagates to deeper
 * subpackages that this package's own default does not reach.
 */
public class InDqSub {
    Object f;
}
