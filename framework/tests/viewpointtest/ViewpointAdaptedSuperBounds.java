import viewpointtest.quals.A;
import viewpointtest.quals.B;
import viewpointtest.quals.Bottom;
import viewpointtest.quals.ReceiverDependentQual;
import viewpointtest.quals.Top;

// All supertype qualifiers come from declaration bounds, not annotations on the clauses.
@SuppressWarnings({
    "inconsistent.constructor.type",
    "super.invocation.invalid",
    "cast.unsafe.constructor.invocation"
})
public class ViewpointAdaptedSuperBounds {
    @ReceiverDependentQual static class DependentClass {}

    @A static class AClass {}

    @B static class BClass {}

    @Top static class TopClass {}

    @ReceiverDependentQual interface DependentInterface {}

    @A interface AInterface {}

    @B interface BInterface {}

    @Top interface TopInterface {}

    // A receiver-dependent bound adapts to the subclass's bound.
    @A static class AExtendsDependent extends DependentClass {}

    @B static class BExtendsDependent extends DependentClass {}

    @ReceiverDependentQual static class DependentExtendsDependent extends DependentClass {}

    @Bottom static class BottomExtendsDependent extends DependentClass {}

    // @Top adapts the receiver-dependent bound to @Lost, and @Top is not a subtype of @Lost.
    // :: error: (declaration.inconsistent.with.extends.clause)
    @Top static class TopExtendsDependent extends DependentClass {}

    // Fixed bounds do not change under viewpoint adaptation.
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

    // The same adaptation applies to implements clauses.
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

    // Interface inheritance uses extends clauses as well.
    @A interface AInterfaceExtendsDependent extends DependentInterface {}

    @B interface BInterfaceExtendsDependent extends DependentInterface {}

    @ReceiverDependentQual interface DependentInterfaceExtendsDependent extends DependentInterface {}

    @Bottom interface BottomInterfaceExtendsDependent extends DependentInterface {}

    // :: error: (declaration.inconsistent.with.implements.clause)
    @Top interface TopInterfaceExtendsDependent extends DependentInterface {}

    @A interface AInterfaceExtendsA extends AInterface {}

    // :: error: (declaration.inconsistent.with.implements.clause)
    @B interface BInterfaceExtendsA extends AInterface {}

    // Check every bound when multiple supertypes are present.
    @A static class AExtendsAndImplements extends AClass implements DependentInterface, AInterface {}

    @B static class BExtendsAndImplements extends DependentClass implements BInterface, TopInterface {}

    // :: error: (declaration.inconsistent.with.implements.clause)
    @A static class IncompatibleSecondInterface implements DependentInterface, BInterface {}

    @A interface MultipleInterfaceBounds extends DependentInterface, AInterface {}

    // :: error: (declaration.inconsistent.with.implements.clause)
    @A interface IncompatibleInterfaceBound extends DependentInterface, BInterface {}
}
