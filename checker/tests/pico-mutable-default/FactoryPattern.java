import org.checkerframework.checker.mutability.qual.Immutable;
import org.checkerframework.checker.mutability.qual.Mutable;
import org.checkerframework.checker.mutability.qual.PolyMutable;
import org.checkerframework.checker.mutability.qual.Readonly;
import org.checkerframework.checker.mutability.qual.ReceiverDependentMutable;

public @ReceiverDependentMutable class FactoryPattern {
    public @ReceiverDependentMutable FactoryPattern() {}

    @PolyMutable Object createObject(@PolyMutable FactoryPattern this) {
        return new @PolyMutable Object();
    }

    // Should issue error because the context can not resolved based on assignment
    @PolyMutable Object createObjectA(@Readonly FactoryPattern this) {
        return new @PolyMutable Object();
    }

    static void test() {
        FactoryPattern immutableFactory = new @Immutable FactoryPattern();
        FactoryPattern mutableFactory = new @Mutable FactoryPattern();

        // :: error: (assignment.type.incompatible)
        @Mutable Object mo = immutableFactory.createObject();
        @Immutable Object imo = immutableFactory.createObject();
        @Mutable Object mo2 = mutableFactory.createObject();
        // :: error: (assignment.type.incompatible)
        @Immutable Object imo2 = mutableFactory.createObject();
    }
}
