import viewpointtest.quals.*;

public class PolyConstructor {

    static class MyClass {
        // :: error: (super.invocation.invalid) :: warning: (inconsistent.constructor.type)
        @PolyVP MyClass(@PolyVP Object o) {
            // :: error: (new.class.type.invalid)
            throw new RuntimeException(" * You are filled with DETERMINATION."); // stub
        }

        void throwTopException() {
            // :: error: (new.class.type.invalid)
            throw new @Top RuntimeException();
        }

        void throwBottomException() {
            // :: warning: (cast.unsafe.constructor.invocation)
            throw new @Bottom RuntimeException();
        }

        void throwAException() {
            // :: warning: (cast.unsafe.constructor.invocation)
            throw new @A RuntimeException();
        }

        void throwBException() {
            // :: warning: (cast.unsafe.constructor.invocation)
            throw new @B RuntimeException();
        }

        void throwLostException() {
            // :: error: (new.class.type.invalid) :: warning: (cast.unsafe.constructor.invocation)
            throw new @Lost RuntimeException();
        }
    }

    void tests(@A Object ao) {
        // After poly resolution, the invocation resolved to @A MyClass
        @A MyClass myA = new MyClass(ao);
        // :: error: (assignment.type.incompatible)
        @B MyClass myB = new MyClass(ao);

        // Both argument "ao" and @B are parts of poly resolution
        // After poly resolution, the invocation resolved to @Top MyClass then casted to @B
        // The @B acts as a downcasting and will issue a warning
        // :: warning: (cast.unsafe.constructor.invocation)
        MyClass myTop = new @B MyClass(ao);
        // :: warning: (cast.unsafe.constructor.invocation)
        myB = new @B MyClass(ao);
        // :: error: (assignment.type.incompatible) :: warning: (cast.unsafe.constructor.invocation)
        myA = new @B MyClass(ao);
    }
}
