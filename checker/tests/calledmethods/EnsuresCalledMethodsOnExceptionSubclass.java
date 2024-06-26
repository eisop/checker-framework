// Test that @EnsuresCalledMethodsOnException is inherited by overridden methods.

import org.checkerframework.checker.calledmethods.qual.EnsuresCalledMethodsOnException;

import java.io.*;

public class EnsuresCalledMethodsOnExceptionSubclass {

    public static class Parent {
        @EnsuresCalledMethodsOnException(value = "#1", methods = "close")
        public void method(Closeable x) throws IOException {
            x.close();
        }
    }

    public static class SubclassWrong extends Parent {
        @Override
        // ::error: (contracts.exceptional.postcondition.not.satisfied)
        public void method(Closeable x) throws IOException {
            throw new IOException();
        }
    }

    public static class SubclassCorrect extends Parent {
        @Override
        public void method(Closeable x) throws IOException {
            // No exception thrown ==> no contract to satisfy!
        }
    }
}
