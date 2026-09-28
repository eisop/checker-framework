// Placeholder source file for NullnessBinaryStubDiffTest.
//
// The real work of the test happens at checker initialization: the -AbinaryStubDiffCheck option
// makes AnnotationFileElementTypes run BinaryStubDiffChecker, which loads every class of the
// binary JDK stub (annotated-jdk.bin.gz) and of every built-in checker stub file (*.astub.bin.gz)
// through both the binary reader and the text parser and reports any disagreement as an error.
// This file only exists so that the per-directory test harness has a compilation to run; it
// expects no diagnostics.

public class BinaryStubDiffPlaceholder {
    /** An arbitrary method so the file is not empty. */
    void method() {}
}
