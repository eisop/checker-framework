import org.checkerframework.checker.signedness.qual.Signed;
import org.checkerframework.checker.signedness.qual.Unsigned;
import org.checkerframework.checker.units.qual.m;

import java.util.Arrays;

public class LiteralCast {

    @Unsigned int u;
    @Signed int s;

    void m() {
        testCompile(2);
        // manifest literals are treated as @SignednessGlb
        testCompile(-2);
        // :: error: (argument.type.incompatible)
        testCompile((@Signed int) 2);
        testCompile((@Unsigned int) 2);
        testCompile((int) 2);
        testCompile((@m int) 2);

        requireSigned((@Signed int) 2);
        // :: error: (argument.type.incompatible)
        requireSigned((@Unsigned int) 2);
        requireSigned((int) 2);
        requireSigned((@m int) 2);
        // :: warning: (cast.unsafe)
        requireSigned((@Signed int) u);
        // :: error: (argument.type.incompatible)
        requireSigned((@Unsigned int) u);
        // :: error: (argument.type.incompatible)
        requireSigned((int) u);
        // :: error: (argument.type.incompatible)
        requireSigned((@m int) u);
        requireSigned((@Signed int) s);
        // :: error: (argument.type.incompatible) :: warning: (cast.unsafe)
        requireSigned((@Unsigned int) s);
        requireSigned((int) s);
        requireSigned((@m int) s);

        // :: error: (argument.type.incompatible)
        requireUnsigned((@Signed int) 2);
        requireUnsigned((@Unsigned int) 2);
        requireUnsigned((int) 2);
        requireUnsigned((@m int) 2);
        // :: error: (argument.type.incompatible) :: warning: (cast.unsafe)
        requireUnsigned((@Signed int) u);
        requireUnsigned((@Unsigned int) u);
        requireUnsigned((int) u);
        requireUnsigned((@m int) u);
        // :: error: (argument.type.incompatible)
        requireUnsigned((@Signed int) s);
        // :: warning: (cast.unsafe)
        requireUnsigned((@Unsigned int) s);
        // :: error: (argument.type.incompatible)
        requireUnsigned((int) s);
        // :: error: (argument.type.incompatible)
        requireUnsigned((@m int) s);
    }

    void requireSigned(@Signed int arg) {}

    void requireUnsigned(@Unsigned int arg) {}

    public static void testCompile(@Unsigned int x) {
        @Unsigned int[] arr = {1, 2, 3, 4, 5, 56};

        Arrays.fill(arr, x);
        Arrays.fill(arr, (@Unsigned int) Integer.valueOf(-2));
        Arrays.fill(arr, Integer.valueOf(-2));
        Arrays.fill(arr, (@Unsigned int) Integer.valueOf(2));
    }
}
