import org.checkerframework.framework.testchecker.lubglb.quals.*;

interface Foo {}

interface Bar {}

class Baz implements Foo, Bar {}

public class IntersectionTypes {
    // :: warning: (explicit.annotation.ignored)
    <S extends @LubglbB Foo & @LubglbC Bar> void call1(S p) {}

    // :: warning: (explicit.annotation.ignored)
    <T extends @LubglbC Bar & @LubglbB Foo> void call2(T p) {}

    void foo1(@LubglbD Baz baz1) {
        call1(baz1);
        call2(baz1);
    }

    void foo2(@LubglbF Baz baz2) {
        call1(baz2);
        call2(baz2);
    }

    void foo3(@LubglbB Baz baz3) {
        // When two bounds carry conflicting qualifiers, the intersection's primary annotation is
        // the qualifier of the first bound in source order (first-bound-wins). call1's first bound
        // is @LubglbB and call2's is @LubglbC, so the same argument is accepted by call1 but not by
        // call2. This source-order dependence is accepted, expected behavior; a checker wanting an
        // order-independent rule overrides
        // AnnotatedTypeFactory.combineIntersectionBoundAnnotationsInHierarchy.
        call1(baz3);
        // :: error: (type.arguments.not.inferred)
        call2(baz3);
    }
}
