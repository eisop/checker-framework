/*
 * @test
 * @summary Regression test for AnnotationFileParser.findVariableElement(FieldAccessExpr): a
 * declaration annotation whose value is a field access must resolve when the receiver type is
 * reachable only through a wildcard type import.
 * @compile -processor org.checkerframework.checker.nullness.NullnessChecker -Astubs=WildcardImportScope.astub -AstubWarnIfNotFound -Werror WildcardImportScope.java
 */

// Class that WildcardImportScope.astub adds a declaration annotation to, using a value that
// refers to an enum constant reachable only through a wildcard type import (see the .astub file
// for the construct this pins).
public class WildcardImportScope {}
