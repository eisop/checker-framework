/*
 * @test
 * @summary Test that -AnoBytecodeStorage writes only source-code annotations into the .class file,
 *          that AnnotatedFor is still read from bytecode, and that -Amode=jspecify implies
 *          -AnoBytecodeStorage.
 *
 * @compile -XDrawDiagnostics -processor org.checkerframework.checker.nullness.NullnessChecker -Anomsgtext ../noBytecodeStorageLib/Lib.java ../noBytecodeStorageLib/Unannotated.java
 * @compile/fail/ref=WithStorage.out -XDrawDiagnostics -processor org.checkerframework.checker.nullness.NullnessChecker -Anomsgtext -AuseConservativeDefaultsForUncheckedCode=bytecode UseLib.java
 *
 * @compile -XDrawDiagnostics -processor org.checkerframework.checker.nullness.NullnessChecker -Anomsgtext -AnoBytecodeStorage ../noBytecodeStorageLib/Lib.java ../noBytecodeStorageLib/Unannotated.java
 * @compile/fail/ref=NoStorage.out -XDrawDiagnostics -processor org.checkerframework.checker.nullness.NullnessChecker -Anomsgtext -AuseConservativeDefaultsForUncheckedCode=bytecode UseLib.java
 *
 * @compile -XDrawDiagnostics -processor org.checkerframework.checker.nullness.NullnessChecker -Anomsgtext -Amode=jspecify ../noBytecodeStorageLib/Lib.java ../noBytecodeStorageLib/Unannotated.java
 * @compile/fail/ref=NoStorage.out -XDrawDiagnostics -processor org.checkerframework.checker.nullness.NullnessChecker -Anomsgtext -AuseConservativeDefaultsForUncheckedCode=bytecode UseLib.java
 */

import org.checkerframework.checker.nullness.qual.NonNull;

import nobytecodestoragelib.Lib;
import nobytecodestoragelib.Unannotated;

public class UseLib {
    void use() {
        // Without -AnoBytecodeStorage, the defaulted @NonNull is in the bytecode.  With
        // -AnoBytecodeStorage, it is not, but Lib is @AnnotatedFor("nullness"), which is
        // retained in the bytecode, so source defaults rather than conservative defaults apply.
        @NonNull Object a = Lib.nonNull();

        // Unannotated is not @AnnotatedFor("nullness").  Without -AnoBytecodeStorage, the
        // defaulted @NonNull is in the bytecode; with it, conservative defaults apply.
        @NonNull Object b = Unannotated.get();

        // Written in the source, so in the bytecode either way.
        @NonNull Object c = Lib.nullable();
    }
}
