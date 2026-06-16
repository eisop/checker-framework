import org.checkerframework.checker.pico.qual.*;

@Immutable class ImmutClass {}

@Mutable class MutableClass {}

class DefaultMutableClass {}

@Immutable interface ImmutInterface {}

interface MutableInterface {}

public class ConflictingAnnotations {
    // ::error: (type.invalid.annotations.on.use)
    @Mutable ImmutClass o1;

    // ::error: (type.invalid.annotations.on.use)
    @Immutable MutableClass o2;

    // ::error: (type.invalid.annotations.on.use)
    @Immutable DefaultMutableClass o3;

    // ::error: (type.invalid.annotations.on.use)
    @Mutable ImmutInterface i1;

    // ::error: (type.invalid.annotations.on.use)
    @Immutable MutableInterface i2;
}
