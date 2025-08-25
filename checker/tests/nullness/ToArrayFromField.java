import org.checkerframework.checker.nullness.qual.NonNull;

import java.util.ArrayList;
import java.util.List;
// Related jtreg test: checker/jtreg/nullness/ToArrayHeuristic/ToArrayHeuristic.java
public class ToArrayFromField {

    private final String[] EMPTY_A = new String[0];
    private final String[] EMPTY_B = {};
    private final String[] EMPTY_C = new String[] {};

    private static final String[] NONEMPTY = {"x"};

    private final List<@NonNull String> nonnullList = new ArrayList<>();

    void okLoopA() {
        for (@NonNull String s : nonnullList.toArray(EMPTY_A)) {}
    }

    void okLoopB() {
        for (@NonNull String s : nonnullList.toArray(EMPTY_B)) {}
    }

    void okLoopC() {
        for (@NonNull String s : nonnullList.toArray(EMPTY_C)) {}
    }

    String[] okReturn() {
        return nonnullList.toArray(EMPTY_B);
    }

    String[] badReturn() {
        // :: error: (return.type.incompatible)
        // :: warning: (toarray.nullable.elements.not.newarray)
        return nonnullList.toArray(NONEMPTY);
    }
}
