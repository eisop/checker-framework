import viewpointtest.quals.*;

@SuppressWarnings("cast.unsafe.constructor.invocation")
class AnnoOnTypeVariableUse<E> {
    @ReceiverDependentQual E rdq;
    @A E a;
    @B E b;

    void test() {
        AnnoOnTypeVariableUse<@B Element> d = new @A AnnoOnTypeVariableUse<>();
        // d.element = @A |> @RDQ = @A
        d.rdq = new @A Element();
        // :: error: (assignment.type.incompatible)
        d.rdq = new @B Element();
        // d.a = @A |> @A = @A
        d.a = new @A Element();
        // :: error: (assignment.type.incompatible)
        d.a = new @B Element();
        // d.b = @A |> @B = @B
        d.b = new @B Element();
        // :: error: (assignment.type.incompatible)
        d.b = new @A Element();
    }

    class Element {}
}
