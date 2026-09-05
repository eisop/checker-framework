import org.checkerframework.checker.nullness.qual.*;
import org.checkerframework.framework.qual.DefaultQualifier;

public class NewObjectNonNull {
    @DefaultQualifier(Nullable.class)
    class A {
        A() {}
    }

    @DefaultQualifier(Nullable.class)
    class B {
        // No explicit constructor.
        // B() {}
    }

    void m() {
        new A().toString();
        new B().toString();
    }
}
