// @below-java10-jdk-skip-test

public class Issue6098 {
    <T> T method(T t) {
        return t;
    }

    void use() {
        var c = method("");
    }
}
