import org.checkerframework.checker.pico.qual.Immutable;

interface IF_AnInterface {}
;

@Immutable interface IF_ImmutableInterface extends IF_AnInterface {}
;

@Immutable public class InterfaceField {
    // ::error: (glacier.mutable.invalid)
    IF_AnInterface o;
    IF_ImmutableInterface o2;
}
