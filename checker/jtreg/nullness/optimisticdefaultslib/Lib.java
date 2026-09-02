package optimisticdefaultslib;

public class Lib<T> {
    public static Object getObject() {
        return null;
    }

    public static void setObject(Object value) {}

    public static void upper(Lib<? extends Object> value) {}

    public static void lower(Lib<? super Object> value) {}
}
