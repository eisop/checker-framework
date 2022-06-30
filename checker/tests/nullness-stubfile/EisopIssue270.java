import java.util.Set;

public class EisopIssue270 {
    void foo(Set<Object> so, Set<? extends Object> seo) {
        so.retainAll(seo);
    }
}
