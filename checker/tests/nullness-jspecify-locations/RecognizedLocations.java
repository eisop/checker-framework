// Locations JSpecify recognizes, including ones nested inside an unrecognized location.  None of
// these is reported, even under -AjspecifyUnrecognizedLocations.

import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

public class RecognizedLocations {

    // A field's root type.
    @Nullable String field;

    // A type parameter's bound, a return type, and a formal parameter.
    <T extends @Nullable Object> @Nullable String recognized(@Nullable String parameter) {
        // A type argument nested inside a local variable's root type.
        List<@Nullable String> local = null;

        // A type argument nested inside a cast's root type.
        @SuppressWarnings("unchecked")
        Object cast = (List<@Nullable String>) local;

        // A type argument of an object creation.
        Object created = new ArrayList<@Nullable String>();

        // An array's component type, even as a local variable's type.
        @Nullable String[] componentAnnotated = null;

        // A wildcard's bound, as opposed to the wildcard itself.
        List<? extends @Nullable String> bound = null;

        // A formal parameter type of a lambda.
        Function<@Nullable String, String> lambda = (@Nullable String s) -> "";

        return null;
    }

    // An unannotated receiver parameter with a type argument, and an unannotated instanceof of an
    // array type: neither is reported just because a receiver parameter or an instanceof pattern
    // is present.
    static class GenericReceiver<T> {
        void method(GenericReceiver<T> this) {}
    }

    void instanceOf(Object o) {
        if (o instanceof String[]) {}
        if (o instanceof String[] unannotated) {}
    }
}
