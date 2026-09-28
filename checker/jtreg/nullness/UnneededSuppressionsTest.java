/*
 * @test
 * @summary Test -AwarnUnneededSuppressions
 *
 * @compile/ref=UnneededSuppressionsTest.out -XDrawDiagnostics -processor org.checkerframework.checker.nullness.NullnessChecker -AwarnUnneededSuppressions UnneededSuppressionsTest.java
 */

import org.checkerframework.checker.nullness.qual.Nullable;

class UnneededSuppressionsTest {

    @SuppressWarnings({"nullness:return.type.incompatible"})
    public String getClassAndUid1() {
        return "hello";
    }

    @SuppressWarnings({"nullness:return.type.incompatible", "unneeded.suppression"})
    public String getClassAndUid2() {
        return "hello";
    }

    @SuppressWarnings({"nullness:return.type.incompatible", "nullness:unneeded.suppression"})
    public String getClassAndUid3() {
        return "hello";
    }

    @SuppressWarnings({"unneeded.suppression", "nullness:return.type.incompatible"})
    public String getClassAndUid5() {
        return "hello";
    }

    @SuppressWarnings({"nullness:unneeded.suppression", "nullness:return.type.incompatible"})
    public String getClassAndUid6() {
        return "hello";
    }

    // A @SuppressWarnings whose value is exactly a checker prefix suppresses every warning of that
    // checker, including the unneeded.suppression warning about itself if that suppression turns
    // out not to be needed.

    @SuppressWarnings("nullness") // needed: suppresses the dereference below
    void neededNullnessPrefix(@Nullable Object o) {
        o.toString();
    }

    @SuppressWarnings("nullness")
    void unneededNullnessPrefix() {
        Object o = new Object();
        o.toString();
    }

    @SuppressWarnings("allcheckers")
    void unneededAllcheckersPrefix() {
        Object o = new Object();
        o.toString();
    }
}
