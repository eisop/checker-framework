import java.util.Date;

import qual.Immutable;

@Immutable
class A {
    @Immutable
    A() {}
}

// @skip-test Until we can get bound annotation on the super type that an anonymous class
// inherits from
public class NewAnnonymousClass {
    protected static A funLUDecompose() {
        // Even though there is no explicit @Immutable annotation on new, new A() still gets
        // @Immutable type. Is this expected behaviour?
        return new A() {
            Date d = new @Immutable Date();
            // Below are not allowed by Java. Will get "Illegal static declaration in inner
            // class <anonymous NewAnnonymousClass$1>"
            // static @ReceiverDependantMutable Object o;
            // static {
            //    o = null;
            // }
            // static @ReceiverDependantMutable Object foo() {return null;}
        };
    }
}
