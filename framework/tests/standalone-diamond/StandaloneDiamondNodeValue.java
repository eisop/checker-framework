import org.checkerframework.framework.testchecker.util.Encrypted;
import org.checkerframework.framework.testchecker.util.PolyEncrypted;

import java.util.ArrayList;
import java.util.List;

// The dataflow value of a diamond nested in an enclosing type argument inference, such as the
// argument of takes(new Box<>(e)), is computed without that inference when the diamond does not
// create an anonymous class and has no poly expression argument
// (GenericAnnotatedTypeFactory#getStandaloneTypeForNodeValue).  Its primary qualifier must be the
// one it has in the enclosing inference, and its underlying type must be javac's; the test checker
// reports a dataflow value of a diamond that differs from the diamond's type
// (nodevalue.qualifier, nodevalue.underlying.type).
public class StandaloneDiamondNodeValue {

    static class Box<T> {
        // The constructor of Object, which this one invokes, is not polymorphic.
        @SuppressWarnings({"inconsistent.constructor.type", "super.invocation.invalid"})
        @PolyEncrypted Box(@PolyEncrypted T t) {}
    }

    static class ObjectBox<T> {
        @SuppressWarnings({"inconsistent.constructor.type", "super.invocation.invalid"})
        @PolyEncrypted ObjectBox(@PolyEncrypted Object o, T t) {}
    }

    static <U> U make() {
        throw new Error();
    }

    static <U> void takes(@Encrypted Box<U> b) {}

    static <U> void takesObjectBox(@Encrypted ObjectBox<U> b) {}

    static <U extends @Encrypted Object> void takesEncryptedTypeArgument(@Encrypted Box<U> b) {}

    static @Encrypted String encrypt(String s) {
        throw new Error();
    }

    static <U> List<U> wrap(List<U> l) {
        return l;
    }

    // Standalone: the polymorphic qualifier comes from an argument of a type variable's type.
    void polymorphicTypeVariableParameter(@Encrypted String e, String s) {
        takes(new Box<>(e));
        // :: error: (argument.type.incompatible)
        takes(new Box<>(s));
    }

    // Standalone: the polymorphic qualifier comes from an argument of a type that is not a type
    // variable.
    void polymorphicParameter(@Encrypted Object e, Object o) {
        takesObjectBox(new ObjectBox<>(e, "x"));
        // :: error: (argument.type.incompatible)
        takesObjectBox(new ObjectBox<>(o, "x"));
    }

    // Not standalone: make() is a poly expression, so inferring the Box on its own would also infer
    // make() without the enclosing inference, which fixes its type argument to an @Encrypted type.
    // The Box is @Encrypted.
    void polyExpressionArgument() {
        takesEncryptedTypeArgument(new Box<>(make()));
    }

    // Standalone: encrypt(make()) is not a poly expression, so the target type of make() is the
    // parameter type of encrypt, whichever way the Box is inferred.
    void polyExpressionInNonPolyArgument() {
        takesEncryptedTypeArgument(new Box<>(encrypt(make())));
    }

    // Standalone: inferred without the enclosing inference, the diamond is an ArrayList<Object>;
    // its dataflow value has javac's type, ArrayList<Number>.
    void underlyingType() {
        List<Number> l = wrap(new ArrayList<>());
    }
}
