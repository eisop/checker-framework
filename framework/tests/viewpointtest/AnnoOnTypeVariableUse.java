import viewpointtest.quals.*;

@SuppressWarnings("cast.unsafe.constructor.invocation")
class Demo<E> {
    @ReceiverDependentQual E element;

    static void test() {
        Demo<@B Element> d = new @A Demo<@B Element>();
        // d.element = @A |> @RDQ = @A
        // thus expects no error here
        d.element = new @A Element();
    }
}

class Element {
    int f = 1;
}
