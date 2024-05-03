import org.checkerframework.checker.immutability.qual.Immutable;
import org.checkerframework.checker.immutability.qual.Mutable;
import org.checkerframework.checker.immutability.qual.Readonly;
import org.checkerframework.checker.immutability.qual.ReceiverDependantMutable;

// ok
@Immutable
class ImmutableClass2<T extends @Readonly Object> {
    @Immutable
    ImmutableClass2() {}
}

// ok
@Immutable
class ImmutableClass3<T extends @ReceiverDependantMutable Object> {
    @Immutable
    ImmutableClass3() {}
}

// ok
@Immutable
class ImmutableClass4<T extends @Mutable Object> {
    @Immutable
    ImmutableClass4() {}
}

// ok
@Immutable
class ImmutableClass5<T extends @Mutable Object, S extends T> {
    @Immutable
    ImmutableClass5() {}
}

@Immutable
class ImmutableClass6<T extends @Immutable Object> {
    @Immutable
    ImmutableClass6() {}
}

@Immutable
class ImmutableClass7 {
    @Immutable
    ImmutableClass7() {}

    // Should NOT have warnings for type parameter with non-immutable upper bound
    // if the type parameter is declared on generic method(?)
    <S extends @Mutable Object> S foo(@Immutable ImmutableClass7 this) {
        return null;
    }
}
