// @below-java21-jdk-skip-test

class NullRedundant {
    void test1(Object o) {
        // :: warning: (nulltest.redundant)
        if (o == null) {
            System.out.println("o is null");
        }
        switch (o) {
            case Number n: System.out.println("Number: " + n); break;
            // :: warning: (nulltest.redundant)
            case null: System.out.println("null"); break;
            default: System.out.println("anything else");
        }

        var output = switch (o) {
            case Number n -> "Number: " + n;
            // :: warning: (nulltest.redundant)
            case null -> "null";
            default -> "anything else";
        };
    }

    Object test2(Object o) {
        return switch (o) {
            // :: warning: (nulltest.redundant)
            case null -> "null";
            default -> "anything else";
        };
    }
}
