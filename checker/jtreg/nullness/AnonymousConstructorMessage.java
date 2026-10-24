/*
 * @test
 * @summary Test that an error about an argument of a qualified anonymous subclass names the
 * superclass instead of printing an empty class name.
 * @requires jdk.version.major >= 11
 *
 * @compile/fail/ref=AnonymousConstructorMessage.out -XDrawDiagnostics -processor org.checkerframework.checker.nullness.NullnessChecker AnonymousConstructorMessage.java
 */

import org.checkerframework.checker.nullness.qual.Nullable;

class AnonymousConstructorMessage {
    class Inner {
        Inner(String... args) {}
    }

    void m(AnonymousConstructorMessage n, @Nullable String nbl) {
        n.new Inner("a", nbl) {};
    }
}
