import org.checkerframework.checker.calledmethods.qual.EnsuresCalledMethods;

import java.io.Closeable;
import java.io.IOException;

public class EnsuresCalledMethodsSubclass {

    public static class Parent {
        @EnsuresCalledMethods(value = "#1", methods = "close")
        public void method(Closeable x) throws IOException {
            x.close();
        }
    }

    public static class Subclass extends Parent {
        @Override
        // ::error: (contracts.postcondition.not.satisfied)
        public void method(Closeable x) throws IOException {}
    }
}
