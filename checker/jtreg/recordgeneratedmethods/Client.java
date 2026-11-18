/*
 * @test
 * @summary Test that the generated methods of a record from bytecode have the types of the
 * Record methods they implement, and that hand-written ones keep their own types.
 *
 * @requires jdk.version >= 17
 * @compile RecordLib.java
 * @compile/fail/ref=Client.out -XDrawDiagnostics -processor org.checkerframework.checker.nullness.NullnessChecker -Anomsgtext Client.java
 * @compile -processor org.checkerframework.checker.lock.LockChecker -Anomsgtext -Werror LockClient.java
 */

import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.function.Predicate;

public class Client {
    void generated(RecordLib.Item item) {
        item.equals(null);
        ((Record) item).equals(null);
        Predicate<@Nullable Object> p = item::equals;
        int h = item.hashCode();
        item.toString().length();
    }

    void generatedUnderDefaultQualifier(RecordLib.NullableDefault n) {
        n.equals(null);
        n.toString().length();
    }

    void handWritten(RecordLib.HandWrittenEquals h, RecordLib.FinalAnnotatedEquals f) {
        h.equals(null);
        f.equals(null);
    }

    void componentNamedEquals(RecordLib.ComponentNamedEquals c) {
        c.equals(null);
        c.equals().length();
    }
}
