// @below-java21-jdk-skip-test

class NullRedundant {
    void test(Object o) {
        // :: (nulltest.redundant)
        if (o == null) {
            System.out.println("o is null");
        }
        switch(o) {
            case Number n: System.out.println("Number: " + n); break;
            // :: (nulltest.redundant)
            case null: System.out.println("null"); break;
            default: System.out.println("anything else");
        }
        switch(o) {
            case Number n -> System.out.println("Number: " + n); break;
            // :: (nulltest.redundant)
            case null -> System.out.println("null"); break;
            // :: (nulltest.redundant)
            case Number n, null -> System.out.println("Number: " + n); break;
            default -> System.out.println("anything else");
        }

        void output = switch(o) {
            case Number n -> "Number: " + n;
            // :: (nulltest.redundant)
            case null -> "null";
            // :: (nulltest.redundant)
            case Number n, null -> "Number: " + n;
            default -> "anything else";
        };

    }
}
