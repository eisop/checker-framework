import org.checkerframework.checker.pico.qual.*;

class PlainObjects {

    public void takeObject(Object o) {}
    ;

    public void takeImmutableObject(@Immutable Object o) {}
    ;

    void foo() {
        Object o1 = null;
        @Immutable Object o2 = null;

        takeObject(o2);

        // ::error: (argument.type.incompatible)
        takeImmutableObject(o1);
    }
}
