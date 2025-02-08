public class VarargsIncompatible {

    public static void test(int[] arr) {
        help(arr);
    }

    @SafeVarargs
    @SuppressWarnings("varargs")
    public static <T> void help(T... arr) {}
}
