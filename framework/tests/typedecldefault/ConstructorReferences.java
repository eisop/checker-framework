import org.checkerframework.framework.testchecker.typedecldefault.quals.*;

import java.util.function.IntFunction;
import java.util.function.Supplier;

// A constructor reference has the type of the constructor's result, as a `new` expression does:
// @TypeDeclDefaultTop, the default for a constructor result, not @TypeDeclDefaultBottom, the
// default for a use. A qualifier written on the reference takes precedence. An array constructor
// reference, such as `Object[]::new`, has the type of the result of javac's constructor for arrays.
public class ConstructorReferences {
    void test() {
        // :: error: (assignment.type.incompatible)
        @TypeDeclDefaultBottom ConstructorReferences c = new ConstructorReferences();
        // :: error: (methodref.return.invalid)
        Supplier<@TypeDeclDefaultBottom ConstructorReferences> s = ConstructorReferences::new;
        Supplier<@TypeDeclDefaultTop ConstructorReferences> t = ConstructorReferences::new;
        Supplier<@TypeDeclDefaultBottom ConstructorReferences> u =
                @TypeDeclDefaultBottom ConstructorReferences::new;
    }

    void arrays() {
        // :: error: (methodref.return.invalid)
        IntFunction<Object @TypeDeclDefaultBottom []> s = Object[]::new;
        IntFunction<Object @TypeDeclDefaultTop []> t = Object[]::new;
        // :: error: (methodref.return.invalid)
        IntFunction<int @TypeDeclDefaultBottom []> u = int[]::new;
        IntFunction<Object @TypeDeclDefaultBottom []> v = Object @TypeDeclDefaultBottom []::new;
    }
}
