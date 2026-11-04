import java.util.Date;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Stream;

@SuppressWarnings("ainfertest") // only check WPI for crashes
public class GetClassTest {

    // See AnnotatedTypeFactory.adaptGetClassReturnTypeToReceiver

    void context() {
        Integer i = 4;
        i.getClass();
        Class<?> a = i.getClass();
        // Type arguments don't match
        @SuppressWarnings("fenum:assignment.type.incompatible")
        Class<? extends Object> b = i.getClass();
        @SuppressWarnings({
            "fenum:assignment.type.incompatible", // Type arguments don't match
            "signedness:assignment.type.incompatible" // Type arguments don't match
        })
        Class<? extends Integer> c = i.getClass();

        Class<?> d = i.getClass();
        // not legal Java; that is, does not type-check under Java rules
        // Class<Integer> e = i.getClass();
    }

    void m(Date d) {
        @SuppressWarnings("fenum:assignment.type.incompatible")
        Class<? extends Date> c = d.getClass();
    }

    // A method reference to getClass returns the same type as an invocation on its receiver.
    @SuppressWarnings({
        // The receiver of getClass is @GuardSatisfied, which a method reference cannot satisfy.
        "lock:methodref.receiver.invalid",
        "lock:methodref.receiver.bound.invalid"
    })
    void methodReferences(Integer i, List<? extends Date> dates) {
        Supplier<Class<?>> a = i::getClass;
        Function<Integer, Class<?>> b = Integer::getClass;
        // Type arguments don't match
        @SuppressWarnings("fenum:methodref.return.invalid")
        Stream<? extends Class<?>> c = dates.stream().map(Date::getClass);
    }
}
