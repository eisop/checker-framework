// Test case for https://tinyurl.com/cfissue/3622

// @skip-test until the issue is fixed

import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.List;

public class Issue3622 {

    public class ImmutableIntList1 {

        @Override
        public boolean equals(@Nullable Object obj) {
            if (obj instanceof ImmutableIntList1) {
                return true;
            } else {
                return obj instanceof List;
            }
        }
    }

    public class ImmutableIntList2 {

        @Override
        public boolean equals(@Nullable Object obj) {
            return obj instanceof ImmutableIntList2;
        }
    }

    public class ImmutableIntList3 {

        @Override
        public boolean equals(@Nullable Object obj) {
            if (obj instanceof ImmutableIntList3) {
                return true;
            } else {
                return false;
            }
        }
    }

    public class ImmutableIntList4 {

        @Override
        public boolean equals(@Nullable Object obj) {
            return obj instanceof ImmutableIntList4 ? true : obj instanceof List;
        }
    }

    public class ImmutableIntList5 {

        @Override
        public boolean equals(@Nullable Object obj) {
            return obj instanceof ImmutableIntList5
                    ? obj instanceof ImmutableIntList5
                    : obj instanceof ImmutableIntList5;
        }
    }

    public class ImmutableIntList6 {

        @Override
        public boolean equals(@Nullable Object obj) {
            return true ? obj instanceof ImmutableIntList6 : obj instanceof ImmutableIntList6;
        }
    }

    public class ImmutableIntList7 {
        @Override
        public boolean equals(@Nullable Object obj) {
            // :: error:  (contracts.conditional.postcondition.not.satisfied)
            return (obj instanceof ImmutableIntList7) ? true : !(obj instanceof List);
        }
    }


    public class ImmutableIntList8 {

        // The false positive is because the ternary expression has condition of literal `true`
        // TODO: prune the dead branch like https://github.com/typetools/checker-framework/pull/3389
        @Override
        @SuppressWarnings("contracts.conditional.postcondition.not.satisfied")
        public boolean equals(@Nullable Object obj) {
            return true ? obj instanceof ImmutableIntList8 : false;
        }
    }

    public class ImmutableIntList9 {

        // The false positive is because the false expression of the tenary expression is literal
        // `false`. In this case only the else-store before should be propagated to the else-store
        // after.
        // TODO: adapt the way of store propagation for boolean variables. i.e. only then-store is
        // propagated for `true` and only else-store is propagated for `false`.
        @Override
        @SuppressWarnings("contracts.conditional.postcondition.not.satisfied")
        public boolean equals(@Nullable Object obj) {
            return obj instanceof ImmutableIntList9 ? true : false;
        }
    }

    public class ImmutableIntList10 {

        // The false positive is because Nullness Checker does not store the boolean value in the
        // Nullness analysis, therefore the relation between boolean variable `b` and `obj` is not
        // known
        @Override
        @SuppressWarnings("contracts.conditional.postcondition.not.satisfied")
        public boolean equals(@Nullable Object obj) {
            boolean b;
            if (obj instanceof ImmutableIntList10) {
                b = true;
            } else {
                b = false;
            }
            return b;
        }
    }
}
