package recordstubclass;

/** A library class whose nested class is a record in the annotated stub (see Outer.astub). */
public class Outer {
    public static class Inner {
        public final Object f = new Object();
    }

    public Object get() {
        return new Object();
    }
}
