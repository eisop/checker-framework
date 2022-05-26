import java.util.Arrays;
import java.util.List;

class Issue2722 {
    void foo() {
        passThrough(Arrays.asList("x")).get(0).length();
    }

    String bar() {
        return passThrough(Arrays.asList("x")).get(0);
    }

    <T> List<? extends T> passThrough(List<? extends T> object) {
        return object;
    }
}
