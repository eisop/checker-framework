import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.checker.nullness.qual.PolyNull;

// The type of an assignment to a field depends on the refined types of its subexpressions, so it
// must not be cached while the dataflow analysis is still iterating over a loop (typetools#602).
public class FlowDependentFieldAssignmentLoop {
    @Nullable String f;

    @PolyNull String id(@PolyNull String o) {
        return o;
    }

    boolean c() {
        return true;
    }

    void assignInLoop(@Nullable FlowDependentFieldAssignmentLoop o, @Nullable String m) {
        if (o == null) {
            return;
        }
        @Nullable String n = "";
        while (c()) {
            o.f = id(n);
            // :: error: (dereference.of.nullable)
            o.f.toString();
            n = m;
        }
    }

    void assignmentValueInLoop(@Nullable FlowDependentFieldAssignmentLoop o, @Nullable String m) {
        if (o == null) {
            return;
        }
        @Nullable String n = "";
        while (c()) {
            // :: error: (dereference.of.nullable)
            (o.f = id(n)).toString();
            n = m;
        }
    }
}
