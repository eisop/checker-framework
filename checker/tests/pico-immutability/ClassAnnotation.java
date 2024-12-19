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
}
