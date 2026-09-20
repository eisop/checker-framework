/*
 * @test
 *
 * @summary A conversion that the CFG desugars into a method call is not type-checked, although the
 * explicit form of the same call is.  The synthetic tree is built by TreeBuilder inside the CFG
 * builder, so it is never part of the AST that BaseTypeVisitor scans.  Each pair below is the
 * explicit form, which is checked, followed by the conversion that desugars to it, which is not.
 * See https://github.com/eisop/checker-framework/pull/208.
 *
 * @compile/fail/ref=BoxingArgs.out -XDrawDiagnostics -processor org.checkerframework.framework.testchecker.h1h2checker.H1H2Checker -Astubs=boxing.astub BoxingArgs.java
 */

import org.checkerframework.framework.testchecker.h1h2checker.quals.H1S1;
import org.checkerframework.framework.testchecker.h1h2checker.quals.H1S2;

import java.util.List;

public class BoxingArgs {
    void explicitBox(@H1S2 int b) {
        Integer boxed = Integer.valueOf(b);
    }

    void implicitBox(@H1S2 int b) {
        Integer boxed = b;
    }

    void explicitUnbox(@H1S2 Integer b) {
        int i = b.intValue();
    }

    void implicitUnbox(@H1S2 Integer b) {
        int i = b;
    }

    void explicitIterator(@H1S2 List<String> l) {
        java.util.Iterator<String> it = l.iterator();
    }

    void enhancedFor(@H1S2 List<String> l) {
        for (String s : l) {}
    }

    void explicitClose(@H1S2 Resource r) throws Exception {
        r.close();
    }

    // try-with-resources desugars to the same close() call, on the resource variable.
    void tryWithResources(@H1S2 Resource r) throws Exception {
        try (@H1S2 Resource r2 = r) {}
    }

    static class Resource implements AutoCloseable {
        // The narrowed receiver is the point of the test; AutoCloseable.close() has no such bound.
        @Override
        @SuppressWarnings({"override.receiver.invalid", "super.invocation"})
        public void close(@H1S1 Resource this) {}
    }
}
