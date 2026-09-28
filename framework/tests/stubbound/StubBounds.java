import org.checkerframework.framework.testchecker.nontopdefault.qual.NTDBottom;
import org.checkerframework.framework.testchecker.nontopdefault.qual.NTDMiddle;
import org.checkerframework.framework.testchecker.nontopdefault.qual.NTDTop;

import java.util.List;
import java.util.Map;

// A stub file's explicit annotation on a type parameter's upper bound must be honored, whether
// it widens or narrows the bound beyond the checker's own UPPER_BOUND default (@NTDMiddle here).
@SuppressWarnings("inconsistent.constructor.type")
public class StubBounds {
    void widen() {
        Map<@NTDTop Object, @NTDTop Object> ok = null;
        Map<@NTDMiddle Object, @NTDMiddle Object> check = null;
    }

    void narrow() {
        // :: error: (type.argument.type.incompatible)
        List<@NTDMiddle Object> ok = null;
        List<@NTDBottom Object> okNarrow = null;
    }
}
