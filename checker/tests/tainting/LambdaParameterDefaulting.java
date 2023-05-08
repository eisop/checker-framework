import org.checkerframework.checker.tainting.qual.Untainted;
import org.checkerframework.framework.qual.DefaultQualifier;
import org.checkerframework.framework.qual.TypeUseLocation;

import java.util.function.Function;

public class LambdaParameterDefaulting {
    @DefaultQualifier(locations = TypeUseLocation.PARAMETER, value = Untainted.class)
    void method() {
        // :: error: (lambda.param.type.incompatible)
        Function<String, String> function = (String s) -> untainted(s);
        Function<@Untainted String, String> function2 = (String s) -> untainted(s);
        Function<@Untainted String, @Untainted String> function3 = (String s) -> untainted(s);

        // :: error: (argument.type.incompatible)
        Function<String, String> function4 = s -> untainted(s);
        Function<@Untainted String, String> function5 = s -> untainted(s);
        Function<@Untainted String, @Untainted String> function6 = s -> untainted(s);
    }

    void method2() {
        // :: error: (argument.type.incompatible)
        Function<String, String> function = (String s) -> untainted(s);
        // :: error: (argument.type.incompatible)
        Function<@Untainted String, String> function2 = (String s) -> untainted(s);
        // :: error: (argument.type.incompatible)
        Function<@Untainted String, @Untainted String> function3 = (String s) -> untainted(s);

        // :: error: (argument.type.incompatible)
        Function<String, String> function4 = s -> untainted(s);
        Function<@Untainted String, String> function5 = s -> untainted(s);
        Function<@Untainted String, @Untainted String> function6 = s -> untainted(s);
    }

    @Untainted String untainted(@Untainted String s) {
        return s;
    }
}
