import org.checkerframework.checker.pico.qual.Immutable;
import org.checkerframework.checker.pico.qual.Mutable;
import org.checkerframework.checker.pico.qual.PolyMutable;
import org.checkerframework.checker.pico.qual.Readonly;
import org.checkerframework.checker.pico.qual.ReceiverDependentMutable;

/**
 * This test case aims to showing the validity of annotation class and object creation.
 * 
 * Annotation @Immutable, @Mutable, @ReceiverDependentMutable are valid annotations for
 * class/interface and object creation.
 */
/* @Immutable */
public class ClassAnnotation {
    /* @Immutable */ interface ImmutableInterfaceImplict {}

    @Immutable interface ImmutableInterfaceExplict {}

    @Mutable interface MutableInterface {}

    @ReceiverDependentMutable interface RDMInterface {}

    // :: error: class.bound.invalid
    @Readonly interface ReadonlyInterface {}

    // :: error: class.bound.invalid
    @PolyMutable interface PolyMutableInterface {}

    /* @Immutable */ abstract class ImmutableAbstractClassImplict {}

    @Immutable abstract class ImmutableAbstractClassExplicit {}

    @Mutable abstract class MutableAbstractClass {}

    @ReceiverDependentMutable abstract class RDMAbstractClass {}

    // :: error: class.bound.invalid
    @Readonly abstract class ReadonlyAbstractClass {}

    // :: error: class.bound.invalid
    @PolyMutable abstract class PolyMutableAbstractClass {}

    /* @Immutable */ class ImmutableClassImplict {}

    @Immutable class ImmutableClassExplicit {}

    @Mutable class MutableClass {}

    @ReceiverDependentMutable class RMDClass {}

    // :: error: class.bound.invalid
    @ReceiverDependentMutable static class RDMStaticClass {}

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
        @Immutable Object obj = new /* @Immutable */ RMDClass();
        new @Immutable RMDClass();
        new @Mutable RMDClass();
        new @ReceiverDependentMutable RMDClass();
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
        new @PolyMutable RMDClass();
        // :: error: constructor.invocation.invalid
        new @Readonly RMDClass();
    }
}
