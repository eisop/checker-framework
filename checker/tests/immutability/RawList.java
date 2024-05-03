import org.checkerframework.checker.immutability.qual.Readonly;

import java.util.AbstractList;

public abstract class RawList extends AbstractList {

    // What method does it override?
    // What should be the type if no type parameter on class declaration
    @Override
    @SuppressWarnings("unchecked")
    public boolean add(Object o) {
        return super.add(o);
    }

    @Override
    @SuppressWarnings("unchecked")
    public void add(int i, Object o) {
        super.add(i, o);
    }

    @Override
    @SuppressWarnings("unchecked")
    public Object set(int i, Object o) {
        return super.set(i, o);
    }
}

abstract class MyList<E> extends AbstractList<E> {

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
