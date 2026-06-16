import org.checkerframework.checker.pico.qual.Immutable;

public @Immutable class ConstructorAssignment {
    public int x = 3; // static assignment is OK

    ConstructorAssignment() {
        x = 4; // constructor assignment is OK
    }

    void setX() {
        // old: (glacier.assignment)
        // :: error: (illegal.field.write)
        x = 5;
    }
}

class OtherClass {
    OtherClass() {
        ConstructorAssignment c = new ConstructorAssignment();
        // old: (glacier.assignment)
        // AOSEN TODO: what happened?
        // :: error: (illegal.field.write)
        c.x = 6;
    }
}
