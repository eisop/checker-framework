import org.checkerframework.checker.initialization.qual.Initialized;
import org.checkerframework.checker.nullness.qual.KeyForBottom;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.framework.qual.AnnotatedFor;

public class AnnotatedForNullness {

    @Initialized @NonNull Object initializedField = new Object();
    @Initialized @KeyForBottom Object initializedKeyForBottomField = new Object();

    @AnnotatedFor("initialization")
    // The method does not report error because AnnotatedFor("initialization") should not change the
    // default for nullness
    Object annotatedForInitialization(Object test) {
        return null;
    }

    @AnnotatedFor("nullness")
    Object annotatedForNullness(Object test) {
        // ::error: (return.type.incompatible)
        return null;
    }

    // Method annotatedFor with both `nullness` and `initialization` should behave the same as
    // annotatedForNullness
    @AnnotatedFor({"nullness", "initialization"})
    Object annotatedForNullnessAndInitialization(Object test) {
        // ::error: (return.type.incompatible)
        return null;
    }

    Object unannotatedFor(Object test) {
        return null;
    }

    @AnnotatedFor("nullness")
    void foo(@Initialized AnnotatedForNullness this) {
        // Issue error because conservative default is applied for unannotatedFor and expects a
        // @FBCBottom @KeyForBottom @Nonull Object
        // ::error: (argument.type.incompatible)
        unannotatedFor(initializedField);
        // Issue error because conservative default is applied other than Initialization checker and
        // expects an @Initialized @KeyForBottom @Nonull Object
        // ::error: (argument.type.incompatible)
        annotatedForInitialization(initializedField);
        // Do not issue error conservative default is applied other than Initialization checker and
        // expects an @Initialized @KeyForBottom @Nonull Object
        annotatedForInitialization(initializedKeyForBottomField);
        // Do not issue error because AnnotatedFor("nullness") and expects an @Initialized
        // @UnknownKeyFor @Nonnull Object
        annotatedForNullness(initializedField);
        annotatedForNullnessAndInitialization(initializedField);
    }
}
