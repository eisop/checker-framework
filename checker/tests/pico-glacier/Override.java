import org.checkerframework.checker.pico.qual.Immutable;

@Immutable abstract class Superclass {
    public abstract void doStuff(int x);
}

public @Immutable class Override {

    public Override() {}

    public void doStuff(int x) {}
}
