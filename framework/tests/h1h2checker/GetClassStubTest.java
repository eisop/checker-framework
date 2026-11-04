package h1h2checker;

import org.checkerframework.framework.testchecker.h1h2checker.quals.*;

import java.util.function.Function;
import java.util.function.Supplier;

public class GetClassStubTest {

    // See AnnotatedTypeFactory.adaptGetClassReturnTypeToReceiver
    void context() {
        Integer i = 4;
        Class<?> a = i.getClass();

        Class<@H1Bot ? extends @H1S1 Object> succeed1 = i.getClass();
        Class<@H1Bot ? extends @H1S1 Integer> succeed2 = i.getClass();

        // :: error: (assignment.type.incompatible)
        Class<@H1Bot ? extends @H1Bot Object> fail1 = i.getClass();

        // :: error: (assignment.type.incompatible)
        Class<@H1Bot ?> fail2 = i.getClass();
    }

    // A method reference to getClass returns the same type as an invocation on its receiver.
    void methodReferences(Integer i) {
        Supplier<Class<?>> a = i::getClass;

        Supplier<Class<@H1Bot ? extends @H1S1 Object>> succeed1 = i::getClass;
        Supplier<Class<@H1Bot ? extends @H1S1 Integer>> succeed2 = i::getClass;
        Function<Integer, Class<@H1Bot ? extends @H1S1 Integer>> succeed3 = Integer::getClass;

        // :: error: (methodref.return.invalid)
        Supplier<Class<@H1Bot ? extends @H1Bot Object>> fail1 = i::getClass;

        // :: error: (methodref.return.invalid)
        Supplier<Class<@H1Bot ?>> fail2 = i::getClass;

        // :: error: (methodref.return.invalid)
        Function<Integer, Class<@H1Bot ? extends @H1Bot Integer>> fail3 = Integer::getClass;
    }
}
