import org.checkerframework.checker.nullness.qual.Nullable;

public class AnnoOnTypeVariableCrashCase {
    <T> @Nullable T test() {
        return (@Nullable T) null;
    }
}
