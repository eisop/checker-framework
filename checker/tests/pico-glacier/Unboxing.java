public class Unboxing {
    public void takeNumber(Number n) {}
    ;

    public void passDouble() {
        takeNumber(42.0);
    }
}
;
