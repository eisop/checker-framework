// Regression test for the inference work budget catching a bound set of many mutually F-bounded
// type parameters, resolved together by one wildcarded generic invocation with no explicit type
// witness (e.g. `MapMakerInternalMap<K, V, E extends InternalEntry<K, V, E>, S extends
// Segment<K, V, E, S>>`'s own factory method, modeled here with a chain of I1..I10, each Ii's
// bound mentioning T1..Ti). Before recordIncorporationWork was also charged in
// Resolution#resolveSmallestSet (JLS 18.4 resolution),
// VariableBounds#doApplyInstantiationsToBounds's
// already-resolved fast path (which still applies constraints), and
// InferenceType#applyInstantiations (substitution back into a type's structure), this shape's cost
// was invisible to the budget: it ran for many seconds (worse for more mutually dependent type
// parameters) rather than being abandoned. This directory runs with a small
// -AinferenceWorkBudget=2000 (see InferenceBudgetTest), so a chain of only 10 mutually F-bounded
// type parameters already exceeds it, instead of needing the ~16+ that reach the default budget.
//
// See fbound in .claude/skills/cf-performance/gen-shapes.py for the generator that found this
// (sweep D, the chain length, to reproduce the super-linear cost this budget now bounds).
public class InferenceWorkBudgetFBound {

    interface I1<T1 extends I1<T1>> {}

    interface I2<T1 extends I1<T1>, T2 extends I2<T1, T2>> {}

    interface I3<T1 extends I1<T1>, T2 extends I2<T1, T2>, T3 extends I3<T1, T2, T3>> {}

    interface I4<
            T1 extends I1<T1>,
            T2 extends I2<T1, T2>,
            T3 extends I3<T1, T2, T3>,
            T4 extends I4<T1, T2, T3, T4>> {}

    interface I5<
            T1 extends I1<T1>,
            T2 extends I2<T1, T2>,
            T3 extends I3<T1, T2, T3>,
            T4 extends I4<T1, T2, T3, T4>,
            T5 extends I5<T1, T2, T3, T4, T5>> {}

    interface I6<
            T1 extends I1<T1>,
            T2 extends I2<T1, T2>,
            T3 extends I3<T1, T2, T3>,
            T4 extends I4<T1, T2, T3, T4>,
            T5 extends I5<T1, T2, T3, T4, T5>,
            T6 extends I6<T1, T2, T3, T4, T5, T6>> {}

    interface I7<
            T1 extends I1<T1>,
            T2 extends I2<T1, T2>,
            T3 extends I3<T1, T2, T3>,
            T4 extends I4<T1, T2, T3, T4>,
            T5 extends I5<T1, T2, T3, T4, T5>,
            T6 extends I6<T1, T2, T3, T4, T5, T6>,
            T7 extends I7<T1, T2, T3, T4, T5, T6, T7>> {}

    interface I8<
            T1 extends I1<T1>,
            T2 extends I2<T1, T2>,
            T3 extends I3<T1, T2, T3>,
            T4 extends I4<T1, T2, T3, T4>,
            T5 extends I5<T1, T2, T3, T4, T5>,
            T6 extends I6<T1, T2, T3, T4, T5, T6>,
            T7 extends I7<T1, T2, T3, T4, T5, T6, T7>,
            T8 extends I8<T1, T2, T3, T4, T5, T6, T7, T8>> {}

    interface I9<
            T1 extends I1<T1>,
            T2 extends I2<T1, T2>,
            T3 extends I3<T1, T2, T3>,
            T4 extends I4<T1, T2, T3, T4>,
            T5 extends I5<T1, T2, T3, T4, T5>,
            T6 extends I6<T1, T2, T3, T4, T5, T6>,
            T7 extends I7<T1, T2, T3, T4, T5, T6, T7>,
            T8 extends I8<T1, T2, T3, T4, T5, T6, T7, T8>,
            T9 extends I9<T1, T2, T3, T4, T5, T6, T7, T8, T9>> {}

    interface I10<
            T1 extends I1<T1>,
            T2 extends I2<T1, T2>,
            T3 extends I3<T1, T2, T3>,
            T4 extends I4<T1, T2, T3, T4>,
            T5 extends I5<T1, T2, T3, T4, T5>,
            T6 extends I6<T1, T2, T3, T4, T5, T6>,
            T7 extends I7<T1, T2, T3, T4, T5, T6, T7>,
            T8 extends I8<T1, T2, T3, T4, T5, T6, T7, T8>,
            T9 extends I9<T1, T2, T3, T4, T5, T6, T7, T8, T9>,
            T10 extends I10<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10>> {}

    static class Factory {
        static <
                        T1 extends I1<T1>,
                        T2 extends I2<T1, T2>,
                        T3 extends I3<T1, T2, T3>,
                        T4 extends I4<T1, T2, T3, T4>,
                        T5 extends I5<T1, T2, T3, T4, T5>,
                        T6 extends I6<T1, T2, T3, T4, T5, T6>,
                        T7 extends I7<T1, T2, T3, T4, T5, T6, T7>,
                        T8 extends I8<T1, T2, T3, T4, T5, T6, T7, T8>,
                        T9 extends I9<T1, T2, T3, T4, T5, T6, T7, T8, T9>,
                        T10 extends I10<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10>>
                I10<?, ?, ?, ?, ?, ?, ?, ?, ?, ?> create() {
            return null;
        }
    }

    void tooManyMutuallyFBoundedTypeParameters() {
        // :: error: (type.argument.inference.budget)
        var s = Factory.create();
    }
}
