import org.checkerframework.checker.initialization.qual.Initialized;
import org.checkerframework.checker.nullness.qual.KeyForBottom;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.framework.qual.AnnotatedFor;

public class AnnotatedForNullness {

    @Initialized @NonNull Object initializedField = new Object();
    @Initialized @NonNull @KeyForBottom Object initializedKeyForBottomField = new Object();

    @AnnotatedFor("initialization")
    // The method does not report error because AnnotatedFor("initialization") does not change the
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
        // ::error: (argument.type.incompatible)
        unannotatedFor(initializedField);
        // Error because keyFor checker requires @KeyForBottom type
        // ::error: (argument.type.incompatible)
        annotatedForInitialization(initializedField);
        annotatedForInitialization(initializedKeyForBottomField);
        annotatedForNullness(initializedField);
        annotatedForNullnessAndInitialization(initializedField);
    }
}
