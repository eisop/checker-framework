import java.util.List;
import java.util.ArrayList;

public class TestAnnotatedJdk {
    public static void main(String[] args) {
        List<String> list = new ArrayList<>();
        list.add("hello");
        list.add(null); // Should emit error with annotated JDK
    }
}