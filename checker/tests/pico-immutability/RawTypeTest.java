import org.checkerframework.checker.pico.qual.Mutable;
import org.checkerframework.checker.pico.qual.Readonly;

import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Iterator;

public class RawTypeTest {
    // :: error: (initialization.field.uninitialized)
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

    interface A {

        public void foo();
    }

    public @Mutable abstract class RawList extends AbstractList {

        // What method does it override?
        // What should be the type if no type parameter on class declaration
        @Override
        @SuppressWarnings("unchecked")
        public boolean add(@Readonly Object o) {
            return super.add(o);
        }

        @Override
        @SuppressWarnings("unchecked")
        public void add(int i, @Readonly Object o) {
            super.add(i, o);
        }

        @Override
        @SuppressWarnings("unchecked")
        public Object set(int i, @Readonly Object o) {
            return super.set(i, o);
        }
    }

    @Mutable abstract class MyList<E> extends AbstractList<E> {

        @Override
        public E get(@Readonly MyList<E> this, int i) {
            return null;
        }

        @Override
        public boolean add(E e) {
            return super.add(e);
        }

        @Override
        public void add(int i, E e) {
            super.add(i, e);
        }

        @Override
        public E set(int i, E e) {
            return super.set(i, e);
        }
    }
}
