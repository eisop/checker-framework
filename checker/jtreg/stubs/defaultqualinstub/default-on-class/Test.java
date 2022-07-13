import org.checkerframework.checker.nullness.qual.*;

/*
 * @test
 * @summary Defaults applied on class will not be inherited.
 *
 * @compile -XDrawDiagnostics -Xlint:unchecked List.java
 * @compile -XDrawDiagnostics -Xlint:unchecked MutableList.java
 * @compile/fail/ref=test.out -XDrawDiagnostics -Xlint:unchecked -processor org.checkerframework.checker.nullness.NullnessChecker -Anomsgtext -Astubs=list.astub -Werror Test.java
 */

public class Test {
    void foo(List<?> list, MutableList<?> mutableList) {
        // as defined in list.astub, the extends bound for elements in `list` is @NonNull by default
        @NonNull Object o1 = list.get(0);

        // the extends bound is @Nullable for elements in `mutableList` by default
        @NonNull Object o2 = mutableList.get(0);

        // invalid assignment
        List<?> list2 = mutableList;
    }
}
