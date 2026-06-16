import org.checkerframework.checker.pico.qual.Immutable;

@Immutable class ImmutableArray {
    private byte @Immutable [] _rgb;

    private String @Immutable [] _strings;

    // Immutable array of mutable objects is mutable.
    // ::error: (glacier.mutable.array.invalid)
    private java.util.Date @Immutable [] _dates;

    // MaybeMutable array of primitives is mutable.
    // ::error: (glacier.mutable.wholearray.invalid)
    private int[] _ints;
}
