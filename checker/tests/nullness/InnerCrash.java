import org.checkerframework.checker.nullness.qual.Nullable;

class InnerCrash {
    class Inner {}

    static @Nullable InnerCrash getInnerCrash() {
        return null;
    }

    static void foo() {
        // :: error: (nullness.on.receiver)
        Object o = getInnerCrash().new Inner();
    }
}
