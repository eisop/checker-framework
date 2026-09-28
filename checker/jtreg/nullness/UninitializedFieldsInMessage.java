/*
 * @test
 * @summary The Initialization Checker names the fields that are still uninitialized when it
 *          rejects a method call on a partially-initialized receiver.  eisop#622.
 * @compile/fail/ref=UninitializedFieldsInMessage.out -XDrawDiagnostics -processor org.checkerframework.checker.nullness.NullnessChecker UninitializedFieldsInMessage.java
 */

public class UninitializedFieldsInMessage {
    String s;
    Object foo;

    // The call sits between the two field initializations, so only "foo" is named.
    UninitializedFieldsInMessage() {
        s = "";
        init();
        foo = "";
    }

    // The call precedes both initializations, so both fields are named.
    UninitializedFieldsInMessage(int x) {
        init();
        s = "";
        foo = "";
    }

    void init() {}
}
