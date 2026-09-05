import viewpointtest.quals.A;
import viewpointtest.quals.B;
import viewpointtest.quals.Bottom;
import viewpointtest.quals.ReceiverDependentQual;
import viewpointtest.quals.Top;

// A class's own type-declaration bound must be a subtype of its supertype's, after the supertype's
// bound is viewpoint-adapted to it.  No clause below is annotated: every supertype qualifier here
// comes from the supertype's declaration bound.
//
// The implicit constructors are not the subject here: each takes the bound of its own class, which
// differs from the bound of the super constructor it invokes.
@SuppressWarnings({"inconsistent.constructor.type", "super.invocation.invalid"})
public class ViewpointAdaptedSuperBounds {
    @ReceiverDependentQual static class DependentClass {}

    @A static class AClass {}

    @B static class BClass {}

    @Top static class TopClass {}

    @ReceiverDependentQual interface DependentInterface {}

    @A interface AInterface {}

    @B interface BInterface {}

    @Top interface TopInterface {}

    // A receiver-dependent bound adapts to the subclass's own bound.
    @A static class AExtendsDependent extends DependentClass {}

    @B static class BExtendsDependent extends DependentClass {}

    @ReceiverDependentQual static class DependentExtendsDependent extends DependentClass {}

    @Bottom static class BottomExtendsDependent extends DependentClass {}

    // Adapting a receiver-dependent bound from @Top yields @Lost, and @Top is not a subtype of it.
    // :: error: (declaration.inconsistent.with.extends.clause)
    @Top static class TopExtendsDependent extends DependentClass {}

    // A fixed bound is unchanged by adaptation, so the ordinary subtype check applies.
    @A static class AExtendsA extends AClass {}

    @Bottom static class BottomExtendsA extends AClass {}

    // :: error: (declaration.inconsistent.with.extends.clause)
    @B static class BExtendsA extends AClass {}

    // :: error: (declaration.inconsistent.with.extends.clause)
    @ReceiverDependentQual static class DependentExtendsA extends AClass {}

    // :: error: (declaration.inconsistent.with.extends.clause)
    @Top static class TopExtendsA extends AClass {}

    @B static class BExtendsB extends BClass {}

    // :: error: (declaration.inconsistent.with.extends.clause)
    @A static class AExtendsB extends BClass {}

    @A static class AExtendsTop extends TopClass {}

    @B static class BExtendsTop extends TopClass {}

    @ReceiverDependentQual static class DependentExtendsTop extends TopClass {}

    @Top static class TopExtendsTop extends TopClass {}

    // Implements clauses are adapted the same way.
    @A static class AImplementsDependent implements DependentInterface {}

    @B static class BImplementsDependent implements DependentInterface {}

    @ReceiverDependentQual static class DependentImplementsDependent implements DependentInterface {}

    @Bottom static class BottomImplementsDependent implements DependentInterface {}

    // :: error: (declaration.inconsistent.with.implements.clause)
    @Top static class TopImplementsDependent implements DependentInterface {}

    @A static class AImplementsA implements AInterface {}

    @Bottom static class BottomImplementsA implements AInterface {}

    // :: error: (declaration.inconsistent.with.implements.clause)
    @B static class BImplementsA implements AInterface {}

    // :: error: (declaration.inconsistent.with.implements.clause)
    @ReceiverDependentQual static class DependentImplementsA implements AInterface {}

    // :: error: (declaration.inconsistent.with.implements.clause)
    @Top static class TopImplementsA implements AInterface {}

    @B static class BImplementsB implements BInterface {}

    // :: error: (declaration.inconsistent.with.implements.clause)
    @A static class AImplementsB implements BInterface {}

    @ReceiverDependentQual static class DependentImplementsTop implements TopInterface {}

    @Top static class TopImplementsTop implements TopInterface {}

    // An interface's extends clause is checked as an implements clause.
    @A interface AInterfaceExtendsDependent extends DependentInterface {}

    @B interface BInterfaceExtendsDependent extends DependentInterface {}

    @ReceiverDependentQual interface DependentInterfaceExtendsDependent extends DependentInterface {}

    @Bottom interface BottomInterfaceExtendsDependent extends DependentInterface {}

    // :: error: (declaration.inconsistent.with.implements.clause)
    @Top interface TopInterfaceExtendsDependent extends DependentInterface {}

    @A interface AInterfaceExtendsA extends AInterface {}

    // :: error: (declaration.inconsistent.with.implements.clause)
    @B interface BInterfaceExtendsA extends AInterface {}

    // Every clause is checked, not just the first.
    @A static class AExtendsAndImplements extends AClass implements DependentInterface, AInterface {}

    @B static class BExtendsAndImplements extends DependentClass implements BInterface, TopInterface {}

    // :: error: (declaration.inconsistent.with.implements.clause)
    @A static class IncompatibleSecondInterface implements DependentInterface, BInterface {}

    @A interface MultipleInterfaceBounds extends DependentInterface, AInterface {}

    // :: error: (declaration.inconsistent.with.implements.clause)
    @A interface IncompatibleInterfaceBound extends DependentInterface, BInterface {}
}
