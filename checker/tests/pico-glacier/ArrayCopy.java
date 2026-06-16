import org.checkerframework.checker.pico.qual.Immutable;

public class ArrayCopy {
    public void takeArray(@Immutable Object @Immutable [] array) {}

    public void passArray() {
        @Immutable Object array[] = new @Immutable Object[5];

        takeArray(array.clone());
    }
}
