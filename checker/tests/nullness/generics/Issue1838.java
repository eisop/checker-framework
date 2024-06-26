// Test case for Issue 1838:
// https://github.com/typetools/checker-framework/issues/1838

import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.ArrayList;
import java.util.List;

public class Issue1838 {
    public static void main(String[] args) {
        f();
    }

    public static void f() {
        List<@Nullable Object> list = new ArrayList<>();
        list.add(null);
        List<List<@Nullable Object>> listList = new ArrayList<List<@Nullable Object>>();
        listList.add(list);
        // :: error: (argument.type.incompatible)
        processElements(listList);
    }

    private static void processElements(List<? extends List<Object>> listList) {
        for (List<Object> list : listList) {
            for (Object element : list) {
                element.toString();
            }
        }
    }
}
