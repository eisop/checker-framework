package storeinbytecodelib;

import java.util.ArrayList;
import java.util.List;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.framework.qual.AnnotatedFor;

/** Only {@code @Nullable} is written; any other qualifier in the class file was stored. */
@AnnotatedFor("nullness")
public class DefaultStorage<T extends @Nullable Object> {
  public static final List<String> FIELD = new ArrayList<>();
}
