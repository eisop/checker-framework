// Test case for issue #3711: https://github.com/typetools/checker-framework/issues/3711

public class CharToFloat {
    void castCharacter(Object o) {
        // :: error: (cast.incompatible)
        floatParameter((Character) o);
        // :: error: (cast.incompatible)
        doubleParameter((Character) o);
    }

    void passCharacter(Character c) {
        floatParameter(c);
        doubleParameter(c);
    }

    void floatParameter(float f) {}

    void doubleParameter(double d) {}
}
