// Test case for casts involving a primitive type, which have no elements to check.

public class PrimitiveCast {

    char narrow(int x) {
        return (char) x;
    }

    int widen(char c) {
        return (int) c;
    }

    long widenToLong(int x) {
        return (long) x;
    }

    double toDouble(long x) {
        return (double) x;
    }

    Integer box(int x) {
        return (Integer) x;
    }

    int unbox(Integer x) {
        return (int) x;
    }
}
