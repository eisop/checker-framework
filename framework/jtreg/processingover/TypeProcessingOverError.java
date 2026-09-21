/*
 * @test
 *
 * @summary Test that a UserError thrown by an overridden typeProcessingOver is reported as a
 * compiler error.  AbstractTypeProcessor wraps the call rather than SourceChecker wrapping its own
 * typeProcessingOver, because an override's own work runs before it calls super.
 *
 * @compile -XDrawDiagnostics -processor org.checkerframework.framework.testchecker.processingover.ThrowingTypeProcessingOverChecker TypeProcessingOverError.java
 * @compile/fail/ref=TypeProcessingOverError.out -XDrawDiagnostics -processor org.checkerframework.framework.testchecker.processingover.ThrowingTypeProcessingOverChecker -AthrowInTypeProcessingOver TypeProcessingOverError.java
 */

public class TypeProcessingOverError {}
