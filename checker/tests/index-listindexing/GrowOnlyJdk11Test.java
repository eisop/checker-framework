// @below-java11-jdk-skip-test

import java.util.List;
import org.checkerframework.checker.index.qual.GrowOnly;

/** Tests GrowOnly with the {@code Collection.toArray(IntFunction)} method, added in Java 11. */
public class GrowOnlyJdk11Test {

  /**
   * Calls {@code toArray} with a generator function on a {@code @GrowOnly} list.
   *
   * @param list a list that may not shrink
   */
  void testToArrayWithGenerator(@GrowOnly List<String> list) {
    // toArray does not modify the list.
    String[] strArray = list.toArray(String[]::new);

    // But it does not affect the list's mutability restrictions.
    // :: error: (method.invocation.invalid)
    list.clear();
  }
}
