// @below-java16-jdk-skip-test

import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.framework.qual.DefaultQualifier;

import java.util.function.Predicate;

class RecordGeneratedMethods {
    record Item(String name) {}

    @DefaultQualifier(Nullable.class)
    record NullableDefault(String name) {}

    record HandWrittenEquals(String name) {
        @Override
        // :: error: (override.param.invalid)
        public boolean equals(Object o) {
            return false;
        }
    }

    record ComponentNamedEquals(@Nullable String equals) {}

    record Pair<U>(U a, @Nullable U b) {}

    void generated(Item item) {
        item.equals(null);
        ((Record) item).equals(null);
        Predicate<@Nullable Object> p = item::equals;
        int h = item.hashCode();
        item.toString().length();
    }

    void generatedUnderDefaultQualifier(NullableDefault n) {
        n.equals(null);
        n.toString().length();
    }

    void handWritten(HandWrittenEquals h) {
        // :: error: (argument.type.incompatible)
        h.equals(null);
    }

    void generic(Pair<String> pair) {
        pair.equals(null);
        Predicate<@Nullable Object> p = pair::equals;
        pair.toString().length();
    }

    void componentNamedEquals(ComponentNamedEquals c) {
        c.equals(null);
        // :: error: (dereference.of.nullable)
        c.equals().length();
    }
}
