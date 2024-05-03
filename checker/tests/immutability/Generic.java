import org.checkerframework.checker.immutability.qual.Immutable;
import org.checkerframework.checker.immutability.qual.Mutable;
import org.checkerframework.checker.immutability.qual.Readonly;

import java.util.Date;

/*If upper bound is @Readonly, @Mutable, type parameter is not in the abstract state of
 * the entire object*/
@Immutable
class Wrapper<T> {
    T t;

    @Immutable
    Wrapper(T t) {
        this.t = t;
    }
}

public class Generic {
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
}
