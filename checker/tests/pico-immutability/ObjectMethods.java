import org.checkerframework.checker.pico.qual.Immutable;
import org.checkerframework.checker.pico.qual.Readonly;
import org.checkerframework.checker.pico.qual.ReceiverDependentMutable;

public class ObjectMethods {
    // Don't have any warnings now
    @Override
    public int hashCode() {
        return super.hashCode();
    }

    @Override
    public boolean equals(Object o) {
        return super.equals(o);
    }

    @Override
    protected Object clone() throws CloneNotSupportedException {
        return super.clone();
    }

    @Override
    public String toString() {
        return super.toString();
    }
}

@Immutable class ObjectMethods2 {

    @Immutable ObjectMethods2() {}

    @Override
    public int hashCode() {
        return super.hashCode();
    }

    @Override
    public boolean equals(Object o) {
        return super.equals(o);
    }

    @Override
    protected @Immutable Object clone(@Immutable ObjectMethods2 this)
            throws CloneNotSupportedException {
        return super.clone();
    }

    @Override
    public String toString() {
        return super.toString();
    }
}

@ReceiverDependentMutable class ObjectMethods3 {

    @ReceiverDependentMutable ObjectMethods3() {}

    @Override
    public int hashCode() {
        return super.hashCode();
    }

    @Override
    public boolean equals(Object o) {
        return super.equals(o);
    }

    @Override
    protected @ReceiverDependentMutable Object clone(@ReceiverDependentMutable ObjectMethods3 this)
            throws CloneNotSupportedException {
        return super.clone();
    }

    @Override
    public String toString() {
        return super.toString();
    }
}

class ObjectMethods4 {
    @Override
    public int hashCode(@Readonly ObjectMethods4 this) {
        return super.hashCode();
    }

    @Override
    public boolean equals(@Readonly ObjectMethods4 this, @Readonly Object o) {
        return super.equals(o);
    }

    @Override
    protected Object clone(@Readonly ObjectMethods4 this) throws CloneNotSupportedException {
        // :: error: (method.invocation.invalid) :: error: (return.type.incompatible)
        return super.clone();
    }

    @Override
    public String toString(@Readonly ObjectMethods4 this) {
        return super.toString();
    }
}

@Immutable class ObjectMethods5 {

    @Immutable ObjectMethods5() {}

    @Override
    public int hashCode(@Readonly ObjectMethods5 this) {
        return super.hashCode();
    }

    @Override
    public boolean equals(@Readonly ObjectMethods5 this, @Readonly Object o) {
        return super.equals(o);
    }

    @Override
    protected @Immutable Object clone(@Readonly ObjectMethods5 this)
            throws CloneNotSupportedException {
        // :: warning: (cast.unsafe) :: error: (method.invocation.invalid)
        return (@Immutable Object) super.clone();
    }

    @Override
    public String toString(@Readonly ObjectMethods5 this) {
        return super.toString();
    }
}

@ReceiverDependentMutable class ObjectMethods6 {

    @Immutable ObjectMethods6() {}

    @Override
    public int hashCode(@Readonly ObjectMethods6 this) {
        return super.hashCode();
    }

    @Override
    public boolean equals(@Readonly ObjectMethods6 this, @Readonly Object o) {
        return super.equals(o);
    }

    @Override
    protected @ReceiverDependentMutable Object clone(@Readonly ObjectMethods6 this)
            throws CloneNotSupportedException {
        // No cast.unsafe
        // :: error: (method.invocation.invalid)
        return (@ReceiverDependentMutable Object) super.clone();
    }

    @Override
    public String toString(@Readonly ObjectMethods6 this) {
        return super.toString();
    }
}
