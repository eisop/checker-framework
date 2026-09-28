import org.checkerframework.framework.testchecker.nodefaulttypevar.quals.Bottom;
import org.checkerframework.framework.testchecker.nodefaulttypevar.quals.Top;

class MinimalCrash {
    interface BoundTop<T, U extends @Top T> {
        U get();
    }

    interface BoundBottom<T, U extends @Bottom T> {
        U get();
    }

    interface BoundNone<T, U extends T> {
        U get();
    }

    // :: error: (type.argument.type.incompatible)
    <T> Object testTopNone(BoundTop<T, ? extends T> t) {
        return t.get();
    }

    // :: error: (type.argument.type.incompatible)
    <T> @Bottom Object testTopNoneAssign(BoundTop<T, ? extends T> t) {
        // :: error: (return.type.incompatible)
        return t.get();
    }

    <T> Object testNoneTop(BoundNone<T, ? extends @Top T> t) {
        return t.get();
    }

    <T> @Bottom Object testNoneTopAssign(BoundNone<T, ? extends @Top T> t) {
        // :: error: (return.type.incompatible)
        return t.get();
    }

    <T> Object testBottomNone(BoundBottom<T, ? extends T> t) {
        return t.get();
    }

    <T> Object testNoneBottom(BoundNone<T, ? extends @Bottom T> t) {
        return t.get();
    }

    <T> Object testNoneNone(BoundNone<T, ? extends T> t) {
        return t.get();
    }

    <T> Object testTopTop(BoundTop<T, ? extends @Top T> t) {
        return t.get();
    }
}
