import org.checkerframework.checker.immutability.qual.Immutable;

@Immutable
public class DeepRDMDefaultForInstanceField {
    // Should have error. Array component should also be defaulted
    // to @ReceiverDependantMutable.
    // Update: maybe don't need deep default. It's not possible to
    // let every field to be in abstract state just by defaulting.
    // :: error: (assignment.type.incompatible)
    Object[] o = new String @Immutable [] {""};
}
