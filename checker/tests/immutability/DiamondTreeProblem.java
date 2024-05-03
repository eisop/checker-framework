import org.checkerframework.checker.immutability.qual.Immutable;

import java.util.ArrayList;
import java.util.List;

public class DiamondTreeProblem {

    void test1() {
        @Immutable List<String> l = new @Immutable ArrayList<>();
    }

    void test2() {
        @Immutable List<String> l = new @Immutable ArrayList<String>();
    }
}
