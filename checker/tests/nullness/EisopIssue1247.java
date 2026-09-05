import org.checkerframework.checker.nullness.qual.EnsuresNonNull;

public abstract class EisopIssue1247 {

    @EnsuresNonNull("#1")
    // Parameters of native method should be considered as effectively final
    native void m1(Object obj);

    @EnsuresNonNull("#1")
    // Parameters of abstract method should be considered as effectively final
    abstract void m2(Object p1);

    @EnsuresNonNull("#1")
    // :: error: (flowexpr.parameter.not.final)
    void m3(Object obj) {
        obj = new Object();
    }

    @EnsuresNonNull("#1")
    void m4(Object obj) {}

    class Inner extends EisopIssue1247 {
        @Override
        // Error because the @EnsuresNonNull("#1") is inherited from the outer class.
        // :: error: (flowexpr.parameter.not.final)
        void m2(Object obj) {
            obj = new Object();
        }

        // Error because the @EnsuresNonNull("#1") is inherited from the outer class.
        // :: error: (flowexpr.parameter.not.final)
        void m3(Object obj) {
            obj = new Object();
        }
    }
}
