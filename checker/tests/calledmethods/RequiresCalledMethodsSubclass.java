import org.checkerframework.checker.calledmethods.qual.RequiresCalledMethods;

import java.io.Closeable;
import java.io.IOException;

public class RequiresCalledMethodsSubclass {

    public static class Parent {
        @RequiresCalledMethods(value = "#1", methods = "close")
        public void method(Closeable x) throws IOException {}

        public void caller(Closeable x) throws IOException {
            // ::error: (contracts.precondition.not.satisfied)
            method(x);
        }
    }

    public static class Subclass extends Parent {
        @Override
        public void method(Closeable x) throws IOException {}

        public void caller(Closeable x) throws IOException {
            method(x); // OK: we override method() with a weaker precondition
        }
    }
}
