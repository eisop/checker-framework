package permissivedefaultslib;

/** Unchecked code: not {@code @AnnotatedFor("nullness")}. */
public class UnsoundLib {
    public void take(String value) {}

    public String get() {
        return "";
    }
}
