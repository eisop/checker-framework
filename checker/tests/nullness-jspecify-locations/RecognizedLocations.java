// Locations JSpecify recognizes, including ones nested inside an unrecognized location.  None of
// these is reported, even under -AjspecifyUnrecognizedLocations.

import org.jspecify.annotations.Nullable;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;

public class RecognizedLocations {

    /** Qualifier for a method reference whose qualifier is a field select. */
    String fieldForMethodRef = "";

    // A field's root type.
    @Nullable String field;

    // A type parameter's bound, a return type, and a formal parameter.
    <T extends @Nullable Object> @Nullable String recognized(@Nullable String parameter) {
        // A type argument nested inside a local variable's root type.
        List<@Nullable String> local = null;

        // A type argument nested inside a cast's root type.
        @SuppressWarnings("unchecked")
        Object cast = (List<@Nullable String>) local;

        // An unannotated intersection-type cast. TreeUtils.getExplicitAnnotationTrees once threw
        // BugInCF on any INTERSECTION_TYPE tree, so this crashed even with no annotation present.
        Object intersection = (Supplier<String> & Serializable) () -> "";

        // A type argument of an object creation.
        Object created = new ArrayList<@Nullable String>();

        // An array's component type, even as a local variable's type.
        @Nullable String[] componentAnnotated = null;

        // A wildcard's bound, as opposed to the wildcard itself.
        List<? extends @Nullable String> bound = null;

        // A formal parameter type of a lambda.
        Function<@Nullable String, String> lambda = (@Nullable String s) -> "";

        // Method references whose qualifier is an expression rather than a type. These once
        // crashed TreeUtils.getExplicitAnnotationTrees, which accepts only type trees.  An
        // annotation on a type qualifier is still reported; see UnrecognizedLocations's
        // "@Nullable String::new".
        Supplier<Integer> literalQualifier = "abc"::length;
        Supplier<String> newQualifier = new Object()::toString;
        Supplier<String> invocationQualifier = "abc".trim()::toString;
        Supplier<Integer> parenthesizedQualifier = ("abc")::length;
        String nonNullLocal = "";
        Supplier<Integer> variableQualifier = nonNullLocal::length;
        // A member select, the one kind whose classification depends on what it resolves to
        // rather than on its kind: this one is a field, so it is an expression.  A member select
        // that resolves to a type, such as "java.lang.String::valueOf", is a type qualifier and
        // is checked.
        Supplier<Integer> fieldSelectQualifier = fieldForMethodRef::length;

        return null;
    }

    // An unannotated receiver parameter with a type argument: not reported just because a
    // receiver parameter is present.
    static class GenericReceiver<T> {
        void method(GenericReceiver<T> this) {}
    }

    // An unannotated instanceof, and (in RecognizedLocationsInstanceOf.java, since pattern
    // matching for instanceof requires Java 14+) an unannotated instanceof pattern: neither is
    // reported just because an instanceof (pattern or not) is present.
    void instanceOf(Object o) {
        if (o instanceof String[]) {}
    }
}
