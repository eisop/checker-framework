import org.checkerframework.checker.pico.qual.Immutable;
import org.checkerframework.checker.pico.qual.Mutable;
import org.checkerframework.checker.pico.qual.PolyMutable;
import org.checkerframework.checker.pico.qual.Readonly;
import org.checkerframework.checker.pico.qual.ReceiverDependentMutable;

/**
 * This test case aims to showing the validity of annotation class, type use, inheritance and object
 * creation.
 */
/* @Immutable */
public class ClassAnnotation {
    /* @Immutable */ interface ImmutableInterfaceImplict {}

    @Immutable interface ImmutableInterfaceExplict {}

    @Mutable interface MutableInterface {}

    @ReceiverDependentMutable interface RDMInterface {}

    // :: error: class.bound.invalid
    @Readonly interface ReadonlyInterface {}

    // :: error: class.bound.invalid
    @PolyMutable interface PolyMutableInterface {}

    /* @Immutable */ abstract class ImmutableAbstractClassImplict {}

    @Immutable abstract class ImmutableAbstractClassExplicit {}

    @Mutable abstract class MutableAbstractClass {}

    @ReceiverDependentMutable abstract class RDMAbstractClass {}

    // :: error: class.bound.invalid
    @Readonly abstract class ReadonlyAbstractClass {}

    // :: error: class.bound.invalid
    @PolyMutable abstract class PolyMutableAbstractClass {}

    /* @Immutable */ class ImmutableClassImplict {

        /* @Immutable */ ImmutableClassImplict() {}

        @Immutable ImmutableClassImplict(int i) {}

        // :: error: (type.invalid.annotations.on.use)
        @Mutable ImmutableClassImplict(String str) {}

        // :: error: (type.invalid.annotations.on.use)
        @ReceiverDependentMutable ImmutableClassImplict(char ch) {}

        // when not annotated explictly, default annotations of <this> is inherited from declaration
        void method1(/* @Immutable */ ImmutableClassImplict this) {}

        void method2(@Immutable ImmutableClassImplict this) {}

        // @Immutable
        void method3(@Readonly ImmutableClassImplict this) {}

        void method4(@PolyMutable ImmutableClassImplict this) {}

        // :: error: (type.invalid.annotations.on.use)
        void method5(@ReceiverDependentMutable ImmutableClassImplict this) {}

        // :: error: (type.invalid.annotations.on.use)
        void method6(@Mutable ImmutableClassImplict this) {}
    }

    @Immutable class ImmutableClassExplicit {}

    @Mutable class MutableClass {
        /* @Mutable */ MutableClass() {}

        // :: error: (type.invalid.annotations.on.use)
        @Immutable MutableClass(int i) {}

        @Mutable MutableClass(String str) {}

        // :: error: (type.invalid.annotations.on.use)
        @ReceiverDependentMutable MutableClass(char ch) {}

        // when not annotated explictly, default annotations of <this> is inherited from declaration
        void method1(/* @Mutable */ MutableClass this) {}

        // :: error: (type.invalid.annotations.on.use)
        void method2(@Immutable MutableClass this) {}

        void method3(@Readonly MutableClass this) {}

        void method4(@PolyMutable MutableClass this) {}

        // :: error: (type.invalid.annotations.on.use)
        void method5(@ReceiverDependentMutable MutableClass this) {}

        void method6(@Mutable MutableClass this) {}
    }

    @ReceiverDependentMutable class RDMClass {
        /* @RDM */ RDMClass() {}

        @Immutable RDMClass(int i) {}

        @Mutable RDMClass(String str) {}

        @ReceiverDependentMutable RDMClass(char ch) {}

        // when not annotated explictly, default annotations of <this> is inherited from declaration
        void method1(/* @RDM */ RDMClass this) {}

        void method2(@Immutable RDMClass this) {}

        void method3(@Readonly RDMClass this) {}

        void method4(@PolyMutable RDMClass this) {}

        void method5(@ReceiverDependentMutable RDMClass this) {}

        void method6(@Mutable RDMClass this) {}
    }

    @ReceiverDependentMutable static class RDMStaticClass {}

    // :: error: class.bound.invalid
    @Readonly class ReadonlyClass {}

    // :: error: class.bound.invalid
    @PolyMutable class PolyMutableClass {}

    void testObjectCreation() {
        // Default to @Immutable for RDM class constructor without annotation
        @Immutable RDMClass rdmClass = new RDMClass();
        @Mutable MutableClass mutableClass = new MutableClass();
        @Immutable ImmutableClassExplicit immutableClassExplicit = new ImmutableClassExplicit();
        new /* @Immutable */ ImmutableClassImplict();
        new @Immutable ImmutableClassImplict();
        // :: error: constructor.invocation.invalid
        new @Mutable ImmutableClassImplict();
        new /* @Immutable */ ImmutableClassExplicit();
        new @Immutable ImmutableClassExplicit();
        // :: error: constructor.invocation.invalid
        new @Mutable ImmutableClassExplicit();
        new /* @Mutable */ MutableClass();
        // :: error: constructor.invocation.invalid
        new @Immutable MutableClass();
        new @Mutable MutableClass();
        @Immutable Object obj = new /* @Immutable */ RDMClass();
        new @Immutable RDMClass();
        new @Mutable RDMClass();
        new @ReceiverDependentMutable RDMClass();
        // Constructor return is @Immutable
        new /* @Immutable */ RDMClass(1);
        new @Immutable RDMClass(1);
        // :: error: constructor.invocation.invalid
        new @Mutable RDMClass(1);
        // :: error: constructor.invocation.invalid
        new @ReceiverDependentMutable RDMClass(1);
        // Constructor return is @Mutable
        new /* @Mutable */ RDMClass("str");
        new @Mutable RDMClass("str");
        // :: error: constructor.invocation.invalid
        new @Immutable RDMClass("str");
        // :: error: constructor.invocation.invalid
        new @ReceiverDependentMutable RDMClass("str");
        // Constructor return is @ReceiverDependentMutable
        new /* @RDM */ RDMClass('c');
        new @Immutable RDMClass('c');
        new @Mutable RDMClass('c');
        new @ReceiverDependentMutable RDMClass('c');
        // :: error: constructor.invocation.invalid
        new @PolyMutable ImmutableClassImplict();
        // :: error: constructor.invocation.invalid
        new @Readonly ImmutableClassImplict();
        // :: error: constructor.invocation.invalid
        new @PolyMutable ImmutableClassExplicit();
        // :: error: constructor.invocation.invalid
        new @Readonly ImmutableClassExplicit();
        // :: error: constructor.invocation.invalid
        new @PolyMutable MutableClass();
        // :: error: constructor.invocation.invalid
        new @Readonly MutableClass();
        // TODO :: error: constructor.invocation.invalid
        new @PolyMutable RDMClass();
        // :: error: constructor.invocation.invalid
        new @Readonly RDMClass();
    }

    // Subclassing check
    class ImmutableChildClassGood1 extends RDMClass {}

    class ImmutableChildClassGood2 extends ImmutableClassExplicit {}

    class ImmutableChildClassGood3 implements ImmutableInterfaceExplict, RDMInterface {}

    class ImmutableChildClassGood4 implements RDMInterface {}

    @Immutable class ImmutableChildClassGood5 implements RDMInterface {}

    // :: error: (declaration.inconsistent.with.extends.clause) :: error: (super.invocation.invalid)
    class ImmutableChildClassBad1 extends MutableClass {}

    // :: error: (declaration.inconsistent.with.implements.clause)
    class ImmutableChildClassBad2 implements MutableInterface {}

    @Mutable class MutableChildClassGood1 extends RDMClass {}

    @Mutable class MutableChildClassGood2 extends MutableClass {}

    @Mutable class MutableChildClassGood3 implements MutableInterface, RDMInterface {}

    @Mutable class MutableChildClassGood4 implements RDMInterface {}

    // :: error: (declaration.inconsistent.with.extends.clause) :: error: (super.invocation.invalid)
    @Mutable class MutableChildClassBad1 extends ImmutableClassExplicit {}

    // :: error: (declaration.inconsistent.with.implements.clause)
    @Mutable class MutableChildClassBad2 implements ImmutableInterfaceExplict {}

    @ReceiverDependentMutable class RDMChildClassGood1 extends RDMClass {}

    @ReceiverDependentMutable class RDMChildClassGood2 implements RDMInterface {}

    // :: error: (declaration.inconsistent.with.extends.clause) :: error: (super.invocation.invalid)
    @ReceiverDependentMutable class RDMChildClassBad1 extends ImmutableClassExplicit {}

    // :: error: (declaration.inconsistent.with.implements.clause)
    @ReceiverDependentMutable class RDMChildClassBad2 implements ImmutableInterfaceExplict {}

    // :: error: (declaration.inconsistent.with.extends.clause) :: error: (super.invocation.invalid)
    @ReceiverDependentMutable class RDMChildClassBad3 extends MutableClass {}

    // :: error: (declaration.inconsistent.with.implements.clause)
    @ReceiverDependentMutable class RDMChildClassBad4 implements MutableInterface {}
}
