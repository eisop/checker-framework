/*
 * @test
 * @summary Test that -AstoreInBytecode=false writes only source-code annotations into
 *          the .class file, that AnnotatedFor is still read from bytecode, and that
 *          -Amode=jspecify implies it, which -AstoreInBytecode=true overrides.
 *
 * @compile -XDrawDiagnostics -processor org.checkerframework.checker.nullness.NullnessChecker -Anomsgtext ../storeInBytecodeLib/Lib.java ../storeInBytecodeLib/Unannotated.java
 * @compile/fail/ref=WithStorage.out -XDrawDiagnostics -processor org.checkerframework.checker.nullness.NullnessChecker -Anomsgtext -AuseConservativeDefaultsForUncheckedCode=bytecode UseLib.java
 *
 * @compile -XDrawDiagnostics -processor org.checkerframework.checker.nullness.NullnessChecker -Anomsgtext -AstoreInBytecode=false ../storeInBytecodeLib/Lib.java ../storeInBytecodeLib/Unannotated.java
 * @compile/fail/ref=NoStorage.out -XDrawDiagnostics -processor org.checkerframework.checker.nullness.NullnessChecker -Anomsgtext -AuseConservativeDefaultsForUncheckedCode=bytecode UseLib.java
 *
 * @compile -XDrawDiagnostics -processor org.checkerframework.checker.nullness.NullnessChecker -Anomsgtext -Amode=jspecify ../storeInBytecodeLib/Lib.java ../storeInBytecodeLib/Unannotated.java
 * @compile/fail/ref=NoStorage.out -XDrawDiagnostics -processor org.checkerframework.checker.nullness.NullnessChecker -Anomsgtext -AuseConservativeDefaultsForUncheckedCode=bytecode UseLib.java
 *
 * @compile -XDrawDiagnostics -processor org.checkerframework.checker.nullness.NullnessChecker -Anomsgtext -Amode=jspecify -AstoreInBytecode=true ../storeInBytecodeLib/Lib.java ../storeInBytecodeLib/Unannotated.java
 * @compile/fail/ref=WithStorage.out -XDrawDiagnostics -processor org.checkerframework.checker.nullness.NullnessChecker -Anomsgtext -AuseConservativeDefaultsForUncheckedCode=bytecode UseLib.java
 */

import org.checkerframework.checker.nullness.qual.NonNull;

import storeinbytecodelib.Lib;
import storeinbytecodelib.Unannotated;

public class UseLib {
    void use() {
        // With the default storage, the defaulted @NonNull is in the bytecode.  With
        // -AstoreInBytecode=false it is not, but Lib is @AnnotatedFor("nullness"), which
        // is retained in the bytecode, so source defaults rather than conservative defaults
        // apply.
        @NonNull Object a = Lib.nonNull();

        // Unannotated is not @AnnotatedFor("nullness").  With the default storage the defaulted
        // @NonNull is in the bytecode; with -AstoreInBytecode=false, conservative
        // defaults apply.
        @NonNull Object b = Unannotated.get();

        // Written in the source, so in the bytecode either way.
        @NonNull Object c = Lib.nullable();
    }
}
