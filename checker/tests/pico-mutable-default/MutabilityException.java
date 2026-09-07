import org.checkerframework.checker.mutability.qual.Immutable;
import org.checkerframework.checker.mutability.qual.Mutable;
import org.checkerframework.checker.mutability.qual.Readonly;
import org.checkerframework.checker.mutability.qual.ReceiverDependentMutable;

public class MutabilityException {

    void defaultCatchParameterIsMutable() {
        try {
            throw new RuntimeException();
        } catch (RuntimeException e) {
            @Mutable RuntimeException mutable = e;
            @Readonly RuntimeException readonly = e;
        }
    }

    void everyCatchParameterQualifierIsPermitted() {
        try {
            throw new RuntimeException();
        } catch (@Mutable RuntimeException e) {
        }

        try {
            throw new RuntimeException();
        } catch (@Readonly RuntimeException e) {
        }

        try {
            throw new RuntimeException();
        } catch (@Immutable RuntimeException e) {
        }

        try {
            throw new RuntimeException();
        } catch (@ReceiverDependentMutable RuntimeException e) {
        }
    }

    void throwMutable(@Mutable RuntimeException e) {
        throw e;
    }

    void throwReadonly(@Readonly RuntimeException e) {
        throw e;
    }

    void throwImmutable(@Immutable RuntimeException e) {
        throw e;
    }

    void throwReceiverDependent(@ReceiverDependentMutable RuntimeException e) {
        throw e;
    }
}
