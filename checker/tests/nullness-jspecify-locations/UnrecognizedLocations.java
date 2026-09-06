// Test the locations at which JSpecify gives a nullness annotation no meaning, reported under
// -AjspecifyUnrecognizedLocations.  Locations that an unconditional nullness.on.* error already
// covers are not repeated here.

import org.jspecify.annotations.Nullable;

import java.io.InputStream;
import java.util.List;
import java.util.function.Supplier;

public class UnrecognizedLocations {

    // :: error: (jspecify.unrecognized.location)
    @Nullable class ClassDeclaration {}

    @interface AnnotationMemberRootType {
        // An annotation member cannot return null, so the specification makes any component of
        // this return type unrecognized.
        // :: error: (jspecify.unrecognized.location)
        @Nullable String value();
    }

    @interface AnnotationMemberComponent {
        // "Any component", so the array component is reported even though an array component type
        // is recognized elsewhere.
        // :: error: (jspecify.unrecognized.location)
        @Nullable String[] value();
    }

    // The wildcard itself, as opposed to its bound.  The parameter's own root type is recognized.
    // :: error: (jspecify.unrecognized.location)
    void wildcard(List<@Nullable ?> parameter) {}

    // :: error: (jspecify.unrecognized.location)
    <@Nullable T> void typeParameter() {}

    // :: error: (jspecify.unrecognized.location)
    void thrownType() throws @Nullable Exception {}

    void body(Object o) {
        // :: error: (jspecify.unrecognized.location)
        @Nullable String local = null;

        // :: error: (jspecify.unrecognized.location)
        Object cast = (@Nullable String) o;

        // The Checker Framework reads this location, so it also reports the reference as
        // returning @Nullable where @NonNull is required.
        // :: error: (methodref.return.invalid) :: error: (jspecify.unrecognized.location)
        Supplier<String> methodReference = @Nullable String::new;

        // The array itself, as opposed to its component type.  The specification gives this
        // example: "String @Nullable [] strings = ...".
        // :: error: (jspecify.unrecognized.location)
        String @Nullable [] annotatedArray = null;
    }

    void resourceVariable() throws Exception {
        // A resource variable is not one of the recognized locations either.
        // :: error: (jspecify.unrecognized.location)
        try (@Nullable InputStream in = null) {}
    }
}
