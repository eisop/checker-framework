// Test case for EISOP issue #2036:
// https://github.com/eisop/checker-framework/issues/2036
// Viewpoint-adapting the direct supertypes of a type whose type argument is a type variable that
// the type's class does not declare recursed until the stack overflowed.
import java.util.List;

public class Issue2036 {
    interface Coll<E> {
        int size();

        default int viaSeq(Seq<E> s) {
            return s.size();
        }
    }

    interface Seq<E> extends Coll<E> {}

    interface MyList<E> extends Seq<E> {}

    int read(MyList<String> l) {
        return l.size();
    }

    int readJdk(List<String> l) {
        return l.size();
    }

    static class Generic<F> {
        int read(MyList<F> l) {
            return l.size();
        }
    }
}
