// Test case for EISOP Issue 433:
// https://github.com/eisop/checker-framework/issues/433

import viewpointtest.quals.*;

public class IntersectionTypes {

    interface Foo {}

    interface Bar {}

    interface Accessor {
        @ReceiverDependentQual Object get();

        void set(@ReceiverDependentQual Object o);
    }

    class Baz implements Foo, Bar, Accessor {
        @Override
        public @ReceiverDependentQual Object get() {
            return null;
        }

        @Override
        public void set(@ReceiverDependentQual Object o) {}
    }

    <T extends Foo & Bar> void call(T p) {}

    <T extends Foo & Accessor> void callAccessor(T p) {}

    // :: error: (type.invalid.annotations.on.use)
    class ViewpointAdaptedIntersectionBound<T extends @ReceiverDependentQual Accessor & Foo> {
        void viewpointAdaptedIntersectionBound(@A T p, @A Object aObj, @B Object bObj) {
            @A Object adapted = p.get();
            // :: error: (assignment.type.incompatible)
            @B Object notAdaptedToB = p.get();

            p.set(aObj);
            // :: error: (argument.type.incompatible)
            p.set(bObj);
        }
    }

    void foo(@A Object aObj, @B Object bObj) {
        // :: warning: (cast.unsafe.constructor.invocation)
        Baz baz = new @A Baz();
        call(baz);
        callAccessor(baz);

        @A Object adapted = baz.get();
        // :: error: (assignment.type.incompatible)
        @B Object notAdaptedToB = baz.get();

        baz.set(aObj);
        // :: error: (argument.type.incompatible)
        baz.set(bObj);
    }

    void intersectionCasts(Object obj) {
        // :: error: (type.invalid.annotations.on.use)
        Foo fooAndBar = (Foo & Bar) obj;
        // :: error: (type.invalid.annotations.on.use)
        Accessor fooAndAccessor = (Foo & Accessor) obj;
    }

    void annotatedIntersectionCast(@B Object obj) {
        // :: warning: (cast.unsafe)
        Accessor fooAndAccessor = (@A Foo & Accessor) obj;
    }

    interface BType<X> {}

    interface CType<X> {}

    // :: error: (type.argument.type.incompatible)
    abstract class D<X extends BType<X> & CType<X>> {}

    // :: error: (type.argument.type.incompatible)
    class BC implements BType<BC>, CType<BC> {}

    class E extends D<BC> {}

    // :: error: (type.argument.type.incompatible)
    <T extends BType<T> & CType<T>> void callBC(T p) {}

    // Documents the current decision for https://github.com/eisop/checker-framework/issues/1735:
    // when multiple bounds in an intersection type have explicit qualifiers in the same hierarchy,
    // the later qualifier is ignored.
    // :: warning: (explicit.annotation.ignored)
    <T extends @A Foo & @B Bar> void callAnnotatedBounds(T p) {}
}
