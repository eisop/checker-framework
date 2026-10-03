/*
 * @test
 * @summary Test that type-parameter upper-bound annotations reach the ATV
 *
 * @compile/fail/ref=Use.out -XDrawDiagnostics -processor org.checkerframework.checker.tainting.TaintingChecker -Astubs=typeparambound.astub Use.java
 */

import org.checkerframework.checker.tainting.qual.Untainted;

import java.util.Collections;
import java.util.Iterator;

public class Use {
    void argAboveBound(Iterator<String> it) {}

    void argAtBound(Iterator<@Untainted String> it) {}

    void methodArgAboveBound() {
        Collections.<String>emptyList();
    }

    void methodArgAtBound() {
        Collections.<@Untainted String>emptyList();
    }
}
