// Adapted from https://www.baeldung.com/immutables
import org.checkerframework.checker.pico.qual.Immutable;
import org.checkerframework.checker.pico.qual.Readonly;

@Immutable public class ImmutablePerson {
    private int idNum;
    private int age;

    public ImmutablePerson(int idNum, int age) {
        this.idNum = idNum;
        this.age = age;
    }

    // @viewmethod
    public int getIdNum(@Readonly ImmutablePerson this) {
        return this.idNum;
    }

    // @viewmethod
    public int getAge(@Readonly ImmutablePerson this) {
        return this.age;
    }

    // @viewmethod
    @Override
    public int hashCode(@Readonly ImmutablePerson this) {
        return this.age * 100 + this.idNum;
    }
}
