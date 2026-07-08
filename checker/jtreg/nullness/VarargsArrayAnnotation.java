/*
 * @test
 * @summary Regression test: BinaryStubWriter previously never read a Parameter's
 *   getVarArgsAnnotations() (the annotation written immediately before the varargs ellipsis,
 *   which applies to the array type itself rather than its element type), silently dropping it
 *   from the binary form of the annotated JDK. This let a null varargs array through checking
 *   incorrectly on the affected JDK methods (Class.getMethod's array-level nullness annotation
 *   was affected) and, conversely, could make an unrelated method's varargs array look nullable
 *   when it is not (String.format's array is not nullable, only its elements are). Caught by a
 *   downstream Daikon CI failure on branch binary-stubs-v2.
 * @compile/fail/ref=VarargsArrayAnnotation.out -XDrawDiagnostics -processor org.checkerframework.checker.nullness.NullnessChecker -Anomsgtext VarargsArrayAnnotation.java
 */

class VarargsArrayAnnotation {

    // Class.getMethod's varargs array itself is @Nullable in the annotated JDK
    // ("Class<?>@Nullable ... parameterTypes"): passing a null array must be allowed.
    void positive(Class<?> cls) throws Exception {
        cls.getMethod("m", (Class<?>[]) null);
    }

    // String.format's varargs array itself is not @Nullable (only its elements are): passing a
    // null array must still be rejected.
    void negative() {
        String.format("%s", (Object[]) null);
    }
}
