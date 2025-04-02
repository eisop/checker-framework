// Test case for EISOP issue #777:
// https://github.com/eisop/checker-framework/issues/777
import viewpointtest.quals.*;

@ReceiverDependentQual public class VarargsConstructor {
    VarargsConstructor(String str, Object... args) {}

    @ReceiverDependentQual VarargsConstructor(@ReceiverDependentQual Object... args) {}

    void foo() {
        VarargsConstructor a = new @A VarargsConstructor("testStr", new @A Object());
    }

    void invokeConstructor(@A Object aObj, @B Object bObj, @Top Object topObj) {
        @A Object a = new @A VarargsConstructor(aObj);
        @B Object b = new @B VarargsConstructor(bObj);
        // :: error: (argument.type.incompatible) :: error: (new.class.type.invalid)
        @Top Object top = new @Top VarargsConstructor(topObj);
        // :: error: (argument.type.incompatible)
        new @A VarargsConstructor(bObj);
        // :: error: (argument.type.incompatible)
        new @B VarargsConstructor(aObj);
    }

    @ReceiverDependentQual class Inner {
        @ReceiverDependentQual Inner(@ReceiverDependentQual Object... args) {}

        void foo() {
            Inner a = new Inner();
            Inner b = new @A Inner(new @A Object());
            Inner c = VarargsConstructor.this.new @A Inner();
            Inner d = VarargsConstructor.this.new @A Inner(new @A Object());
        }

        void invokeConstructor(@A Object aObj, @B Object bObj, @Top Object topObj) {
            @A Object a = new @A Inner(aObj);
            @B Object b = new @B Inner(bObj);
            // :: error: (argument.type.incompatible) :: error: (new.class.type.invalid)
            @Top Object top = new @Top Inner(topObj);
            // :: error: (argument.type.incompatible)
            new @A Inner(bObj);
            // :: error: (argument.type.incompatible)
            new @B Inner(aObj);
        }
    }

    void testAnonymousClass(@A Object aObj, @B Object bObj, @Top Object topObj) {
        Object o =
                new @A VarargsConstructor("testStr", new @A Object()) {
                    void foo() {
                        VarargsConstructor a =
                                new @A VarargsConstructor("testStr", new @A Object());
                    }
                };
        @A Object a = new @A VarargsConstructor(aObj) {};
        @B Object b = new @B VarargsConstructor(bObj) {};
        // :: error: (argument.type.incompatible) :: error: (new.class.type.invalid) :: error:
        // (varargs.type.incompatible)
        @Top Object top = new @Top VarargsConstructor(topObj) {};
        // :: error: (argument.type.incompatible)
        new @A VarargsConstructor(bObj) {};
        // :: error: (argument.type.incompatible)
        new @B VarargsConstructor(aObj) {};
    }
}
