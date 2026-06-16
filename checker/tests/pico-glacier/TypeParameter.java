import org.checkerframework.checker.pico.qual.Immutable;

class E {} // Note same name as type parameter, but mutable class.

@Immutable class TP_Superclass<E> {
    private E aField; // Should be OK because we'll make sure E is instantiated with an immutable

    // type. This E is the type parameter, not the class above.

    void aMethod() {}
}

@Immutable public class TypeParameter<E> extends TP_Superclass<E> {
    private TP_Superclass<? extends E> s;
    private TP_Superclass<? extends String> t;

    // ::error: (glacier.typeparameter.mutable)
    private TP_Superclass<? extends java.util.Date> u;

    void aMethod() {}
}

@Immutable class Test {
    TypeParameter<String> t1 = null; // OK

    // ::error: (glacier.typeparameter.mutable)
    TypeParameter<java.util.Date> t2 = null;
}
