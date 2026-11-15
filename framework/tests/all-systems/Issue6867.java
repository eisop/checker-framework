public class Issue6867 {
    interface A<T extends Number> {}

    static class B<T extends A<? super Integer>> {}

    void main(B<A<Number>> b) {
        // javac collapses the capture of `? super Number` to `Number`, because `Number` is the
        // bound of `T`, and accepts the assignment (JDK-8054309); Eclipse and IntelliJ reject it.
        // A checker accepts it only if it gives both bounds of `? super Number` the same
        // qualifier; otherwise `A<? super Number>` is a range of types that does not equal
        // `A<Number>`. See checker/tests/tainting/SuperWildcardCollapsedCapture.java.
        @SuppressWarnings({"super.wildcard", "assignment.type.incompatible"})
        B<A<? super Number>> x = b;
    }
}
