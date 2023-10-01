package index;

import org.checkerframework.checker.index.qual.GTENegativeOne;
import org.checkerframework.checker.index.qual.SameLenBottom;
import org.checkerframework.checker.index.qual.SearchIndexBottom;
import org.checkerframework.checker.index.qual.UpperBoundBottom;
import org.checkerframework.common.value.qual.BottomVal;

/** This class tests that errors issued on the same tree are sorted by checker. */
public class OrderOfCheckers {
    // Ignore the test suite's usage of qualifiers in illegal locations.
    @SuppressWarnings("type.invalid.annotations.on.location")
    void test(int[] y) {
        @GTENegativeOne @UpperBoundBottom @SearchIndexBottom int @BottomVal @SameLenBottom [] x = y;
    }
}
