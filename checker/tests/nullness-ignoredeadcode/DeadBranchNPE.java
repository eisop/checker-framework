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
        // The following loop is dead code because the loop condition is false.
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
