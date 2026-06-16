import org.checkerframework.checker.pico.qual.Readonly;

// Done

public class MutablePerson {
    private int idNum;
    private int age;

    public MutablePerson(int idNum, int age) {
        this.idNum = idNum;
        this.age = age;
    }

    // @viewmethod
    public int getIdNum(@Readonly MutablePerson this) {
        return this.idNum;
    }

    // @viewmethod
    public int getAge(@Readonly MutablePerson this) {
        return this.age;
    }

    // @immutable  # <--- should not be marked const
    public void setAge(@Readonly MutablePerson this, int age) {
        // :: error: (illegal.field.write)
        this.age = age;
    }

    // @viewmethod
    @Override
    public int hashCode(@Readonly MutablePerson this) {
        return this.age * 100 + this.idNum;
    }
}
