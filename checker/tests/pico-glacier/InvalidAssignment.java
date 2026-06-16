import org.checkerframework.checker.pico.qual.Immutable;

import java.util.Calendar;

public @Immutable class InvalidAssignment {
    class Inner {
        public int i;
    }

    String s;
    // :: error: glacier.mutable.invalid
    Calendar c;
    // :: error: glacier.mutable.invalid
    Inner i;
    int x;

    public void setString(String s) {
        // :: error: glacier.assignment
        this.s = s;
    }

    public void setX(int x) {
        // :: error: glacier.assignment
        this.x = x;
    }

    public void setMonth(int month) {
        c.set(
                Calendar.MONTH,
                month); // No error here; the problem is that d was mutable in the first place.
    }

    public void setInner(int i) {
        this.i.i = i; // No error here; the problem is that this.i was mutable in the first place.
    }
}

class AnotherClass {
    void aMethod() {
        InvalidAssignment i = new InvalidAssignment();

        // ::error: (glacier.assignment)
        i.s = "hello";
    }
}
