// @below-java17-jdk-skip-test

import java.util.function.Supplier;

public final class Issue5930 {
    enum TestEnum {
        FIRST,
        SECOND
    };

    public static void main(String[] args) {
        TestEnum testEnum = TestEnum.FIRST;
        Supplier<Integer> supplier =
                switch (testEnum) {
                    case FIRST -> () -> 1;
                    case SECOND -> () -> 2;
                };
        System.out.println(supplier.get());
    }
}
