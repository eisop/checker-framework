import org.checkerframework.checker.pico.qual.*;

import java.util.Arrays;

class Array {
    public static <T> T[] add(final T @Readonly [] array, final T entry) {
        final int s = array.length;
        final T[] t = Arrays.copyOf(array, s + 1);
        t[s] = entry;
        return t;
    }
}

public class ArrayParameter {
    final @Immutable Object @Immutable [] keys = new @Immutable Object @Immutable [0];

    public void passArray(@Immutable Object k) {
        Array.add(keys, k);
    }
}
