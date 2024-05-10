import org.checkerframework.checker.immutability.qual.Immutable;
import org.checkerframework.checker.immutability.qual.Mutable;
import org.checkerframework.checker.immutability.qual.Readonly;

import java.util.Date;

/** Planet is an immutable class, since there is no way to change its state after construction. */
@Immutable
public class Planet {
    /** Final primitive data is always immutable. */
    private double fMass;

    /** An immutable object field. (String objects never change state.) */
    private String fName;

    /**
     * An immutable object field. The state of this immutable field can never be changed by anyone.
     */
    private @Immutable Date fDateOfDiscovery;

    public @Immutable Planet(double aMass, String aName, @Immutable Date aDateOfDiscovery) {
        fMass = aMass;
        fName = aName;
        // No need to copy aDateOfDiscovery, as it's @Immutable
        fDateOfDiscovery = aDateOfDiscovery;
    }

    /**
     * Returns a primitive value.
     *
     * <p>The caller can do whatever they want with the return value, without affecting the
     * internals of this class. Why? Because this is a primitive value. The caller sees its "own"
     * double that simply has the same value as fMass.
     */
    public double getMass(@Readonly Planet this) {
        return fMass;
    }

    /**
     * Returns an immutable object.
     *
     * <p>The caller gets a direct reference to the internal field. But this is not dangerous, since
     * String is immutable and cannot be changed.
     */
    public String getName(@Readonly Planet this) {
        return fName;
    }

    //  /**
    //  * Returns a mutable object - likely bad style.
    //  *
    //  * The caller gets a direct reference to the internal field. This is usually dangerous,
    //  * since the Date object state can be changed both by this class and its caller.
    //  * That is, this class is no longer in complete control of fDate.
    //  */
    //  public Date getDateOfDiscovery() {
    //    return fDateOfDiscovery;
    //  }

    /** Not need to return a defensive copy of the field. */
    public @Immutable Date getDateOfDiscovery(@Readonly Planet this) {
        return fDateOfDiscovery;
    }

    @Override
    public String toString() {
        // TODO Handle case in which fDateOfDiscovery is not @Immutable
        return "Name: " + fName + " mass: " + fMass + " date of discovery: " + fDateOfDiscovery;
    }

    public static void main(String[] args) {
        @Immutable Date discoveryDate = new @Immutable Date();
        // :: error: (type.invalid.annotations.on.use)
        @Mutable Planet mPlanet;
        // :: error: (constructor.invocation.invalid)
        mPlanet = new @Mutable Planet(1, "Earth", discoveryDate);
        @Immutable Planet imPlanet = new @Immutable Planet(1, "Earth", discoveryDate);
        // None of the fields are allowed to be modified on an immutable object
        // :: error: (illegal.field.write)
        imPlanet.fMass = 2;
        // :: error: (illegal.field.write)
        imPlanet.fName = "Jupitor";
        // :: error: (illegal.field.write)
        imPlanet.fDateOfDiscovery = new @Immutable Date();
        // :: error: (method.invocation.invalid)
        imPlanet.fDateOfDiscovery.setTime(123L);
        // Object returned by getter method is neither modifiable
        // :: error: (method.invocation.invalid)
        imPlanet.getDateOfDiscovery().setTime(123L);
        // Caller cannot mutate date object passed into imPlanet object
        // :: error: (method.invocation.invalid)
        discoveryDate.setTime(123L);
    }
}
