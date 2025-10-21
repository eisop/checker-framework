// @below-java9-jdk-skip-test

import java.util.Collections;

public class Issue6856 {
    static void test(Class<String> p) {
        var x = Collections.checkedCollection((Collections.emptyList()), p);
    }
}
