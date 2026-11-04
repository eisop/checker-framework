import org.checkerframework.checker.index.qual.GrowOnly;

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

public class GrowOnlyViewTest {

    void testViewCollection(@GrowOnly List<String> list) {
        Collection<String> cc = Collections.checkedCollection(list, String.class);
        // :: error: (method.invocation.invalid)
        cc.clear();

        Collection<String> syncC = Collections.synchronizedCollection(list);
        // :: error: (method.invocation.invalid)
        syncC.clear();

        List<String> subList = list.subList(2, 4);
        // :: error: (method.invocation.invalid)
        subList.clear();
    }

    void testIterator(@GrowOnly Iterator<String> itor) {
        itor.next();
        // :: error: (method.invocation.invalid)
        itor.remove();
    }

    void testIteratorMethod(@GrowOnly List<String> list) {
        Iterator<String> itor = list.iterator();
        itor.next();
        // :: error: (method.invocation.invalid)
        itor.remove();
    }
}
