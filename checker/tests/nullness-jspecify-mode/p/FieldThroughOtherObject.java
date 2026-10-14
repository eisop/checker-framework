package p;

// Test case for eisop#2259: -Amode=jspecify crashed on a field read through another object.
public final class FieldThroughOtherObject {
    String name = "";

    String other(FieldThroughOtherObject that) {
        return that.name;
    }

    String self() {
        return this.name;
    }
}
