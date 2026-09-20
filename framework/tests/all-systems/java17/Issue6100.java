// @below-java17-jdk-skip-test
// @infer-jaifs-skip-test The AFU's JAIF reading/writing libraries don't support records.

import org.checkerframework.checker.index.qual.NonNegative;

import java.util.List;

@SuppressWarnings("signedness")
public record Issue6100(List<@NonNegative Integer> bar) {

    public Issue6100 {
        if (bar.size() < 0) {
            throw new IllegalArgumentException();
        }
    }
}
