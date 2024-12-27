import org.checkerframework.checker.pico.qual.Immutable;
import org.checkerframework.checker.pico.qual.Mutable;
import org.checkerframework.checker.pico.qual.ReceiverDependentMutable;

@ReceiverDependentMutable public class RDMClass {
    @ReceiverDependentMutable RDMClass() {}

    @Immutable RDMClass(int dummy) {}

    @Mutable RDMClass(boolean dummy) {}

    void RDMMethod(@ReceiverDependentMutable RDMClass this) {}

    void ImmutableMethod(@Immutable RDMClass this) {}

    void MutableMethod(@Mutable RDMClass this) {}

    void testMethodInvocation() {
        RDMClass immutInstance1 = new @Immutable RDMClass();
        RDMClass immutInstance2 = new @Immutable RDMClass(1);
        // :: error: (constructor.invocation.invalid)
        RDMClass immutInstance3 = new @Immutable RDMClass(true);
        RDMClass mutInstance1 = new @Mutable RDMClass();
        // :: error: (constructor.invocation.invalid)
        RDMClass mutInstance2 = new @Mutable RDMClass(1);
        RDMClass mutInstance3 = new @Mutable RDMClass(true);
        immutInstance1.ImmutableMethod();
        // :: error: (method.invocation.invalid)
        immutInstance1.MutableMethod();
        immutInstance1.RDMMethod();
        // :: error: (method.invocation.invalid)
        mutInstance1.ImmutableMethod();
        mutInstance1.MutableMethod();
        mutInstance1.RDMMethod();
    }
}
