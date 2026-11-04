// Test case for https://github.com/eisop/checker-framework/issues/2418

import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

public class EisopIssue2418<E> {
    class It {
        E k() {
            throw new RuntimeException();
        }
    }

    static class N extends EisopIssue2418<@Nullable String> {
        void direct() {
            new It() {}.k();
        }

        class Keys {
            void inner() {
                new It().k();
                new It() {}.k();
                // :: error: (dereference.of.nullable)
                new It() {}.k().toString();
            }

            class Deeper {
                @Nullable String deeper() {
                    return new It() {}.k();
                }
            }
        }
    }

    static class M extends EisopIssue2418<String> {
        class Keys {
            @NonNull String inner() {
                return new It() {}.k();
            }
        }
    }
}
