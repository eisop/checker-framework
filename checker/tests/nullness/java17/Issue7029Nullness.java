// @skip-test EISOP issue #1213
// @below-java17-jdk-skip-test

package open.crash;

import java.util.Collection;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.checkerframework.checker.initialization.qual.Initialized;
import org.checkerframework.checker.nullness.qual.Nullable;

public class Issue7029Nullness {

  private record Item(@Nullable String id) {}

  Map<String, Item> test(Collection<Item> source) {
    return source.stream()
        .flatMap(
            item -> {
              @Initialized String id = item.id();
              return id == null ? Stream.empty() : Stream.of(Map.entry(id, item));
            })
        .collect(Collectors.toUnmodifiableMap(entry -> entry.getKey(), entry -> entry.getValue()));
  }
}
