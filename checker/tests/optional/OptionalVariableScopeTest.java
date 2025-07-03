import org.checkerframework.checker.optional.qual.*;

import java.util.Optional;

class OptionalVariableScopeTest {

    @Present Optional<String> getPresentOpt() {
        throw new Error();
    }

    Optional<String> getOpt() {
        throw new Error();
    }

    void m1() {
        @Present Optional<String> presentOpt = getPresentOpt();
        {
            presentOpt.get();
            getPresentOpt().get();
            @Present Optional<String> anotherPresentOpt = getPresentOpt();
            anotherPresentOpt.get();
        }
    }

    void m2() {
        Optional<String> opt = getOpt();
        {
            // :: error: (method.invocation.invalid)
            opt.get();
            // :: error: (method.invocation.invalid)
            getOpt().get();
            Optional<String> anotherOpt = getOpt();
            // :: error: (method.invocation.invalid)
            anotherOpt.get();
        }
    }
}
