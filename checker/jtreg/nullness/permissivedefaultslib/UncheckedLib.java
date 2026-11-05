package permissivedefaultslib;

/** Unchecked code: not {@code @AnnotatedFor("nullness")}. */
public class UncheckedLib {
    public static Object get() {
        return "";
    }

    public static void take(Object value) {}
}
