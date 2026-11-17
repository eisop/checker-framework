package permissivedefaultslib;

import org.checkerframework.checker.tainting.qual.Tainted;
import org.checkerframework.checker.tainting.qual.Untainted;

import java.util.List;

/** Unchecked code: not {@code @AnnotatedFor("tainting")}. */
public class TaintingLib {
    public void unannotatedParameter(String value) {}

    public void taintedParameter(@Tainted String value) {}

    public void taintedElement(List<@Tainted String> values) {}

    public void taintedElementNarrowed(List<@Tainted String> values) {}

    public String unannotatedReturn() {
        return "";
    }

    public @Untainted String untaintedReturn() {
        return "";
    }

    public <T> void implicitBound(T value) {}
}
