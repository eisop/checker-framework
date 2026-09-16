import org.checkerframework.checker.tainting.qual.PolyTainted;
import org.checkerframework.checker.tainting.qual.Tainted;
import org.checkerframework.checker.tainting.qual.Untainted;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

public class MemberReferenceInference {
    void clever2(
            Stream<Optional<BigDecimal>> taintedStream,
            Stream<Optional<@Untainted BigDecimal>> untaintedStream) {
        // :: error: (type.arguments.not.inferred)
        Stream<@Untainted BigDecimal> s = taintedStream.map(Optional::get);
        Stream<@Untainted BigDecimal> s2 = untaintedStream.map(Optional::get);
        Stream<@Tainted BigDecimal> s3 = taintedStream.map(Optional::get);
        Stream<@Tainted BigDecimal> s4 = untaintedStream.map(Optional::get);
    }

    interface MyClass<Q> {
        String getName();
    }

    void method(
            MyClass<? extends String> clazz,
            Map<MyClass<? extends String>, @Untainted String> annotationClassNames) {
        // :: error: (type.arguments.not.inferred)
        String canonicalName = annotationClassNames.computeIfAbsent(clazz, MyClass::getName);
    }

    void method2(
            MyClass<? extends String> clazz,
            Map<MyClass<? extends String>, String> annotationClassNames) {
        String canonicalName = annotationClassNames.computeIfAbsent(clazz, MyClass::getName);
    }

    interface PolyClass<Q> {
        @PolyTainted String getName(@PolyTainted PolyClass<Q> this);
    }

    // The unbound method reference PolyClass::getName is, like MyClass::getName above, the
    // top-level poly expression for which type-argument inference runs (not a nested argument to
    // an enclosing generic call), so this exercises
    // InvocationTypeInference.infer(MemberReferenceTree)
    // rather than Expression.reduceMethodRef. @PolyTainted resolves against clazz's @Untainted
    // receiver type to @Untainted, matching the map's @Untainted value type.
    void polyMethod(
            @Untainted PolyClass<? extends String> clazz,
            Map<@Untainted PolyClass<? extends String>, @Untainted String> annotationClassNames) {
        String canonicalName = annotationClassNames.computeIfAbsent(clazz, PolyClass::getName);
    }

    // Same shape, but the receiver's declared type carries no qualifier, so @PolyTainted resolves
    // to the default, @Tainted, which does not satisfy the map's @Untainted value type.
    void polyMethodMismatch(
            PolyClass<? extends String> clazz,
            Map<PolyClass<? extends String>, @Untainted String> annotationClassNames) {
        // :: error: (type.arguments.not.inferred)
        String canonicalName = annotationClassNames.computeIfAbsent(clazz, PolyClass::getName);
    }

    static class PolyException extends Exception {}

    @FunctionalInterface
    interface UntaintedThrowingConsumer<E extends Throwable> {
        void accept(@Untainted String s) throws E;
    }

    @FunctionalInterface
    interface TaintedThrowingConsumer<E extends Throwable> {
        void accept(@Tainted String s) throws E;
    }

    static void consume(@PolyTainted String s) throws @PolyTainted PolyException {}

    static <E extends Throwable> E runUntainted(UntaintedThrowingConsumer<E> job) throws E {
        throw new AssertionError();
    }

    static <E extends Throwable> E runTainted(TaintedThrowingConsumer<E> job) throws E {
        throw new AssertionError();
    }

    // The exception type variable E is inferred from a checked-exception constraint, which is
    // built from the thrown types of the method reference's compile-time declaration. @PolyTainted
    // on that thrown type resolves against the function type's @Untainted parameter to @Untainted,
    // so E is @Untainted PolyException.
    void polyThrows() throws PolyException {
        @Untainted PolyException e = runUntainted(MemberReferenceInference::consume);
    }

    // Same shape, but the function type's parameter is @Tainted, so @PolyTainted resolves to
    // @Tainted, which does not satisfy the @Untainted target of the assignment.
    void polyThrowsMismatch() throws PolyException {
        // :: error: (assignment.type.incompatible) :: error: (type.arguments.not.inferred)
        @Untainted PolyException e = runTainted(MemberReferenceInference::consume);
    }
}
