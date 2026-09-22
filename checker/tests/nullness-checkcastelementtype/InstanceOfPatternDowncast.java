// @below-java17-jdk-skip-test
// isTypeCastSafe (fixed for downcasts in BaseTypeVisitor) is also called from
// visitInstanceOf's binding-pattern check, not just from checkTypecastSafety. Test that call
// site too, so that a downcast-shaped instanceof pattern does not crash the way a downcast cast
// expression used to.

import org.checkerframework.checker.nullness.qual.Nullable;

public class InstanceOfPatternDowncast {
    interface Supplier<T extends @Nullable Object> {}

    interface SubSupplier<T extends @Nullable Object> extends Supplier<T> {}

    void test(Supplier<@Nullable String> supplier) {
        // Narrows the type argument's nullness (@Nullable String to String); with
        // -AcheckCastElementType, visitInstanceOf's binding-pattern check issues a warning.
        // :: warning: (instanceof.pattern.unsafe)
        if (supplier instanceof SubSupplier<String> sub) {
            sub.toString();
        }
    }
}
