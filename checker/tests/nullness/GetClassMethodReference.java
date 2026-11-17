// A method reference to getClass returns the same type as an invocation on its receiver: for a
// receiver of type Foo, Class<? extends Foo> rather than the declared Class<?>.

import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Stream;

class GetClassMethodReference {
    interface Foo {}

    static <T> T take(Class<? extends T> c) {
        throw new AssertionError();
    }

    void boundAndUnbound(Foo f, Foo[] fs) {
        Supplier<Class<? extends Foo>> bound = f::getClass;
        Function<Foo, Class<? extends Foo>> unbound = Foo::getClass;
        Supplier<Class<? extends Foo[]>> array = fs::getClass;
        Supplier<Class<?>> self = this::getClass;
        Supplier<Class<? extends Object>> fromSuper = super::getClass;
        Function<Foo, Class<?>> object = Object::getClass;
    }

    void inference(Foo f, List<? extends Foo> ext) {
        Stream<Class<? extends Foo>> classes = ext.stream().map(Foo::getClass);
        Foo first = take(Stream.of(f).map(Foo::getClass).findFirst().get());
    }

    void nullableReceiver(@Nullable Foo nf) {
        // :: error: (methodref.receiver.invalid)
        Function<@Nullable Foo, Class<?>> unbound = Foo::getClass;
    }
}
