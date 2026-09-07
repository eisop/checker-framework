import org.checkerframework.checker.mutability.qual.Immutable;
import org.checkerframework.checker.mutability.qual.Mutable;
import org.checkerframework.checker.mutability.qual.Readonly;
import org.checkerframework.checker.mutability.qual.ReceiverDependentMutable;

public class FieldDefaulting {

    @ReceiverDependentMutable static class RdmClass {}

    @ReceiverDependentMutable class Fields<T extends @ReceiverDependentMutable Object> {
        RdmClass declaredField = null;
        T typeVariableField = null;

        @Readonly RdmClass explicitReadonlyField = null;
    }

    @ReceiverDependentMutable static class Statics {
        static RdmClass staticField;
    }

    void declaredFields(
            @Mutable Fields<@Mutable Object> mutable,
            @Immutable Fields<@Immutable Object> immutable) {
        @Mutable RdmClass mutableField = mutable.declaredField;
        @Immutable RdmClass immutableField = immutable.declaredField;

        // :: error: (assignment.type.incompatible)
        @Immutable RdmClass notImmutable = mutable.declaredField;
        // :: error: (assignment.type.incompatible)
        @Mutable RdmClass notMutable = immutable.declaredField;

        @Readonly RdmClass readonlyField = immutable.explicitReadonlyField;
        @Mutable RdmClass staticField = Statics.staticField;
    }

    void typeVariableFields(
            @Mutable Fields<@Mutable Object> mutable,
            @Immutable Fields<@Immutable Object> immutable) {
        @Mutable Object mutableField = mutable.typeVariableField;
        @Immutable Object immutableField = immutable.typeVariableField;

        // :: error: (assignment.type.incompatible)
        @Immutable Object notImmutable = mutable.typeVariableField;
        // :: error: (assignment.type.incompatible)
        @Mutable Object notMutable = immutable.typeVariableField;
    }
}
