import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.checker.nullness.qual.PolyNull;
import org.checkerframework.checker.optional.qual.Present;

import java.util.Optional;

public class OptionalMapMethodReference {
    Optional<String> getString() {
        return Optional.of("");
    }

    @Present Optional<Integer> method() {
        Optional<String> o = getString();
        @Present Optional<Integer> oInt;
        if (o.isPresent()) {
            // :: error: (assignment.type.incompatible)
            oInt = o.map(this::convertNull);
            oInt = o.map(this::convertPoly);
            return o.map(this::convert);
        }
        return Optional.of(0);
    }

    @Nullable Integer convertNull(String s) {
        return null;
    }

    @PolyNull Integer convertPoly(@PolyNull String s) {
        return null;
    }

    Integer convert(String s) {
        return 0;
    }
}
