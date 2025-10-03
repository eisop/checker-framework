import java.lang.invoke.MethodHandle;

/**
 * For signature polymorphic methods, javac sometimes does not include the corresponding vararg
 * parameter in the method type. This seems to happen for no-arg invocations and invocations with a
 * single argument of a primitive type.
 *
 * <p>We work around this issue in TreeUtils.isVarargsCall() and TreeUtils.isSignaturePolymorphic.
 */
public class Issue6078 {
    static void call(MethodHandle methodHandle) throws Throwable {
        methodHandle.invoke();
        methodHandle.invoke("");
        methodHandle.invoke(1);
        methodHandle.invokeExact(true);
    }

    static void call(MethodHandle methodHandle, Object[] array) throws Throwable {
        methodHandle.invoke(array);
    }

    @SuppressWarnings("nullness:argument") // invoke is annotated conservatively.
    static void callNull(MethodHandle methodHandle) throws Throwable {
        methodHandle.invoke(null);
    }

    void use() {
        foo();
    }

    @SafeVarargs
    private final <T> void foo(T... ts) {}
}
