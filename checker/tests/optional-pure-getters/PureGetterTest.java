import java.util.Optional;

class PureGetterTest {

    @SuppressWarnings("optional.field")
    Optional<String> field;

    // This method will be treated as @Pure because of -AassumePureGetters.
    Optional<String> getOptional() {
        return Optional.of("hello");
    }

    Optional<String> otherOptional() {
        return Optional.of("hello");
    }

    void sideEffect() {}

    void foo() {
        if (field.isPresent()) {
            field.get();
        }
        if (field.isPresent()) {
            sideEffect();
            // :: error: (method.invocation.invalid)
            field.get();
        }
        if (field.isPresent()) {
            getOptional();
            field.get();
        }
        if (field.isPresent()) {
            otherOptional();
            // :: error: (method.invocation.invalid)
            field.get();
        }

        if (getOptional().isPresent()) {
            getOptional().get();
        }
        if (getOptional().isPresent()) {
            sideEffect();
            // :: error: (method.invocation.invalid)
            getOptional().get();
        }
        if (getOptional().isPresent()) {
            getOptional();
            getOptional().get();
        }
        if (getOptional().isPresent()) {
            otherOptional();
            // :: error: (method.invocation.invalid)
            getOptional().get();
        }

        if (otherOptional().isPresent()) {
            // BUG: https://github.com/typetools/checker-framework/issues/6291 error:
            // (method.invocation.invalid)
            otherOptional().get();
        }
        if (otherOptional().isPresent()) {
            sideEffect();
            // :: error: (method.invocation.invalid)
            otherOptional().get();
        }
        if (otherOptional().isPresent()) {
            getOptional();
            // BUG: https://github.com/typetools/checker-framework/issues/6291 error:
            // (method.invocation.invalid)
            otherOptional().get();
        }
        if (otherOptional().isPresent()) {
            otherOptional();
            // :: error: (method.invocation.invalid)
            otherOptional().get();
        }
    }
}
