import org.checkerframework.checker.pico.qual.Immutable;

@Immutable interface IIIC_ImmutableInterface {}

// :: error: glacier.interface.immutable
public class InvalidImmutableInterfaceClass implements Cloneable, IIIC_ImmutableInterface {}
