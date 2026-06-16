import org.checkerframework.checker.pico.qual.Immutable;

@Immutable class Inner {
    int x;
}

public @Immutable class Transitivity {
    Inner i;

    public Transitivity() {}

    public void test() {}
}
