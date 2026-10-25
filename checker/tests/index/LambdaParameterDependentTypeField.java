// @below-java14-jdk-skip-test
// On JDK 8 and 11, javac does not find the parameter of a lambda in a field initializer.

import org.checkerframework.checker.index.qual.LTEqLengthOf;

import java.util.function.Function;

public class LambdaParameterDependentTypeField {
    Function<int[], Integer> fieldInitializer =
            a -> {
                @LTEqLengthOf("a") int j = a.length;
                return j;
            };
}
