import org.checkerframework.checker.nullness.qual.Nullable;

// Anonymous and local classes are checked if and only if their enclosing class is.
public class CheckMe {

    interface Result {
        Object get();
    }

    static Object member() {
        // :: error: (return.type.incompatible)
        return null;
    }

    static Result anonymous() {
        return new Result() {
            public Object get() {
                // :: error: (return.type.incompatible)
                return null;
            }
        };
    }

    static Result local(@Nullable Object o) {
        class Local implements Result {
            public Object get() {
                // :: error: (return.type.incompatible)
                return o;
            }
        }
        return new Local();
    }

    static Result nestedInLocal() {
        class Local {
            class Nested implements Result {
                public Object get() {
                    // :: error: (return.type.incompatible)
                    return null;
                }
            }
        }
        return new Local().new Nested();
    }
}
