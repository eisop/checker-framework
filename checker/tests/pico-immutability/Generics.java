import org.checkerframework.checker.pico.qual.Immutable;
import org.checkerframework.checker.pico.qual.Mutable;
import org.checkerframework.checker.pico.qual.Readonly;

import java.util.Date;

public class Generics {
    @Mutable static class MutableBox {}

    @Immutable static class ImmutableClass {

        // :: error: (implicit.shallow.immutable)
        MutableBox implicit = new MutableBox();

        @Mutable MutableBox explicit = new MutableBox();
    }

    @Immutable static class ImmutableGenericEx<T extends @Immutable Object> {

        T t;

        @Immutable ImmutableGenericEx(T t) {
            this.t = t;
        }
    }

    @Immutable static class ImmutableGenericIm<T extends MutableBox> {
        // :: error: (implicit.shallow.immutable)
        T t; // RDA

        @Immutable ImmutableGenericIm(T t) {
            this.t = t;
        }
    }

    void test() {
        @Immutable ImmutableGenericIm<MutableBox> t = new ImmutableGenericIm<MutableBox>(new MutableBox());
        // :: error: (illegal.field.write)
        t.t = new MutableBox();
    }

    @Immutable class Wrapper<T> {
        T t;

        @Immutable Wrapper(T t) {
            this.t = t;
        }
    }

    void test1() {
        @Mutable Object arg = new @Mutable Object();
        @Immutable Wrapper<@Mutable Object> wrapper = new @Immutable Wrapper<@Mutable Object>(arg);
        /*Since t is not in the abstract state, we can get a mutable object out of an immutable
        object. This is just like we have mutable elements in immutable list, those mutable
        elements are not in the abstract state of the list*/
        @Mutable Object mo = wrapper.t;
    }

    void test2() {
        @Mutable Date md = new @Mutable Date();
        @Readonly Date spy = md;
        @Immutable Wrapper<@Readonly Date> victim = new @Immutable Wrapper<@Readonly Date>(spy);
        /*Same argument as above*/
        md.setTime(123L);
    }

    @Mutable interface MIt<E extends @Readonly Object> {
        E next();
    }

    void raw() {
        @Mutable MIt raw = null;

        // Using optimictic uninferred type arguments, so it is
        // allowed
        // :: error: (assignment.type.incompatible)
        @Immutable Object ro = raw.next();
    }
}
