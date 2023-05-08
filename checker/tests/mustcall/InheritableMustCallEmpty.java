// A simple test for @InheritableMustCall({}).

import org.checkerframework.checker.mustcall.qual.*;

import java.io.*;

public class InheritableMustCallEmpty {

    @InheritableMustCall({})
    // :: error: inconsistent.mustcall.subtype
    class NoObligationCloseable implements Closeable {
        @Override
        public void close() throws IOException {
            // no resource, nothing to do
        }
    }

    @InheritableMustCall()
    // :: error: inconsistent.mustcall.subtype
    class NoObligationCloseable2 implements Closeable {
        @Override
        public void close() throws IOException {
            // no resource, nothing to do
        }
    }
}
