/*
 * @test
 * @summary Test that the Resource Leak Checker prints the text of its messages, not their keys:
 * an error of the consistency analysis, which the Resource Leak Checker reports, and an error of
 * its Called Methods subchecker's visitor.
 *
 * @compile/fail/ref=ResourceLeakMessages.out -XDrawDiagnostics -processor org.checkerframework.checker.resourceleak.ResourceLeakChecker ResourceLeakMessages.java
 */

import org.checkerframework.checker.mustcall.qual.Owning;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

class ResourceLeakMessages {
    void leak() throws IOException {
        InputStream s = new FileInputStream("x");
        s.read();
    }

    static class Parent {
        void take(@Owning InputStream s) throws IOException {
            s.close();
        }
    }

    static class Child extends Parent {
        @Override
        void take(InputStream s) throws IOException {
            s.close();
        }
    }
}
