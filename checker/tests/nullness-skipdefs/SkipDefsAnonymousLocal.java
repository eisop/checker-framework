import org.checkerframework.checker.nullness.qual.Nullable;

// Anonymous and local classes are skipped if and only if their enclosing class is.
public class SkipDefsAnonymousLocal {

    interface Result {
        Object get();
    }

    static class SkipMe {
        static Result anonymous() {
            return new Result() {
                public Object get() {
                    return null;
                }
            };
        }

        static Result local() {
            class Local implements Result {
                public Object get() {
                    return null;
                }
            }
            return new Local();
        }
    }

    static class DontSkip {
        static Result anonymous() {
            return new Result() {
                public Object get() {
                    // :: error: (return.type.incompatible)
                    return null;
                }
            };
        }

        static Result local() {
            class Local implements Result {
                public Object get() {
                    // :: error: (return.type.incompatible)
                    return null;
                }

                class Inner {
                    Object get(@Nullable Object o) {
                        // :: error: (return.type.incompatible)
                        return o;
                    }
                }
            }
            return new Local();
        }
    }
}
