import viewpointtest.quals.*;

@SuppressWarnings("cast.unsafe.constructor.invocation")
class AnnoOnTypeVariableUse<E> {
    @ReceiverDependentQual E element;
    @A E a;
    @B E b;

    void test() {
        AnnoOnTypeVariableUse<@B Element> d = new @A AnnoOnTypeVariableUse<>();
        // d.element = @A |> @RDQ = @A
        d.element = new @A Element();
        // d.a = @A |> @A = @A
        d.a = new @A Element();
        // :: error: (assignment.type.incompatible)
        d.a = new @B Element();
        // d.b = @B |> @B = @B
        d.b = new @B Element();
        // :: error: (assignment.type.incompatible)
        d.b = new @A Element();
    }
}

class Element {
    int f = 1;
}
