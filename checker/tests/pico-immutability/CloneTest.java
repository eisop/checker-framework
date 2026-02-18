// clone() method proposal:
// 1. Let overriding check in type declaration with @Mutable and @Immutable
// bound more flexible.
// In the overriding clone() method in those two type declarations,
// as long as declared receiver and return type are the same as bound,
// overriding check passes. For example, @Mutable class can override clone()
// method as @Mutable Object clone(@Mutable A this) while @Immutable class
// can override to @Immutable Object clone(@Immutable B this). But
// @ReceiverDependentMutable class should keep the exact same signature as that
// in jdk.astub, because both @Mutable and @Immutable might be the client.
// Overriding to either @Mutable or @Immutable may cause existing client to break.

// 2. clone() method has the same defaulted mechanism as other instance methods
// (Remove the special handling of clone() method)

// 3. There is still need to specially handle super.clone() invocation, because @Immutable
// object's @RDA @Mutable field and @RDA @Readonly field are not guaranteed to be @Immutable
// so there might be use cases to clone those fields and reassign them to the cloned copy.

// 3.1 As of method signature of clone() method in terms of initialization qualifiers,
// I would keep method return @Initialized. It's common that overriding method call the constructor
// and returns a new @Initialized cloned copy. Chaning clone() method return to @UnderInitialization
// just for special handling for super invocations doesn't make much sense.
// But this requires us to disable subtype checking of return type in terms of initialization
// hierarchy.

// ======================== Outdated =======================//
// 3.1 Assigning field in cloned copy is an indication of mutable object. Because
// @Immutable objects can not be modified, so no need to deeply clone it. So let's
// treat reassigning cloned object as sign of @Mutable object, thus don't need special
// handling for super.clone()'s result(i.e. no @UnderInitialization staff for the super
// clone() invocation result).

// 3.2 @Immutable classes' clone() method should either: 1) directly call new
// instance creation and all the initialization steps are finished after constructor
// returns. This is also consistent with the case where objectsa are finished initialization
// after constructor returns and no furthur modification is allowed outside constructor.
// 2) doesn't override clone() method(by default shallow copies receiver object which
// makes sense)
// ======================== Outdated =======================//
// @skip-test
public class CloneTest {
    // @Initialized return type
    public Object clone() {
        AbstractDistribution copy =
                (AbstractDistribution) super.clone(); // returns @UnderInitialization object
        // Allow reassigning since copy is @UnderInitialization
        if (this.randomGenerator != null)
            copy.randomGenerator = (RandomEngine) this.randomGenerator.clone();
        // Disable subtype checking in terms of initialization hierarchy. Since
        // @UnderInitialization is not
        // subtype of @Initialized
        return copy;
    }

    public Object clone() {
        return new Algebra(property.tolerance());
    }

    public Object clone() {
        BooleanArrayList clone = new BooleanArrayList((boolean[]) elements.clone());
        // clone finished being initialized, so later instance method call to mutate the state of
        // clone makes clone to be @Mutable. This is different from super.clone() extends
        // initialization
        // scope
        clone.setSizeRaw(size);
        return clone;
    }

    public Object clone() {
        return partFromTo(0, size - 1);
    }

    public AbstractShortList partFromTo(int from, int to) {
        int length = to - from + 1;
        ShortArrayList part = new ShortArrayList(length);
        part.addAllOfFromTo(this, from, to);
        return part;
    }

    public boolean removeAll(CloneProblem other) throws CloneNotSupportedException {
        if (true) {
            CloneProblem l = (CloneProblem) other.overridenClone();
            // Gets method invocation invalid error
            l.foo();
        }
        return true;
    }

    public Object overridenClone() {
        return null;
    }

    void foo() {}
}
