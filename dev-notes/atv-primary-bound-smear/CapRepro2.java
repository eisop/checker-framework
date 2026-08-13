import java.util.List;
import org.checkerframework.checker.nullness.qual.Nullable;

public class CapRepro2 {

    <T extends @Nullable Object> T viaVar(List<? extends T> l) {
        var x = l.get(0);
        T y = x;
        return y;
    }

    <T extends @Nullable Object> void refined(List<? extends T> l) {
        var x = l.get(0);
        if (x != null) {
            x.toString();
        }
    }

    static <T> List<T> catListAndIterable(List<T> newList, Iterable<? extends T> iterable) {
        for (T iterObject : iterable) {
            newList.add(iterObject);
        }
        return newList;
    }
}
