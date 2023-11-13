import viewpointtest.quals.*;

@SuppressWarnings("cast.unsafe.constructor.invocation")
class AnnoOnTypeVariableUse<E> {
    @ReceiverDependentQual E element;

    static void test() {
        AnnoOnTypeVariableUse<@B Element> d = new @A AnnoOnTypeVariableUse<@B Element>();
        // d.element = @A |> @RDQ = @A
        // thus expects no error here
        d.element = new @A Element();
    }
}

class Element {
    int f = 1;
}
