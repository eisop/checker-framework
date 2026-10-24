import org.checkerframework.checker.calledmethods.qual.EnsuresCalledMethods;
import org.checkerframework.checker.mustcall.qual.MustCall;

import java.io.Closeable;
import java.io.IOException;
import java.util.Collections;
import java.util.List;

// Calls whose postconditions name a local variable, wrapped in the artificial trees that the CFG
// creates: boxing and unboxing, string concatenation, varargs arrays, enhanced-for loops,
// try-with-resources, and postfix increments.
class ArtificialTreeContracts {

    @MustCall("close") static class Res implements Closeable {
        @Override
        public void close() {}
    }

    @EnsuresCalledMethods(value = "#1", methods = "close")
    static int closeAndCount(Closeable c) throws IOException {
        c.close();
        return 1;
    }

    @EnsuresCalledMethods(value = "#1", methods = "close")
    static List<String> closeAndList(Closeable c) throws IOException {
        c.close();
        return Collections.emptyList();
    }

    static void varargs(Object... os) {}

    void boxing() throws IOException {
        Res r = new Res();
        Integer boxed = closeAndCount(r);
        int unboxed = boxed;
    }

    void unboxing() throws IOException {
        Res r = new Res();
        Integer total = 0;
        total += closeAndCount(r);
    }

    void concatenation() throws IOException {
        Res r = new Res();
        String s = "count: " + closeAndCount(r);
        Res r2 = new Res();
        s += closeAndCount(r2);
    }

    void varargsArray() throws IOException {
        Res r = new Res();
        varargs(closeAndCount(r), "x");
    }

    void enhancedFor() throws IOException {
        Res r = new Res();
        for (String s : closeAndList(r)) {}
    }

    void enhancedForOverArray() throws IOException {
        Res r = new Res();
        for (int i : new int[] {closeAndCount(r)}) {}
    }

    void tryWithResources() throws IOException {
        Res r = new Res();
        try (Res inner = new Res()) {
            closeAndCount(r);
        }
    }

    void postfix() throws IOException {
        Res r = new Res();
        int i = 0;
        int[] a = new int[2];
        a[i++] = closeAndCount(r);
        i--;
    }

    void lambda() {
        java.util.concurrent.Callable<Integer> call =
                () -> {
                    Res r = new Res();
                    Integer boxed = closeAndCount(r);
                    return boxed;
                };
    }

    void leak() throws IOException {
        // :: error: (required.method.not.called)
        Res r = new Res();
        Integer boxed = 1;
        boxed++;
    }
}
