import org.checkerframework.common.subtyping.qual.Unqualified;

public class LhsExceptionTypeVarNext {
    <T> void defaulted(@Unqualified T p) {
        // Type-checks only if the local variable default @Unqualified applies to t.
        T t = p;
    }
}
