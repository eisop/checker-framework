/*
 * @test
 * @summary Regression test for AnnotationFileParser.findVariableElement(FieldAccessExpr): a
 * declaration annotation's field-access value must resolve even when its scope is itself a field
 * access, not just when the scope is a plain name.
 * @compile DefinedByExample.java
 * @compile -processor org.checkerframework.checker.nullness.NullnessChecker -Astubs=UsesDefinedBy.astub -AmergeStubsWithSource -AstubWarnIfNotFound -Werror UsesDefinedBy.java
 */

package nestedscope;

// UsesDefinedBy.astub adds @DefinedByExample(DefinedByExample.Api.COMPILER) to bar(), using a
// scope ("DefinedByExample.Api") that is itself a field access, not a simple name -- see that
// file for the construct this pins.
public class UsesDefinedBy {
    void bar() {}
}
