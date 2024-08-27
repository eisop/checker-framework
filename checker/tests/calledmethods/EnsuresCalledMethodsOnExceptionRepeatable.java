// Test that @EnsuresCalledMethodsOnException can be repeated.

import org.checkerframework.checker.calledmethods.qual.EnsuresCalledMethodsOnException;

import java.io.*;

class EnsuresCalledMethodsOnExceptionRepeatable {

    @EnsuresCalledMethodsOnException(value = "#1", methods = "close")
    @EnsuresCalledMethodsOnException(value = "#2", methods = "close")
    // ::error: (contracts.exceptional.postcondition.not.satisfied)
    public void close2MissingFirst(Closeable r1, Closeable r2) throws IOException {
        r1.close();
    }

    @EnsuresCalledMethodsOnException(value = "#1", methods = "close")
    @EnsuresCalledMethodsOnException(value = "#2", methods = "close")
    // ::error: (contracts.exceptional.postcondition.not.satisfied)
    public void close2MissingSecond(Closeable r1, Closeable r2) throws IOException {
        r2.close();
    }

    @EnsuresCalledMethodsOnException(value = "#1", methods = "close")
    @EnsuresCalledMethodsOnException(value = "#2", methods = "close")
    public void close2Correct(Closeable r1, Closeable r2) throws IOException {
        try {
            r1.close();
        } finally {
            r2.close();
        }
    }

    @EnsuresCalledMethodsOnException(value = "#1", methods = "close")
    @EnsuresCalledMethodsOnException(value = "#2", methods = "close")
    public void close2CorrectViaCall(Closeable r1, Closeable r2) throws IOException {
        close2Correct(r1, r2);
    }

    public static class Subclass extends EnsuresCalledMethodsOnExceptionRepeatable {
        @Override
        // ::error: (contracts.exceptional.postcondition.not.satisfied)
        public void close2Correct(Closeable r1, Closeable r2) throws IOException {
            throw new IOException();
        }
    }
}
