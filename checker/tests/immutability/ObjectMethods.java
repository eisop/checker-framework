import org.checkerframework.checker.immutability.qual.Immutable;
import org.checkerframework.checker.immutability.qual.Mutable;
import org.checkerframework.checker.immutability.qual.Readonly;
import org.checkerframework.checker.immutability.qual.ReceiverDependantMutable;

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

@Immutable
class ObjectMethods2 {

    @Immutable
    ObjectMethods2() {}

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

@ReceiverDependantMutable
class ObjectMethods3 {

    @ReceiverDependantMutable
    ObjectMethods3() {}

    @Override
    public int hashCode() {
        return super.hashCode();
    }

    @Override
    public boolean equals(Object o) {
        return super.equals(o);
    }

    @Override
    protected @ReceiverDependantMutable Object clone(@ReceiverDependantMutable ObjectMethods3 this)
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
        // :: warning: (cast.unsafe)
        return (@Mutable Object) super.clone();
    }

    @Override
    public String toString(@Readonly ObjectMethods4 this) {
        return super.toString();
    }
}

@Immutable
class ObjectMethods5 {

    @Immutable
    ObjectMethods5() {}

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
        // :: warning: (cast.unsafe)
        return (@Immutable Object) super.clone();
    }

    @Override
    public String toString(@Readonly ObjectMethods5 this) {
        return super.toString();
    }
}

@ReceiverDependantMutable
class ObjectMethods6 {

    @Immutable
    ObjectMethods6() {}

    @Override
    public int hashCode(@Readonly ObjectMethods6 this) {
        return super.hashCode();
    }

    @Override
    public boolean equals(@Readonly ObjectMethods6 this, @Readonly Object o) {
        return super.equals(o);
    }

    @Override
    protected @ReceiverDependantMutable Object clone(@Readonly ObjectMethods6 this)
            throws CloneNotSupportedException {
        // No cast.unsafe
        return (@ReceiverDependantMutable Object) super.clone();
    }

    @Override
    public String toString(@Readonly ObjectMethods6 this) {
        return super.toString();
    }
}
