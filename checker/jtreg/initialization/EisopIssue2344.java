/*
 * @test
 * @summary Test case for eisop issue 2344: the type of this in a constructor, which the
 * Initialization Checker computes from the store before the tree, is the same whether the
 * type factory caches it or recomputes it on every query.
 *
 * @compile/fail/ref=EisopIssue2344.out -XDrawDiagnostics -processor org.checkerframework.checker.nullness.NullnessChecker EisopIssue2344.java
 * @compile/fail/ref=EisopIssue2344.out -XDrawDiagnostics -processor org.checkerframework.checker.nullness.NullnessChecker -AatfDoNotCache EisopIssue2344.java
 */

import org.checkerframework.checker.initialization.qual.UnderInitialization;

public class EisopIssue2344 extends EisopIssue2344Super {
    // Initialized only in the delegated constructor.
    private Object table;

    private int size = 0;

    EisopIssue2344(int capacity) {
        useSuper(this);
        useSelf(this);
        table = new Object();
        useSelf(this);
        this.size = capacity;
    }

    EisopIssue2344() {
        this(4);
        useSelf(this);
        table.hashCode();
        this.size = 0;
    }

    static void useSuper(@UnderInitialization(EisopIssue2344Super.class) EisopIssue2344 m) {}

    static void useSelf(@UnderInitialization(EisopIssue2344.class) EisopIssue2344 m) {}
}

class EisopIssue2344Super {
    Object base;

    EisopIssue2344Super() {
        base = new Object();
    }
}
