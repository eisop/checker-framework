import org.checkerframework.checker.pico.qual.Immutable;
import org.checkerframework.checker.pico.qual.Mutable;
import org.checkerframework.checker.pico.qual.PolyMutable;
import org.checkerframework.checker.pico.qual.Readonly;
import org.checkerframework.checker.pico.qual.ReceiverDependentMutable;

public class PolyMutableTest {
    @Mutable private static class MutableClass {
        int field = 0;
    }

    @ReceiverDependentMutable private class RDMHolder {

        // :: error: (type.invalid.annotations.on.use)
        @ReceiverDependentMutable MutableClass field = new MutableClass();
        @Mutable MutableClass mutableField = new MutableClass();

        public @PolyMutable MutableClass getField(@PolyMutable RDMHolder this) {
            return field;
        }

        public void setField(@Mutable RDMHolder this, MutableClass field) {
            this.field = field;
        }

        void asImmutable(@Immutable RDMHolder r) {
            // :: error: (illegal.field.write)
            r.field.field = 1;
            // :: error: (illegal.field.write)
            r.getField().field = 1;
            // :: error: (method.invocation.invalid)
            r.setField(new MutableClass());
        }
    }

    @Immutable private static class ImmutableHolder {
        // :: error: (type.invalid.annotations.on.use)
        @ReceiverDependentMutable MutableClass field = new MutableClass();

        public @PolyMutable MutableClass getField(@PolyMutable ImmutableHolder this) {
            return field;
        }
    }

    void foo(A a) {
        // Having parentheis here causes StackOverFlowError
        // It causes ((MemberSelectTree) methodInvocation.getMethodSelect()).getExpression()
        // in TypeArgInferenceUtil to return a ParenthesizedTree instead of MethodInvocationTree
        (a.subtract()).multiply();
    }

    class A {
        A subtract() {
            return this;
        }

        A multiply() {
            return this;
        }

        @ReceiverDependentMutable Object read(@Readonly A this, @PolyMutable Object p) {
            return new @ReceiverDependentMutable Object();
        }
    }

    @PolyMutable Object bar(@PolyMutable A a) {
        // Typecheck now. Only when the declared type is @PolyMutable, after viewpoint adadptation,
        // it becomes @SubsitutablePolyMutable, and then will be resolved by QualifierPolymorphism
        // Note: viewpoint adaptation(ATF) happens before QualfierPolymorphism(GATF) in current
        // implementation
        @PolyMutable Object result = a.read(new @Immutable Object());
        return result;
    }

    @ReceiverDependentMutable class B {
        @PolyMutable B getObject(@Mutable B this) {
            return null;
        }

        @PolyMutable B getSecondObject(@PolyMutable B this) {
            return null;
        }

        @PolyMutable B getThirdObject(@Mutable B this) {
            return null;
        }

        @Immutable B getForthObject(@Mutable B this) {
            return this.getThirdObject();
        }

        void test1(@Mutable B mb) {
            @Mutable Object l = mb.getObject();
            @Immutable Object r = mb.getObject();
        }

        void test2(@Mutable B mb) {
            @Mutable Object l = mb.getSecondObject();
            // TODO Should be poly.invocation.error something...
            // :: error: (assignment.type.incompatible)
            @Immutable Object r = mb.getSecondObject();
        }

        void test3(@Immutable B imb) {
            // TODO Should be poly.invocation.error something...
            // :: error: (assignment.type.incompatible)
            @Mutable Object l = imb.getSecondObject();
            @Immutable Object r = imb.getSecondObject();
        }

        void test4(@Mutable B b) {
            // This correctly typechecks
            @Immutable Object r = b.getObject().getThirdObject();
        }

        // TODO Poly return type used on poly receiver. This is not yet implemented yet in CF
        void test5(@Mutable B b) {
            // TODO Should typecheck.
            // :: error: (assignment.type.incompatible)
            @Immutable Object r = b.getSecondObject().getSecondObject();
        }
    }

    class PolyMutableOnConstructorParameters<T> {
        @Immutable PolyMutableOnConstructorParameters(@PolyMutable Object o) {}

        void test(String[] args) {
            @Immutable PolyMutableOnConstructorParameters o1 =
                    new @Immutable PolyMutableOnConstructorParameters(new @Immutable Object());
        }
    }
}
