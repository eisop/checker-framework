import org.checkerframework.checker.pico.qual.Immutable;
import org.checkerframework.checker.pico.qual.Mutable;
import org.checkerframework.checker.pico.qual.PolyMutable;
import org.checkerframework.checker.pico.qual.Readonly;
import org.checkerframework.checker.pico.qual.ReceiverDependentMutable;

/* @Immutable */
public class ClassAnnotation {

    /* @Immutable */ class ImmutableClassImplict {}

    @Immutable class ImmutableClassExplicit {}

    @Mutable class MutableClass {}

    @ReceiverDependentMutable class RecieverDependentMutableClass {}

    // :: error: class.bound.invalid
    @ReceiverDependentMutable static class RecieverDependentMutableStaticClass {}

    // :: error: class.bound.invalid
    @Readonly class ReadonlyClass {}

    // :: error: class.bound.invalid
    @PolyMutable class PolyMutableClass {}

    void testObjectCreation() {
        new /* @Immutable */ ImmutableClassImplict();
        new @Immutable ImmutableClassImplict();
        // :: error: constructor.invocation.invalid
        new @Mutable ImmutableClassImplict();
        new /* @Immutable */ ImmutableClassExplicit();
        new @Immutable ImmutableClassExplicit();
        // :: error: constructor.invocation.invalid
        new @Mutable ImmutableClassExplicit();
        new /* @Mutable */ MutableClass();
        // :: error: constructor.invocation.invalid
        new @Immutable MutableClass();
        new @Mutable MutableClass();
        @Immutable Object obj = new /* @Immutable */ RecieverDependentMutableClass();
        new @Immutable RecieverDependentMutableClass();
        new @Mutable RecieverDependentMutableClass();
        new @ReceiverDependentMutable RecieverDependentMutableClass();
        // :: error: constructor.invocation.invalid
        new @PolyMutable ImmutableClassImplict();
        // :: error: constructor.invocation.invalid
        new @Readonly ImmutableClassImplict();
        // :: error: constructor.invocation.invalid
        new @PolyMutable ImmutableClassExplicit();
        // :: error: constructor.invocation.invalid
        new @Readonly ImmutableClassExplicit();
        // :: error: constructor.invocation.invalid
        new @PolyMutable MutableClass();
        // :: error: constructor.invocation.invalid
        new @Readonly MutableClass();
        // TODO :: error: constructor.invocation.invalid
        new @PolyMutable RecieverDependentMutableClass();
        // :: error: constructor.invocation.invalid
        new @Readonly RecieverDependentMutableClass();
    }
}
