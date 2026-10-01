// Type argument inference used to throw FalseBoundException on a `? super` wildcard whose
// argument mentions the inferred type variable through `? extends`, and the exception was
// reported as "type.argument.inference.crashed".  javac accepts this code.

public class CaptureSubtypeInference<T> {

    static <K> Object callee(
            CaptureSubtypeInference<? super CaptureSubtypeInference<? extends K>> p) {
        throw new Error();
    }

    static <K> void caller(
            CaptureSubtypeInference<? super CaptureSubtypeInference<? extends K>> p) {
        callee(p);
    }
}
