import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

// Stress test for the Java 8 type-argument-inference incorporation worklist
// (framework/.../typeinference8). The worklist re-scans only the inference variables a newly
// instantiated variable can affect, tracked with reverse-dependency edges built from
// AbstractType.getInferenceVariables(). Its one fragile assumption is that those edges cover every
// way one variable's bounds can mention another. This file concentrates inference shapes where an
// edge could be missed -- F-bounded and recursive type variables, wildcard capture, intersection
// bounds, nested generic arguments, conditional (lub) inference, lambdas/method references, and
// generic varargs -- so that, under the worklist verify/strict mode this project's tests enable
// (-Dcf.typeinference.worklist.strict), a missing edge throws instead of being silently
// self-healed.
//
// The file is about exercising *inference*, not the Nullness type system, so it is suppressed; the
// strict-mode worklist check is independent of @SuppressWarnings (it throws an AssertionError from
// inside inference, not a checker diagnostic).
//
// Complementary tests checker/tests/nullness/InferenceWorkBudget.java and
// checker/tests/inference-budget/InferenceWorkBudget.java cover the work budget that abandons an
// inference whose incorporation -- the work this worklist performs -- grows too expensive.
@SuppressWarnings("nullness") // this test is about inference, not the Nullness type system
public class Java8InferenceWorklistStress {

    // ---- basic chains (the core worklist driver) ----
    static <T> T id(T x) {
        return x;
    }

    void chains(String s) {
        String a = id(id(id(id(id(s)))));
        List<String> b = id(id(Arrays.asList(s, s)));
    }

    // ---- F-bounded / recursive type variables ----
    static <T extends Comparable<T>> T max(List<T> xs) {
        return Collections.max(xs);
    }

    static <B extends Builder<B>> B configure(B b) {
        return b.self();
    }

    interface Builder<B extends Builder<B>> {
        B self();
    }

    <T extends Comparable<T>> void fbounded(List<Integer> ints) {
        Integer m = max(ints);
        Integer m2 = max(id(ints));
    }

    // ---- intersection bounds ----
    static <T extends Comparable<T> & Serializable> T pick(T a, T b) {
        return a;
    }

    void intersections(Integer i, Integer j) {
        Integer r = pick(i, j);
        Comparable<Integer> c = pick(id(i), id(j));
    }

    // ---- wildcard capture (exercises Resolution backtracking / restore()) ----
    static <T> void copy(List<? super T> dst, List<? extends T> src) {}

    static <T> T firstOf(List<? extends T> xs) {
        return xs.get(0);
    }

    void captures(List<? extends Number> nums, List<? super Integer> sink, List<?> anything) {
        Number n = firstOf(nums);
        Number n2 = firstOf(id(nums));
        copy(sink, Arrays.asList(1, 2, 3));
        Object o = firstOf(anything);
    }

    // ---- nested generic arguments ----
    static <K, V> Map<K, V> singleton(K k, V v) {
        Map<K, V> m = new HashMap<>();
        m.put(k, v);
        return m;
    }

    void nested(String k, Integer v) {
        Map<String, List<Integer>> m = singleton(k, Arrays.asList(v, v));
        Map<String, Map<String, List<Integer>>> deep = singleton(k, id(m));
    }

    // ---- conditional (lub) inference ----
    void conditionals(boolean cond, String s) {
        List<String> l = cond ? new ArrayList<>() : new LinkedList<>();
        Comparable<?> c = cond ? Integer.valueOf(1) : "two";
        List<? extends Number> n = cond ? Arrays.asList(1, 2) : Arrays.asList(1.0, 2.0);
    }

    // ---- lambdas and method references ----
    interface Fn<A, R> {
        R apply(A a);
    }

    static <A, R> R applyFn(A a, Fn<A, R> f) {
        return f.apply(a);
    }

    void lambdas(String s) {
        Integer len = applyFn(s, x -> x.length());
        Integer len2 = applyFn(s, String::length);
        List<Integer> lens =
                Stream.of("a", "bb", "ccc").map(String::length).collect(Collectors.toList());
        Map<Integer, List<String>> grouped =
                Stream.of("a", "bb", "cc").collect(Collectors.groupingBy(String::length));
    }

    // ---- generic varargs ----
    @SafeVarargs
    static <T> List<T> listOf(T... xs) {
        return Arrays.asList(xs);
    }

    void varargs(String s, Integer i) {
        List<String> ls = listOf(s, s, s);
        List<Object> lo = listOf(s, i, this);
        List<List<String>> nested = listOf(listOf(s), id(listOf(s)));
    }

    // ---- combinations of the above features ----
    // A missed reverse-dependency edge is most likely where two inference mechanisms interact, so
    // these combine the features above: the inference variables of one feed those of another in one
    // problem.

    // id-chain wrapping a lambda whose inferred result is a nested generic
    void idLambdaNested(String s) {
        Map<String, List<String>> m = id(id(applyFn(s, x -> singleton(x, listOf(x)))));
    }

    // F-bounded inference fed by a captured wildcard list
    <T extends Comparable<T>> T maxOfCapture(List<? extends T> xs) {
        return max(new ArrayList<T>(xs));
    }

    // generic varargs of method references and lambdas, then a stream over the result
    void varargsOfFunctions(String s) {
        List<Fn<String, Integer>> fns = listOf(x -> x.length(), String::length);
        List<Integer> rs = fns.stream().map(f -> f.apply(s)).collect(Collectors.toList());
    }

    // conditional (lub) of two generic-method results, id-wrapped, with nested element types
    void lubOfNestedGenerics(boolean c, String s, Integer v) {
        List<? extends Map<String, Integer>> l =
                id(c ? listOf(singleton(s, v)) : Arrays.asList(singleton(s, v)));
    }

    // wildcard capture flowing into a nested generic argument
    void captureIntoNested(List<? extends Number> nums, String k) {
        Map<String, ? extends List<? extends Number>> m = singleton(k, new ArrayList<>(nums));
    }

    // intersection-bounded method over id-wrapped, capture-bearing arguments
    void intersectionWithIdAndCapture(List<? extends Integer> a, Integer b) {
        Comparable<Integer> r = pick(id(firstOf(a)), id(b));
    }

    // lambda producing a stream collected into a nested map (groupingBy + nested + method ref)
    void lambdaStreamNested(List<String> ss) {
        Map<Integer, List<List<String>>> grouped =
                ss.stream()
                        .collect(
                                Collectors.groupingBy(
                                        String::length,
                                        Collectors.mapping(x -> listOf(x), Collectors.toList())));
    }

    // F-bounded builder chain wrapped through id and a lambda
    <B extends Builder<B>> void builderThroughLambdaAndId(B b) {
        B r = id(applyFn(b, x -> configure(x)));
    }
}
