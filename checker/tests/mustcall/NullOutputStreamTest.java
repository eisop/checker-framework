// @below-java11-jdk-skip-test OutputStream.nullOutputStream() was introduced in JDK 11.

import org.checkerframework.checker.mustcall.qual.MustCall;

import java.io.OutputStream;

class NullOutputStreamTest {

    void m() {
        @MustCall() OutputStream nullOS = OutputStream.nullOutputStream();
    }
}
