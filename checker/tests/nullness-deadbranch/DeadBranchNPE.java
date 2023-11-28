// @skip-test until the bug is fixed

class DeadBranchNPE {
    void test1() {
        Object obj = null;
        if (true) {
            // :: error: (dereference.of.nullable)
            obj.toString();
        } else {
            obj.toString();
        }
    }

    void test2() {
        Object obj = null;
        // :: error: (dereference.of.nullable)
        obj.toString();
        for (int i = 0; i < 0; i++) {
            obj.toString();
        }
    }

    void test3() {
        Object obj = null;
        // :: error: (dereference.of.nullable)
        obj.toString();
        while (obj != null) {
            obj.toString();
        }
    }
}
