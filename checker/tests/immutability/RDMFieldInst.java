import org.checkerframework.checker.immutability.qual.*;

public class RDMFieldInst {
    @Mutable
    private static class MutableBox {}

    @Immutable
    private static class ImmutableBox {}

    @ReceiverDependantMutable
    private static class RDMBox {}

    @Immutable
    private static class ImmutableClass {
        // :: error: (type.invalid.annotations.on.use)
        @ReceiverDependantMutable MutableBox mutableBoxInRDM;
    }

    @Mutable
    private static class MutableClass {
        @ReceiverDependantMutable MutableBox mutableBoxInRDM = new MutableBox();

        @ReceiverDependantMutable RDMBox rdmBoxInRDMnewM = new @Mutable RDMBox();
        // :: error: (assignment.type.incompatible)
        @ReceiverDependantMutable RDMBox rdmBoxInRDMnewI = new @Immutable RDMBox();
        // :: error: (assignment.type.incompatible)
        @ReceiverDependantMutable RDMBox rdmBoxInRDMnewRDM = new @ReceiverDependantMutable RDMBox();
        // :: error: (type.invalid.annotations.on.use)
        @ReceiverDependantMutable ImmutableBox immutableBoxInRDM = new ImmutableBox();
    }
}
