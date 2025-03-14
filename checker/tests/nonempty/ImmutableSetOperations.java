// @below-java9-jdk-skip-test

import org.checkerframework.checker.nonempty.qual.NonEmpty;

import java.util.Set;

class ImmutableSetOperations {

    void testCreateEmptyImmutableSet() {
        Set<Integer> emptyInts = Set.of();
        // Creating a copy of an empty set should also yield an empty set
        // :: error: (assignment.type.incompatible)
        @NonEmpty Set<Integer> copyOfEmptyInts = Set.copyOf(emptyInts);
    }

    void testCreateNonEmptyImmutableSet() {
        Set<Integer> nonEmptyInts = Set.of(1, 2, 3);
        // Creating a copy of a non-empty set should also yield a non-empty set
        @NonEmpty Set<Integer> copyOfNonEmptyInts = Set.copyOf(nonEmptyInts);
    }
}
