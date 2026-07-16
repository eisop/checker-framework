import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.Arrays;

public class CopyOfArray {
    protected void makeCopy(Object[] args, int i) {
        Object[] copyExact1 = Arrays.copyOf(args, args.length);
        @Nullable Object[] copyExact2 = Arrays.copyOf(args, args.length);

        // :: warning: (arrays.copyof.size.mismatch)
        // :: error: (assignment.type.incompatible)
        Object[] copyInexact1 = Arrays.copyOf(args, i);
        @Nullable Object[] copyInexact2 = Arrays.copyOf(args, i);
    }

    static int callCount = 0;

    static String[] getArray() {
        callCount++;
        if (callCount == 1) {
            return new String[] {"a"};
        } else {
            return new String[] {"a", "b", "c"};
        }
    }

    void testSideEffect() {
        // getArray() has side effects, so Arrays.copyOf returns an array of @Nullable elements.
        // Assigning it to an array of @NonNull elements should be an error.
        // :: warning: (arrays.copyof.impure)
        // :: error: (assignment.type.incompatible)
        String[] result = Arrays.copyOf(getArray(), getArray().length);
    }

    @org.checkerframework.dataflow.qual.Pure
    static String[] getPureArray() {
        return new String[] {"a", "b", "c"};
    }

    void testPureMethod() {
        // getPureArray() is pure, so this should not produce an error!
        String[] result = Arrays.copyOf(getPureArray(), getPureArray().length);
    }

    String[] fieldArray = new String[] {"a"};

    void testMemberSelect() {
        String[] result = Arrays.copyOf(this.fieldArray, this.fieldArray.length);
    }

    void testArrayAccess(String[][] matrix) {
        String[] result = Arrays.copyOf(matrix[0], matrix[0].length);
    }

    void testCast(Object[] args) {
        String[] result = Arrays.copyOf((String[]) args, ((String[]) args).length);
    }

    void testParenthesized(String[] args) {
        String[] result = Arrays.copyOf((args), (args).length);
    }

    void testAssignment(String[] args) {
        String[] other;
        // :: warning: (arrays.copyof.impure)
        // :: error: (assignment.type.incompatible)
        String[] result = Arrays.copyOf(other = args, (other = args).length);
    }

    void testPreIncrement(String[][] matrix, int i) {
        // :: warning: (arrays.copyof.impure)
        // :: error: (assignment.type.incompatible)
        String[] result = Arrays.copyOf(matrix[++i], matrix[++i].length);
    }

    <T extends Object> void testTypeVar(T[] args, int i) {
        T[] copyExact1 = Arrays.copyOf(args, args.length);
        @Nullable T[] copyExact2 = Arrays.copyOf(args, args.length);

        // :: warning: (arrays.copyof.size.mismatch)
        // :: error: (assignment.type.incompatible)
        T[] copyInexact1 = Arrays.copyOf(args, i);
        @Nullable T[] copyInexact2 = Arrays.copyOf(args, i);
    }
}
