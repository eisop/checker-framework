import org.checkerframework.checker.immutability.qual.Immutable;
import org.checkerframework.checker.immutability.qual.Mutable;

public class DeepMutable {
    @Mutable
    static class MutableBox {}

    @Immutable
    static class ImmutableClass {

        // :: error: (implicit.shallow.immutable)
        MutableBox implicit = new MutableBox();

        @Mutable MutableBox explicit = new MutableBox();
    }

    @Immutable
    static class ImmutableGenericEx<T extends @Immutable Object> {

        T t;

        @Immutable
        ImmutableGenericEx(T t) {
            this.t = t;
        }
    }

    @Immutable
    static class ImmutableGenericIm<T extends MutableBox> {
        // :: error: (implicit.shallow.immutable)
        T t;

        @Immutable
        ImmutableGenericIm(T t) {
            this.t = t;
        }
    }
}
