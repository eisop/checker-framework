import viewpointtest.quals.*;

public class ReceiverAdaption {

    @ReceiverDependentQual Object dependent;

    @C Object c;

    void testA(@A ReceiverAdaption this) {
        @A Object varA = dependent;
        // :: error: (assignment.type.incompatible)
        @B Object varB = dependent;
        @C Object varC = c;
        // :: error: (assignment.type.incompatible)
        @A Object varAc = c;
        // :: error: (assignment.type.incompatible)
        @B Object varBc = c;

        // Explicit this access
        @A Object thisVarA = this.dependent;
        // :: error: (assignment.type.incompatible)
        @B Object thisVarB = this.dependent;
        @C Object thisVarC = this.c;
        // :: error: (assignment.type.incompatible)
        @A Object thisVarAc = this.c;
    }

    void testB(@B ReceiverAdaption this) {
        // :: error: (assignment.type.incompatible)
        @A Object varA = dependent;
        @B Object varB = dependent;
        @C Object varC = c;
        // :: error: (assignment.type.incompatible)
        @A Object varAc = c;
        // :: error: (assignment.type.incompatible)
        @B Object varBc = c;

        // Explicit this access
        // :: error: (assignment.type.incompatible)
        @A Object thisVarA = this.dependent;
        @B Object thisVarB = this.dependent;
        @C Object thisVarC = this.c;
    }

    static @C Object staticC;

    void testTop(@Top ReceiverAdaption this) {
        // :: error: (assignment.type.incompatible)
        @A Object varA = dependent;
        // :: error: (assignment.type.incompatible)
        @B Object varB = dependent;
        // :: error: (assignment.type.incompatible)
        @C Object varC = c;
        @Top Object varT = c;

        // Explicit this access
        // :: error: (assignment.type.incompatible)
        @C Object thisVarC = this.c;
        @Top Object thisVarT = this.c;

        // Static field is not viewpoint adapted by instance receiver
        @C Object sC = staticC;
        @Top Object sTop = staticC;
        // :: error: (assignment.type.incompatible)
        @A Object sA = staticC;
    }

    static void testStatic() {
        @C Object sC = staticC;
        @Top Object sTop = staticC;
        // :: error: (assignment.type.incompatible)
        @A Object sA = staticC;
    }

    @SuppressWarnings("cast.unsafe.constructor.invocation")
    static class CtorAdaption {
        @C Object cInit = new @C Object();
        @C Object cUninit;

        CtorAdaption() {
            // Initialized field has declared type in constructor initial store
            @C Object vInit = cInit;
            @Top Object vInitTop = cInit;
            @C Object thisVInit = this.cInit;

            // :: error: (assignment.type.incompatible)
            @C Object vUninit = cUninit;
            @Top Object vUninitTop = cUninit;

            // :: error: (assignment.type.incompatible)
            @A Object vInitA = cInit;
        }
    }

    static class SuperClass {
        @C Object superC;
    }

    static class SubClass extends SuperClass {
        void testSubTop(@Top SubClass this) {
            // :: error: (assignment.type.incompatible)
            @C Object varC = superC;
            @Top Object varT = superC;

            // :: error: (assignment.type.incompatible)
            @C Object thisVarC = this.superC;
            @Top Object thisVarT = this.superC;
        }
    }
}
