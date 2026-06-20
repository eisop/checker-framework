// Test case for EISOP issue 628:
// https://github.com/eisop/checker-framework/issues/628

// @below-java21-jdk-skip-test

import org.checkerframework.checker.nullness.qual.Nullable;

class NullRedundant {
    void test1(Object o) {
        // :: warning: (nulltest.redundant)
        if (o == null) {
            System.out.println("o is null");
        }

        switch (o) {
            case Number n:
                System.out.println("Number: " + n);
                break;
            // :: warning: (nulltest.redundant)
            case null:
                System.out.println("null");
                break;
            default:
                System.out.println("anything else");
        }

        switch (o) {
            // :: warning: (nulltest.redundant)
            case null, default:
                System.out.println("null");
                break;
        }
    }

    Object test2(Object o) {
        switch (o) {
            case Number n -> System.out.println("Number: " + n);
            // :: warning: (nulltest.redundant)
            case null -> System.out.println("null");
            default -> System.out.println("anything else");
        }
        ;

        switch (o) {
            // :: warning: (nulltest.redundant)
            case null, default -> System.out.println("null");
        }

        var output =
                switch (o) {
                    case Number n -> "Number: " + n;
                    // :: warning: (nulltest.redundant)
                    case null -> "null";
                    default -> "anything else";
                };

        return switch (o) {
            // :: warning: (nulltest.redundant)
            case null -> "null";
            default -> "anything else";
        };
    }

    // Test with Nullable argument to make sure there is no false positive.
    void test3(@Nullable Object o) {
        if (o == null) {
            System.out.println("o is null");
        }

        switch (o) {
            case Number n:
                System.out.println("Number: " + n);
                break;
            case null:
                System.out.println("null");
                break;
            default:
                System.out.println("anything else");
        }

        switch (o) {
            case null, default:
                System.out.println("null");
                break;
        }
    }

    Object test4(@Nullable Object o) {
        switch (o) {
            case Number n -> System.out.println("Number: " + n);
            case null -> System.out.println("null");
            default -> System.out.println("anything else");
        }
        ;

        switch (o) {
            case null, default -> System.out.println("null");
        }

        var output =
                switch (o) {
                    case Number n -> "Number: " + n;
                    case null -> "null";
                    default -> "anything else";
                };

        return switch (o) {
            case null -> "null";
            default -> "anything else";
        };
    }
}
