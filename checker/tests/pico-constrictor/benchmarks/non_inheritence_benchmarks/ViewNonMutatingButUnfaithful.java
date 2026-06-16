import org.checkerframework.checker.pico.qual.Assignable;
import org.checkerframework.checker.pico.qual.Readonly;

public class ViewNonMutatingButUnfaithful {
    private @Assignable int a;
    private int b;

    public int setBToA() {
        this.b = this.a;
        return this.b;
    }

    // @immutable
    public int getA(@Readonly ViewNonMutatingButUnfaithful this) {
        return this.a;
    }

    // @viewmethod
    public int getB(@Readonly ViewNonMutatingButUnfaithful this) {
        return this.b;
    }
}
