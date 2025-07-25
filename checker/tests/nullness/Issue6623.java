// @skip-test Test runs too fast on GitHub Actions.
// @below-java14-jdk-skip-test

import java.util.List;

public class Issue6623 {

    public List<TargetOuter> getUserData(List<SrcOuter> input) {
        // Fixing issue #6623 will eliminate this warning.
        return input.stream()
                .map(
                        c ->
                                new TargetOuter(
                                        c.data().stream()
                                                // :: warning: (slow.typechecking)
                                                .map(
                                                        inner ->
                                                                new TargetInner(
                                                                        inner.data(),
                                                                        inner.data(),
                                                                        inner.data(),
                                                                        inner.data()))
                                                .toList()))
                .toList();
    }

    record SrcOuter(List<SrcInner> data) {}

    record SrcInner(int data) {}

    record TargetOuter(List<TargetInner> inner) {}

    record TargetInner(int a, int b, int c, int d) {}
}
