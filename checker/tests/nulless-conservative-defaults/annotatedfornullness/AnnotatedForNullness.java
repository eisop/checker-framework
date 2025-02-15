import org.checkerframework.checker.initialization.qual.Initialized;
import org.checkerframework.checker.nullness.qual.KeyForBottom;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.framework.qual.AnnotatedFor;

public class AnnotatedForNullness {

    @Initialized @NonNull Object initializedField = new Object();
    @Initialized @KeyForBottom @NonNull Object initializedKeyForBottomField = new Object();

    @AnnotatedFor("initialization")
    // No errors because AnnotatedFor("initialization") does not change the default for nullness.
    Object annotatedForInitialization(Object test) {
        return null;
    }

    @AnnotatedFor("nullness")
    Object annotatedForNullness(Object test) {
        // ::error: (return.type.incompatible)
        return null;
    }

    // Method annotatedFor with both `nullness` and `initialization` should behave the same as
    // annotatedForNullness.
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
        // Expect two [argument.type.incompatible] errors in KeyForChecker and InitilizationChecker
        // because conservative defaults are applied to `unannotatedFor` and it expects a @FBCBottom
        // @KeyForBottom @Nonull Object.
        // ::error: (argument.type.incompatible)
        unannotatedFor(initializedField);
        // Expect an error in KeyForChecker because conservative defaults are applied to
        // `annotatedForInitialization` for hierarchies other than the Initialization Checker and
        // it expects an @Initialized @KeyForBottom @Nonull Object.
        // ::error: (argument.type.incompatible)
        annotatedForInitialization(initializedField);
        // Do not expect an error when conservative defaults are applied to
        // `annotatedForInitialization` for hierarchies other than the Initialization Checker and
        // it expects an @Initialized @KeyForBottom @Nonull Object.
        annotatedForInitialization(initializedKeyForBottomField);
        // Do not expect an error because these are AnnotatedFor("nullness") and these expect
        // @Initialized @UnknownKeyFor @Nonnull Object.
        annotatedForNullness(initializedField);
        annotatedForNullnessAndInitialization(initializedField);
    }

    @AnnotatedFor("initialization")
    void bar() {
        // Expect an error in InitilizationChecker because conservative defaults are applied to
        // `unannotatedFor` and it expects a @FBCBottom @UnknownKeyFor @Nonull Object.
        // ::error: (argument.type.incompatible)
        unannotatedFor(initializedField);
        // Do not expect an error because the warning is suppressed other than initialzation
        // hierarchy when conservative defaults are applied to source code and it expects an
        // @Initialized @KeyForBottom @Nonull Object.
        annotatedForInitialization(initializedField);
        // Do not expect an error when conservative defaults are applied to
        // `annotatedForInitialization` for hierarchies other than the Initialization Checker and
        // it expects an @Initialized @KeyForBottom @Nonull Object.
        annotatedForInitialization(initializedKeyForBottomField);
        // Do not expect an error because these are AnnotatedFor("nullness") and these expect
        // @Initialized @UnknownKeyFor @Nonnull Object.
        annotatedForNullness(initializedField);
        annotatedForNullnessAndInitialization(initializedField);
    }
}
