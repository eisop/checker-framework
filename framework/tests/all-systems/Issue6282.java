import java.lang.invoke.MethodHandle;
import java.lang.reflect.AccessibleObject;

@SuppressWarnings("ainfertest") // only check WPI for crashes
public class Issue6282 {
    public static final MethodHandle setAccessible0_Method = setAccessible0_Method();

    public static final MethodHandle setAccessible0_Method() {
        throw new RuntimeException();
    }

    public static void setAccessible(final AccessibleObject accessibleObject) {
        try {
            // MethodHandle.invokeExact's polymorphic-signature return is @Nullable on an
            // annotated JDK. Class initialization calls setAccessible0_Method(), which always
            // throws, before this line can ever run, so the cast can never actually unbox a null.
            @SuppressWarnings("cast.unsafe")
            boolean newFlag = (boolean) setAccessible0_Method.invokeExact(accessibleObject, true);
            assert newFlag;
        } catch (Throwable throwable) {
            throw new AssertionError(throwable);
        }
    }
}
