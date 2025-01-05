class DeadBranchNPE {
    void test1() {
        Object obj = null;
        if (true) {
            // :: error: (dereference.of.nullable)
            obj.toString();
        } else {
            // :: error: (dereference.of.nullable)
            obj.toString();
        }
    }

    void test2() {
        Object objOut = null;
        Object objInner = null;
        // :: error: (dereference.of.nullable)
        objOut.toString();
        // The following loop is dead code because the loop condition is false.
        for (int i = 0; i < 0; i++) {
            // :: error: (dereference.of.nullable)
            objInner.toString();
        }
    }

    void test3() {
        Object objOut = null;
        Object objInner = null;
        // :: error: (dereference.of.nullable)
        objOut.toString();
        // :: warning: (nulltest.redundant)
        while (objOut != null) {
            // :: error: (dereference.of.nullable)
            objInner.toString();
        }
    }
}
