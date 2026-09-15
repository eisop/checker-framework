import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

// -Amode=jspecify assumes that every called method is pure (-AassumePure) and that assertions are
// enabled (-AassumeAssertionsAreEnabled). Without those options, both dereferences below are
// errors.
@NullMarked
public class JSpecifyModeAssumptions {
    @Nullable Object f;

    void sideEffect() {}

    void refinementSurvivesCall() {
        if (f != null) {
            sideEffect();
            // No expected error: the call is assumed not to change f.
            f.toString();
        }
    }

    void assertionRefines(@Nullable Object o) {
        assert o != null;
        // No expected error: the assertion is assumed to have run.
        o.toString();
    }
}
