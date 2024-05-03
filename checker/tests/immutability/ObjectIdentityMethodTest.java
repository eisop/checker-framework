import org.checkerframework.checker.immutability.qual.*;

@ReceiverDependantMutable
class A {
    @Assignable @Mutable B b;

    @ReceiverDependantMutable
    A() {}

    void bar(@Readonly A this) {}
}

class B {}

class Super {
    void foo() {}
}

public class ObjectIdentityMethodTest extends Super {
    static A a0;
    /*Fields NOT in abstract states*/
    @Assignable A a1;
    @Mutable A a2;
    @Readonly A a3;
    /*Fields in abstract state*/
    A a4; // default is @RDA @RDM
    @Immutable A a5;
    final A a6;
    final @Immutable A a7;

    // :: error: (initialization.fields.uninitialized)
    ObjectIdentityMethodTest() {
        a6 = new A();
        a7 = new @Immutable A();
    }

    @ObjectIdentityMethod
    public void testFieldAcess() {
        // :: warning: (object.identity.static.field.access.forbidden) :: warning:
        // (object.identity.method.invocation.invalid)
        a0.bar();
        // :: warning: (object.identity.field.access.invalid) :: warning:
        // (object.identity.method.invocation.invalid)
        a1.bar();
        // :: warning: (object.identity.field.access.invalid) :: warning:
        // (object.identity.method.invocation.invalid)
        a2.bar();
        // :: warning: (object.identity.field.access.invalid) :: warning:
        // (object.identity.method.invocation.invalid)
        a3.bar();
        // Transitively check bar() is object identity method => deep object identity check!
        // :: warning: (object.identity.method.invocation.invalid)
        a4.bar();
        // Transitively check b is in abstract state => deep object identity check!
        // :: warning: (object.identity.field.access.invalid)
        Object o = a4.b;
        // :: warning: (object.identity.method.invocation.invalid)
        a5.bar();
        // :: warning: (object.identity.method.invocation.invalid)
        a6.bar();
        // :: warning: (object.identity.method.invocation.invalid)
        a7.bar();

        /*With explicit "this"*/
        // :: warning: (object.identity.field.access.invalid) :: warning:
        // (object.identity.method.invocation.invalid)
        this.a1.bar();
        // :: warning: (object.identity.field.access.invalid) :: warning:
        // (object.identity.method.invocation.invalid)
        this.a2.bar();
        // :: warning: (object.identity.field.access.invalid) :: warning:
        // (object.identity.method.invocation.invalid)
        this.a3.bar();
        // :: warning: (object.identity.method.invocation.invalid)
        this.a4.bar();
        // Similar argument for deep object identity check as above
        // :: warning: (object.identity.field.access.invalid)
        Object o2 = this.a4.b;
        // :: warning: (object.identity.method.invocation.invalid)
        this.a5.bar();
        // :: warning: (object.identity.method.invocation.invalid)
        this.a6.bar();
        // :: warning: (object.identity.method.invocation.invalid)
        this.a7.bar();
    }

    @ObjectIdentityMethod
    void testMethodInvocation(ObjectIdentityMethodTest p, A a) {
        // :: warning: (object.identity.method.invocation.invalid)
        foo();
        // :: warning: (object.identity.method.invocation.invalid)
        this.foo();
        // :: warning: (object.identity.method.invocation.invalid)
        super.foo();
        // TODO Should these two method invocations also be checked? It's not trivial to only check
        // method invocations on the transitively reachable objects from field. Right now, they are
        // also checked
        // :: warning: (object.identity.method.invocation.invalid)
        p.foo();
        // :: warning: (object.identity.method.invocation.invalid)
        a.bar();
    }

    void foo() {}

    @Override
    public int hashCode() {
        int result = super.hashCode();
        // :: warning: (object.identity.field.access.invalid)
        result += a1.hashCode();
        // :: warning: (object.identity.field.access.invalid)
        result += a2.hashCode();
        // :: warning: (object.identity.field.access.invalid)
        result += a3.hashCode();
        result += a4.hashCode();
        result += a5.hashCode();
        result += a6.hashCode();
        result += a7.hashCode();
        return result;
    }
}
