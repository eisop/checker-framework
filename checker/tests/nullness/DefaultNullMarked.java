import org.jspecify.nullness.NullMarked;
import org.jspecify.nullness.Nullable;

@NullMarked
public class DefaultNullMarked<T> {
  // :: error: (type.argument.type.incompatible)
  void foo(DefaultNullMarked<@Nullable String> d) {}
}
