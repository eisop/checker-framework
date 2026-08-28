import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.List;

public class CapturedWildcards {
    abstract static class MyClass {
        abstract boolean contains(MyClass other);
    }

    public boolean pass(List<? extends @Nullable MyClass> list, MyClass other) {
        return list.stream().anyMatch(je -> je != null && je.contains(other));
    }

    public boolean fail(List<? extends @Nullable MyClass> list, MyClass other) {
        // :: error: (dereference.of.nullable)
        return list.stream().anyMatch(je -> je.contains(other));
    }

    // The capture of a wildcard must keep the qualifier that was written on the wildcard's bound,
    // rather than the qualifier that the type parameter's declaration would default to. The
    // enhanced for loops below read `list` bare -- the read itself is what capture-converts the
    // type, with no type-variable substitution involved -- so the loop variable's type is the
    // captured type variable, and its upper bound decides whether the dereference is an error.

    public void bareReadNullableBound(List<? extends @Nullable MyClass> list) {
        for (MyClass elt : list) {
            // :: error: (dereference.of.nullable)
            elt.toString();
        }
    }

    public void bareReadNonNullBound(List<? extends MyClass> list) {
        for (MyClass elt : list) {
            elt.toString();
        }
    }

    public void bareReadNullableBoundOfNullableTypeParameter(
            MyList<? extends @Nullable MyClass> list) {
        for (MyClass elt : list) {
            // :: error: (dereference.of.nullable)
            elt.toString();
        }
    }

    // The wildcard bound is more restrictive than the type parameter's declared bound, so the
    // capture's upper bound is the wildcard's.
    public void bareReadNonNullBoundOfNullableTypeParameter(MyList<? extends MyClass> list) {
        for (MyClass elt : list) {
            elt.toString();
        }
    }

    interface MyList<T extends @Nullable Object> extends Iterable<T> {}
}
