// @below-java17-jdk-skip-test

package open.falsepos;

import org.checkerframework.checker.nullness.qual.KeyFor;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public record Issue6750(String type) {

    void needKeyFor(@KeyFor("#2") String s, Map<String, String> map) {
        throw new RuntimeException();
    }

    @KeyFor("#1") String returnKeyFor(Map<String, String> map) {
        throw new RuntimeException();
    }

    Map<String, String> getMap(Function<String, String> s) {
        throw new RuntimeException();
    }

    void use() {
        // :: error: (argument.type.incompatible)
        needKeyFor("", getMap(String::toString));
        // :: error: (expression.unparsable.type.invalid) :: error: (assignment.type.incompatible)
        @KeyFor("getMap(String::toString)") String s = returnKeyFor(new HashMap<>(getMap(String::toString)));
    }

    void method(List<Issue6750> externals) {
        externals.stream().collect(Collectors.groupingBy(Issue6750::type)).entrySet().stream()
                .forEach(
                        values -> {
                            // :: error: (assignment.type.incompatible)
                            @KeyFor({}) String b = values.getKey();
                        });
    }

    void test(List<Issue6750> externals) {
        externals.stream().collect(Collectors.groupingBy(Issue6750::type)).entrySet().stream()
                .forEach(
                        values -> {
                            throw new RuntimeException("");
                        });
    }
}
