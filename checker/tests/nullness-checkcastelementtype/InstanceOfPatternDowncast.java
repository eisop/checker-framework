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
        // TODO: this narrows the type argument's nullness (@Nullable String to String) the same
        // way Downcast.java's downcastUnsafe does via an explicit cast, but -AcheckCastElementType
        // does not warn here. visitInstanceOf's binding-pattern check shares isTypeCastSafe with
        // the cast check, so this looks like a pre-existing, separate gap in that call site
        // (not something this file's downcast fix introduced or fixes) rather than a difference
        // in what is safe.
        if (supplier instanceof SubSupplier<String> sub) {
            sub.toString();
        }
    }
}
