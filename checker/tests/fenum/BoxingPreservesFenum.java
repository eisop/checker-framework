import org.checkerframework.checker.fenum.qual.Fenum;

// Boxing and unboxing are desugared into Integer.valueOf and Integer.intValue, which jdk.astub
// declares as @PolyFenum so that the conversion preserves the fake enum.
public class BoxingPreservesFenum {

    void boxing(@Fenum("A") int a) {
        @Fenum("A") Integer boxed = a;
    }

    void unboxing(@Fenum("A") Integer a) {
        @Fenum("A") int unboxed = a;
    }

    // The conversion preserves the fake enum, so it does not launder one fenum into another.
    void boxingDoesNotLaunder(@Fenum("A") int a) {
        // :: error: (assignment.type.incompatible)
        @Fenum("B") Integer boxed = a;
    }

    void unboxingDoesNotLaunder(@Fenum("A") Integer a) {
        // :: error: (assignment.type.incompatible)
        @Fenum("B") int unboxed = a;
    }

    void roundTrip(@Fenum("A") int a) {
        @Fenum("A") Integer boxed = a;
        @Fenum("A") int back = boxed;
    }
}
