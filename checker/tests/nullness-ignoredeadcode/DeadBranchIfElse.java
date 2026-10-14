// Test case for -AignoreDeadCode: a literal-condition `if` never executes its untaken branch,
// so no error should be reported there. The taken branch is live and must still be checked.
class DeadBranchIfElse {

    void constantTrue() {
        Object obj = null;
        if (true) {
            // :: error: (dereference.of.nullable)
            obj.toString();
        } else {
            // This branch is dead; no error expected under -AignoreDeadCode.
            obj.toString();
        }
    }

    void constantFalse() {
        Object obj = null;
        if (false) {
            // This branch is dead; no error expected under -AignoreDeadCode.
            obj.toString();
        } else {
            // :: error: (dereference.of.nullable)
            obj.toString();
        }
    }

    static final boolean DEBUG = false;

    void constantFinalField() {
        Object obj = null;
        if (DEBUG) {
            // This branch is dead; no error expected under -AignoreDeadCode.
            obj.toString();
        }
    }
}
