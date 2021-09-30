// Test case for https://tinyurl.com/cfissue/3622

// @skip-test until the issue is fixed

import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.List;

public class Issue3622 {

    // These currently pass (no warnings)

    public class ImmutableIntList1 {

        @Override
        public boolean equals(@Nullable Object obj1) {
            if (obj1 instanceof ImmutableIntList1) {
                return true;
            } else {
                return obj1 instanceof List;
            }
        }
    }

    public class ImmutableIntList2 {

        @Override
        public boolean equals(@Nullable Object obj2) {
            return obj2 instanceof ImmutableIntList2;
        }
    }

    public class ImmutableIntList3 {

        @Override
        public boolean equals(@Nullable Object obj3) {
            if (obj3 instanceof ImmutableIntList3) {
                return true;
            } else {
                return false;
            }
        }
    }

    public class ImmutableIntList8 {

        @Override
        public boolean equals(@Nullable Object obj8) {
            return obj8 instanceof ImmutableIntList8 ? true : obj8 instanceof List;
        }
    }

    public class ImmutableIntList9 {

        @Override
        public boolean equals(@Nullable Object obj9) {
            return obj9 instanceof ImmutableIntList9
                    ? obj9 instanceof ImmutableIntList9
                    : obj9 instanceof ImmutableIntList9;
        }
    }

    // These currently fail (false positive warnings)

    public class ImmutableIntList4 {

        @Override
        @SuppressWarnings("contracts.conditional.postcondition.not.satisfied")
        public boolean equals(@Nullable Object obj4) {
            boolean b;
            if (obj4 instanceof ImmutableIntList4) {
                b = true;
            } else {
                b = false;
            }
            return b;
        }
    }

    public class ImmutableIntList5 {

        @Override
        @SuppressWarnings(
                "contracts.conditional.postcondition.not.satisfied" // TODO: Need special treatment
        // for true and false boolean  literals (cut off dead parts of graph).
        )
        public boolean equals(@Nullable Object obj5) {
            return true ? obj5 instanceof ImmutableIntList5 : obj5 instanceof ImmutableIntList5;
        }
    }

    public class ImmutableIntList6 {

        @Override
        @SuppressWarnings(
                "contracts.conditional.postcondition.not.satisfied" // TODO: Need special treatment
        // for true and false boolean  literals (cut off dead parts of graph).
        )
        public boolean equals(@Nullable Object obj6) {
            return true ? obj6 instanceof ImmutableIntList6 : false;
        }
    }

    public class ImmutableIntList7 {

        @Override
        @SuppressWarnings("contracts.conditional.postcondition.not.satisfied")
        public boolean equals(@Nullable Object obj7) {
            return obj7 instanceof ImmutableIntList7 ? true : false;
        }
    }

    // The false-negative case introduced by 'BOTH_TO_THEN', 'BOTH_TO_ELSE' flow rules
    public class ImmutableIntList10 {
        @Override
        public boolean equals(@Nullable Object obj10) {
            // :: error:  (contracts.conditional.postcondition.not.satisfied)
            return (obj10 instanceof ImmutableIntList10) ? true : !(obj10 instanceof List);
        }
    }
}
