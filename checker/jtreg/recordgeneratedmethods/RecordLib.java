import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.framework.qual.DefaultQualifier;

// Compiled without a checker, so that the records are read from bytecode.
public class RecordLib {
    public record Item(String name) {}

    @DefaultQualifier(Nullable.class)
    public record NullableDefault(String name) {}

    public record HandWrittenEquals(String name) {
        @Override
        public boolean equals(Object o) {
            return false;
        }
    }

    public record FinalAnnotatedEquals(String name) {
        @Override
        public final boolean equals(@NonNull Object o) {
            return false;
        }
    }

    public record ComponentNamedEquals(@Nullable String equals) {}
}
