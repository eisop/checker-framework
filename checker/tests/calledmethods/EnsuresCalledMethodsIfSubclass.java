import org.checkerframework.checker.calledmethods.qual.EnsuresCalledMethodsIf;

import java.io.Closeable;
import java.io.IOException;

public class EnsuresCalledMethodsIfSubclass {

    public static class Parent {
        @EnsuresCalledMethodsIf(expression = "#1", result = true, methods = "close")
        public boolean method(Closeable x) throws IOException {
            x.close();
            return true;
        }
    }

    public static class SubclassWrong extends Parent {
        @Override
        public boolean method(Closeable x) throws IOException {
            // ::error: (contracts.conditional.postcondition.not.satisfied)
            return true;
        }
    }

    public static class SubclassRight extends Parent {
        @Override
        public boolean method(Closeable x) throws IOException {
            return false;
        }
    }
}
