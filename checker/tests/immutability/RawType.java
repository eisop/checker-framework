import java.util.ArrayList;
import java.util.Iterator;

interface A {

    public void foo();
}

public class RawType {

    private ArrayList list;

    protected void foo() {
        for (Iterator i = list.iterator(); i.hasNext(); ) {
            // Iterator is raw type here. After JDK1.5, it're represented as if there is type
            // argument
            // "? extends @Mutable Object"(a range of types below @Mutable Object), which is passed
            // to
            // type parameter "E extends @Readonly Object"(one fixtd type below @Readonly Object).
            // Since
            // any type under @Mutable Object is below @Readonly Object, "? extends @Mutable Object"
            // is
            // a valid type argument. foo() method expects a @Mutable A receiver, like above,
            // "? extends @Mutable Object" is a valid actual receiver(subtype of @Mutable Object) so
            // the
            // method invocation typechecks
            ((A) i.next()).foo();
        }
    }
}
