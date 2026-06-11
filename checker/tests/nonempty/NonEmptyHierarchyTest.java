import org.checkerframework.checker.nonempty.qual.NonEmpty;
import org.checkerframework.checker.nonempty.qual.UnknownNonEmpty;

import java.util.List;

class NonEmptyHierarchyTest {

    void testAssignments(@NonEmpty List<String> l1, @UnknownNonEmpty List<String> l2) {
        @NonEmpty List<String> l3 = l1; // OK, both are @NonEmpty

        // :: error: (assignment.type.incompatible)
        @NonEmpty List<String> l4 = l2;

        List<String> l5 =
                l1; // l5 implicitly has type @UnknownNonEmpty, assigning l1 to it is legal
    }
}
