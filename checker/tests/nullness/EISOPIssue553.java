// Test case for EISOP Issue 553:
// https://github.com/eisop/checker-framework/issues/553

public class EISOPIssue553 {
    static Object sfield = "";
    Object field = "";

    public static void main(String[] args) {
        EISOPIssue553 x = null;
        Object o = x.sfield;
        // :: error: (dereference.of.nullable)
        o = x.field;
    }
}
