/*
 * @test
 *
 * @summary A conversion that the CFG desugars into a method call is not type-checked, although the
 * explicit form of the same call is.  The synthetic tree is built by TreeBuilder inside the CFG
 * builder, so it is never part of the AST that BaseTypeVisitor scans.  Each pair below is the
 * explicit form, which is checked, followed by the conversion that desugars to it, which is not.
 * See https://github.com/eisop/checker-framework/pull/208.
 *
 * @compile/fail/ref=BoxingArgs.out -XDrawDiagnostics -processor org.checkerframework.framework.testchecker.boxing.BoxingChecker -Astubs=boxing.astub BoxingArgs.java
 */

import org.checkerframework.framework.testchecker.boxing.qual.Alpha;
import org.checkerframework.framework.testchecker.boxing.qual.Beta;

import java.util.List;

public class BoxingArgs {
    void explicitBox(@Beta int b) {
        Integer boxed = Integer.valueOf(b);
    }

    void implicitBox(@Beta int b) {
        Integer boxed = b;
    }

    void explicitUnbox(@Beta Integer b) {
        int i = b.intValue();
    }

    void implicitUnbox(@Beta Integer b) {
        int i = b;
    }

    void explicitIterator(@Beta List<String> l) {
        java.util.Iterator<String> it = l.iterator();
    }

    void enhancedFor(@Beta List<String> l) {
        for (String s : l) {}
    }

    void explicitClose(@Beta Resource r) throws Exception {
        r.close();
    }

    // try-with-resources desugars to the same close() call, on the resource variable.
    void tryWithResources(@Beta Resource r) throws Exception {
        try (@Beta Resource r2 = r) {}
    }

    static class Resource implements AutoCloseable {
        // The narrowed receiver is the point of the test; AutoCloseable.close() has no such bound.
        @Override
        @SuppressWarnings("override.receiver.invalid")
        public void close(@Alpha Resource this) {}
    }
}
