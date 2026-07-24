import viewpointtest.quals.*;

public class ConstructorTypeVariableBounds {
    static class MyClass {
        <T extends @ReceiverDependentQual Object> MyClass(T t) {}
    }

    void test(@Top Object top, @A Object a, @B Object b, @Bottom Object bottom) {

        // :: error: (type.argument.type.incompatible) :: error: (new.class.type.invalid)
        new <@Top Object>@Top MyClass(top);

        // :: warning: (cast.unsafe.constructor.invocation)
        new <@A Object>@A MyClass(a);

        // :: warning: (cast.unsafe.constructor.invocation)
        new <@B Object>@B MyClass(b);
    }
}
