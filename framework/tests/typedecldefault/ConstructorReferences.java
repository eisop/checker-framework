import org.checkerframework.framework.testchecker.typedecldefault.quals.*;

import java.util.function.Supplier;

// A constructor reference has the type of the constructor's result, as a `new` expression does:
// @TypeDeclDefaultTop, the default for a constructor result, not @TypeDeclDefaultBottom, the
// default for a use. A qualifier written on the reference takes precedence.
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
}
