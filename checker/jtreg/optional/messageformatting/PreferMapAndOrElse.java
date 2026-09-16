/*
 * @test
 * @summary Ensure the "prefer.map.and.orelse" warning message formats correctly, instead of
 *   crashing, for the declaration-initializer pattern with no else branch:
 *   `if (VAR.isPresent()) { TYPE x = METHOD(VAR.get()); }`. This is a regression test for a
 *   message-arity bug: OptionalImplVisitor#checkConditionalStatementIsPresentGetCall used to pass
 *   only 2 arguments to checker.reportWarning for this message key, but the key's
 *   messages.properties format string has 3 "%s" placeholders. Every JUnit test in this project
 *   runs with -Anomsgtext, which skips String.format entirely, so the mismatch was invisible
 *   there; a real invocation throws a MissingFormatArgumentException wrapped in BugInCF. This
 *   jtreg test does not pass -Anomsgtext, so it exercises the actual String.format call and
 *   compares the formatted message text against the reference output.
 *
 * @compile/ref=PreferMapAndOrElse.out -XDrawDiagnostics -processor org.checkerframework.checker.optional.OptionalChecker PreferMapAndOrElse.java
 */

import java.util.Optional;

public class PreferMapAndOrElse {

    static class Customer {}

    static Customer identity(Customer c) {
        return c;
    }

    @SuppressWarnings("optional:parameter")
    void m(Optional<Customer> optCustomer) {
        if (optCustomer.isPresent()) {
            Customer c = identity(optCustomer.get());
        }
    }
}
