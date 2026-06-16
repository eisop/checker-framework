import java.util.Map;

public class MapParameters {
    public static void takeMap(Map<String, ? extends Object> m) {}
    ;

    public void foo() {
        Map<String, Integer> m = null;
        takeMap(m);
    }
}
