import org.checkerframework.checker.pico.qual.AbstractStateOnly;
import org.checkerframework.checker.pico.qual.Assignable;

import java.util.Objects;

public final class MyString {
    private final char[] value; // Abstract state

    private @Assignable int hash; // Cached hash code, mutable, not abstract state

    public MyString(char[] value) {
        this.value = value.clone();
    }

    public int length() {
        return value.length;
    }

    public char charAt(int index) {
        return value[index];
    }

    @Override
    @AbstractStateOnly
    public int hashCode() {
        int h = hash;
        if (h == 0 && value.length > 0) {
            for (char c : value) {
                h = 31 * h + c;
            }
            hash = h;
        }
        return h;
    }

    @Override
    @AbstractStateOnly
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof MyString)) return false;
        MyString other = (MyString) o;
        return Objects.equals(this.toString(), other.toString());
    }

    @Override
    @AbstractStateOnly
    public String toString() {
        return new String(value);
    }
}
