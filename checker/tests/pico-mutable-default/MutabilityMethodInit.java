import org.checkerframework.checker.initialization.qual.EnsuresInitialized;
import org.checkerframework.checker.initialization.qual.UnderInitialization;
import org.checkerframework.checker.mutability.qual.Immutable;

@Immutable public class MutabilityMethodInit {
    Object a;
    Object b;
    Object c;

    MutabilityMethodInit() {
        initA();
        initB();
        initC();
    }

    @EnsuresInitialized("this.a")
    void initA(@UnderInitialization(Object.class) MutabilityMethodInit this) {
        this.a = new @Immutable Object();
    }

    @EnsuresInitialized("this.b")
    void initB(@UnderInitialization(Object.class) MutabilityMethodInit this) {
        this.b = new @Immutable Object();
    }

    @EnsuresInitialized("this.c")
    void initC(@UnderInitialization(Object.class) MutabilityMethodInit this) {
        this.c = new @Immutable Object();
    }
}
