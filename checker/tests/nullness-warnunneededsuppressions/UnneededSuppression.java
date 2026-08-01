import org.checkerframework.checker.nullness.qual.Nullable;

// Tests that -AwarnUnneededSuppressions reports a bare-prefix @SuppressWarnings (one whose value is
// exactly a checker prefix) that suppresses nothing. Such a suppression suppresses every warning of
// the checker, so it must not suppress the "unneeded.suppression" warning about itself.
public class UnneededSuppression {

    @SuppressWarnings("nullness") // needed: suppresses the dereference below
    void needed(@Nullable Object o) {
        o.toString();
    }

    // :: warning: (unneeded.suppression)
    @SuppressWarnings("nullness")
    void unneededNullnessPrefix() {
        Object o = new Object();
        o.toString();
    }

    // :: warning: (unneeded.suppression)
    @SuppressWarnings("allcheckers")
    void unneededAllcheckersPrefix() {
        Object o = new Object();
        o.toString();
    }
}
