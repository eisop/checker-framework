// @below-java17-jdk-skip-test

import org.checkerframework.checker.nullness.qual.NonNull;

import java.util.function.Supplier;

public final class Issue5967 {

    enum TestEnum {
        FIRST,
        SECOND;
    }

    public static void main(String[] args) {
        TestEnum testEnum = TestEnum.FIRST;
        Supplier<Integer> supplier =
                switch (testEnum) {
                    case FIRST:
                        yield () -> 1;
                    case SECOND:
                        yield () -> 2;
                };
        @NonNull Supplier<Integer> supplier1 = supplier;
    }
}
