interface NA_AnInterface {}
;

public class NullAssignment {
    public void takeObj(Object o) {}

    public void foo() {
        NA_AnInterface i = null;
        takeObj(i);
    }
}
