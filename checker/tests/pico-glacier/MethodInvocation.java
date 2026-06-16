import org.checkerframework.checker.pico.qual.*;

@Immutable interface Interface {
    public void doStuff();
}

public class MethodInvocation {
    public void foo() {
        Interface i = null;
        i.doStuff();
    }
}
