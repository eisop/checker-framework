import org.checkerframework.checker.mutability.qual.Mutable;
import org.checkerframework.checker.mutability.qual.Readonly;

import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Iterator;

public class RawTypeTest {
    private ArrayList list;

    protected void foo() {
        for (Iterator i = list.iterator(); i.hasNext(); ) {
            // Raw Iterator uses the erased element type. PICO treats the raw element as an
            // existential type below @Readonly Object, so the cast to A and the subsequent call are
            // checked against A's normal receiver requirements.
            ((A) i.next()).foo();
        }
    }

    interface A {

        public void foo();
    }

    public @Mutable abstract class RawList extends AbstractList {

        // Raw AbstractList erases E to Object, so these methods override the erased AbstractList
        // signatures. The Object parameters are annotated explicitly to avoid relying on raw-type
        // defaulting.
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
