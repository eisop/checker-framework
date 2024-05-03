import org.checkerframework.checker.immutability.qual.ReceiverDependantMutable;

public class DeepStaticRDMForbidden {

    // :: error: (static.receiverdependantmutable.forbidden)
    static <T extends @ReceiverDependantMutable Object> void foo(T p) {
        // :: error: (static.receiverdependantmutable.forbidden)
        p = null;
    }
}
