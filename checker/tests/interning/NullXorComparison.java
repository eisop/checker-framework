// Regression test for a bug in BinaryStubDiffChecker.compareAtm (branch binary-stubs-v2):
// "if (a == null || b == null) { if (a != b) ... }" was written to detect "exactly one of a, b
// is null", but knowing only the disjunction (at least one is null) does not make a non-null
// operand provably interned, so the identity comparison needs @Interned operands.

public class NullXorComparison {

    static class Node {}

    static boolean exactlyOneNull(Node a, Node b) {
        if (a == null || b == null) {
            // :: error: (not.interned)
            return a != b;
        }
        return false;
    }

    // The fix: compare nullness itself (a boolean), not object identity.
    static boolean exactlyOneNullFixed(Node a, Node b) {
        if (a == null || b == null) {
            return (a == null) != (b == null);
        }
        return false;
    }
}
