import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.Arrays;

// Illustrate various mis-uses of java.util.Arrays.
public class UtilArrays {
    public static void main(String[] args) {
        @Nullable Object[] arrayWithNull = {"", null, ""};
        Object[] arrayWithoutNull = {""};

        try {
            // :: error: (argument.type.incompatible)
            Arrays.binarySearch(arrayWithNull, "");
        } catch (NullPointerException e) {
            System.out.println("got NPE for array containing null");
        }

        try {
            // :: error: (argument.type.incompatible)
            Arrays.binarySearch(arrayWithoutNull, null);
        } catch (NullPointerException e) {
            System.out.println("got NPE for null key");
        }

        try {
            // :: error: (type.argument.type.incompatible)
            Arrays.sort(arrayWithNull, null);
        } catch (NullPointerException e) {
            System.out.println("got NPE for sort");
        }

        try {
            // TODO: false negative: covariant arrays and polymorphism cause that this call is
            // allowed.
            Arrays.fill(arrayWithoutNull, null);
            arrayWithoutNull[0].toString();
        } catch (NullPointerException e) {
            System.out.println("got NPE for fill");
        }
    }
}
