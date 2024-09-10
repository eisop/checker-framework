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
    }

    void testTop(@Top ReceiverAdaption this) {
        // :: error: (assignment.type.incompatible)
        @A Object varA = dependent;
        // :: error: (assignment.type.incompatible)
        @B Object varB = dependent;
        // :: error: (assignment.type.incompatible)
        @C Object varC = c;
        @Top Object varT = c;
    }
}
