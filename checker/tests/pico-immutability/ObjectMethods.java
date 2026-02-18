import org.checkerframework.checker.pico.qual.Immutable;
import org.checkerframework.checker.pico.qual.Mutable;
import org.checkerframework.checker.pico.qual.Readonly;
import org.checkerframework.checker.pico.qual.ReceiverDependentMutable;

public @Mutable class ObjectMethods {
    // Don't have any warnings now
    @Override
    public int hashCode() {
        return super.hashCode();
    }

    @Override
    public boolean equals(@Readonly Object o) {
        return super.equals(o);
    }

    @Override
    protected @Mutable Object clone() throws CloneNotSupportedException {
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
    public boolean equals(@Readonly Object o) {
        return super.equals(o);
    }

    @Override
    protected Object clone(ObjectMethods2 this) throws CloneNotSupportedException {
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
    public int hashCode(@Readonly ObjectMethods3 this) {
        return super.hashCode();
    }

    @Override
    public boolean equals(@Readonly ObjectMethods3 this, @Readonly Object o) {
        return super.equals(o);
    }

    @Override
    protected @ReceiverDependentMutable Object clone(@ReceiverDependentMutable ObjectMethods3 this)
            throws CloneNotSupportedException {
        return super.clone();
    }

    @Override
    public String toString(@Readonly ObjectMethods3 this) {
        return super.toString();
    }
}
