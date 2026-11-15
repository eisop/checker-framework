import org.checkerframework.checker.tainting.qual.Tainted;

// javac collapses the capture of `? super Number` to `Number`, because `Number` is the bound of
// `T`, so `B<A<Number>>` and `B<A<? super Number>>` are the same Java type. The bounds of the
// wildcard default to `@Untainted Number` and `@Tainted Number`, so `A<? super Number>` is a range
// of types and does not equal `A<@Tainted Number>`; with equal bounds it does.
public class SuperWildcardCollapsedCapture {
    interface A<T extends Number> {}

    static class B<T extends A<? super Integer>> {}

    @SuppressWarnings("super.wildcard")
    void differentBounds(B<A<Number>> b) {
        // :: error: (assignment.type.incompatible)
        B<A<? super Number>> x = b;
    }

    @SuppressWarnings("super.wildcard")
    void equalBounds(B<A<Number>> b) {
        B<A<? super @Tainted Number>> x = b;
        B<A<Number>> y = x;
    }
}
