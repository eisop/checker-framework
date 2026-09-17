import org.checkerframework.checker.initialization.qual.EnsuresInitialized;
import org.checkerframework.checker.initialization.qual.UnderInitialization;

public class EnsuresInitializedPrimitive {
    int b;
    int lyingField;
    int branchField;

    public EnsuresInitializedPrimitive(boolean flag) {
        initB();
        initLying();
        initBranch(flag);
    }

    @EnsuresInitialized("this.b")
    private void initB(
            @UnderInitialization(EnsuresInitializedPrimitive.class) EnsuresInitializedPrimitive this) {
        this.b = 5;
    }

    @EnsuresInitialized("this.lyingField")
    // :: error: (contracts.postcondition.not.satisfied)
    void initLying(
            @UnderInitialization(EnsuresInitializedPrimitive.class) EnsuresInitializedPrimitive this) {
        // Lying: no assignment at all!
    }

    @EnsuresInitialized("this.branchField")
    // :: error: (contracts.postcondition.not.satisfied)
    void initBranch(
            @UnderInitialization(EnsuresInitializedPrimitive.class) EnsuresInitializedPrimitive this,
            boolean flag) {
        if (flag) {
            this.branchField = 5;
        }
        // Branching: no assignment on the else branch!
    }
}
