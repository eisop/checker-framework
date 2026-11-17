// Anonymous subclasses of inner classes created with an explicit enclosing instance. The
// enclosing types of their annotated types differ from javac's, which made getErased throw a
// NullPointerException when javac's chain was the longer one. The diamond forms are in
// checker/tests/nullness/EisopIssue2461.java: under the Value Checker, inference fails on a
// diamond anonymous class with arguments and no target type, which is a different bug.

public class EisopIssue2461<K> {
    class It<T> {
        T t;

        It(T t) {
            this.t = t;
        }
    }

    static class Top {
        class Mid<E> {
            class In<T> {
                E e;

                In(E e) {
                    this.e = e;
                }
            }
        }
    }

    class Keys {
        void longerJavacChain(Object n, EisopIssue2461<K> outer) {
            Object a = outer.new It<Object>(n) {}.t;
            Object b = EisopIssue2461.this.new It<Object>(n) {}.t;
        }

        void sameLengthDifferentClasses(Top.Mid<String> outer, String s) {
            String e = outer.new In<Object>(s) {}.e;
        }

        void lambdaAndAnonymous(EisopIssue2461<K> outer, Object n) {
            Runnable r =
                    () -> {
                        Object x = outer.new It<Object>(n) {}.t;
                    };
            Object y =
                    new Object() {
                        Object z = outer.new It<Object>(n) {}.t;
                    };
        }

        void lubAndInference(EisopIssue2461<K> outer, Object n, boolean b) {
            It<Object> x = b ? outer.new It<Object>(n) {} : outer.new It<Object>(n) {};
            It<Object> y = id(outer.new It<Object>(n) {});
        }

        <S> S id(S s) {
            return s;
        }
    }

    void shorterJavacChain(Top.Mid<String> outer, String s) {
        String e = outer.new In<Object>(s) {}.e;
    }

    static void staticContext(EisopIssue2461<String> outer, Object n) {
        Object x = outer.new It<Object>(n) {}.t;
    }
}
