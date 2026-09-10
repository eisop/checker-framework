/*
 * @test
 * @summary Regression test for AnnotationFileParser.processTypeDecl's member-processing switch: a
 * nested annotation type declaration must be processed like a nested class, interface, or enum,
 * not silently ignored.
 * @compile -processor org.checkerframework.checker.nullness.NullnessChecker -Astubs=Outer.astub -AmergeStubsWithSource -AstubWarnIfNotFound -Werror Outer.java
 */

// Outer.astub gives the parameter of Outer.Nested.Helper.m a @Nullable type. Applying that
// requires AnnotationFileParser to process the nested annotation type declaration Outer.Nested and
// then its member class Helper -- the construct this test pins. See Outer.astub.
public class Outer {
    public @interface Nested {
        class Helper {
            void m(Object p) {}
        }
    }

    void use(Outer.Nested.Helper h) {
        // With the nested annotation type processed, m's parameter is @Nullable, so passing null
        // is allowed. Without that processing the parameter stays @NonNull and this is an
        // argument.type.incompatible error.
        h.m(null);
    }
}
