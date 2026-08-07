/*
 * @test
 * @summary Test that type-parameter upper-bound annotations reach the ATV
 *
 * @compile/fail/ref=Use.out -XDrawDiagnostics -processor org.checkerframework.framework.testchecker.typedeclbounds.TypeDeclBoundsChecker -Astubs=typeparambound.astub Use.java
 */

import org.checkerframework.framework.testchecker.typedeclbounds.quals.Bottom;

import java.util.Collections;
import java.util.Iterator;

public class Use {
    void argAboveBound(Iterator<String> it) {}

    void argAtBound(Iterator<@Bottom String> it) {}

    void methodArgAboveBound() {
        Collections.<String>emptyList();
    }

    void methodArgAtBound() {
        Collections.<@Bottom String>emptyList();
    }
}
