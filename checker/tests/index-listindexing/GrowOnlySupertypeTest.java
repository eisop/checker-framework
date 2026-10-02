import java.util.AbstractCollection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Deque;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import org.checkerframework.checker.index.qual.GrowOnly;

public class GrowOnlySupertypeTest {

  List<String> list2 = Arrays.asList("hello");

  void testList(@GrowOnly List<String> list) {
    Collection<String> c = list;
    // :: error: (method.invocation.invalid)
    c.remove("hello");
    // :: error: (method.invocation.invalid)
    c.removeAll(list2);
    // :: error: (method.invocation.invalid)
    c.removeIf(s -> s.equals("hello"));
    // :: error: (method.invocation.invalid)
    c.retainAll(list2);
    // :: error: (method.invocation.invalid)
    c.clear();

    Iterable<String> ible = list;
    Iterator<String> itor = ible.iterator();
    itor.next();
    // :: error: (method.invocation.invalid)
    itor.remove();
  }

  void testAbstractCollection(@GrowOnly AbstractCollection<String> ac) {
    // :: error: (method.invocation.invalid)
    ac.remove("hello");
    // :: error: (method.invocation.invalid)
    ac.removeAll(list2);
    // :: error: (method.invocation.invalid)
    ac.retainAll(list2);
    // :: error: (method.invocation.invalid)
    ac.clear();
  }

  void testArrayList(@GrowOnly ArrayList<String> list) {
    AbstractCollection<String> ac = list;
    // :: error: (method.invocation.invalid)
    ac.remove("hello");
    // :: error: (method.invocation.invalid)
    ac.removeAll(list2);
    // :: error: (method.invocation.invalid)
    ac.retainAll(list2);
    // :: error: (method.invocation.invalid)
    ac.clear();
  }

  void testDeque(@GrowOnly Deque<String> d) {
    // :: error: (method.invocation.invalid)
    d.remove("hello");
    // :: error: (method.invocation.invalid)
    d.removeAll(list2);
    // :: error: (method.invocation.invalid)
    d.removeIf(s -> s.equals("hello"));
    // :: error: (method.invocation.invalid)
    d.retainAll(list2);
    // :: error: (method.invocation.invalid)
    d.clear();
  }

  void testLinkedList(@GrowOnly LinkedList<String> list) {
    Queue<String> q = list;
    // :: error: (method.invocation.invalid)
    q.remove("hello");
    // :: error: (method.invocation.invalid)
    q.removeAll(list2);
    // :: error: (method.invocation.invalid)
    q.removeIf(s -> s.equals("hello"));
    // :: error: (method.invocation.invalid)
    q.retainAll(list2);
    // :: error: (method.invocation.invalid)
    q.clear();
    // :: error: (method.invocation.invalid)
    q.poll();

    Deque<String> d = list;
    // :: error: (method.invocation.invalid)
    d.remove("hello");
    // :: error: (method.invocation.invalid)
    d.removeAll(list2);
    // :: error: (method.invocation.invalid)
    d.removeIf(s -> s.equals("hello"));
    // :: error: (method.invocation.invalid)
    d.retainAll(list2);
    // :: error: (method.invocation.invalid)
    d.clear();

    // :: error: (method.invocation.invalid)
    d.poll();
    // :: error: (method.invocation.invalid)
    d.pollFirst();
    // :: error: (method.invocation.invalid)
    d.pollLast();
    // :: error: (method.invocation.invalid)
    d.pop();
    // :: error: (method.invocation.invalid)
    d.remove();
    // :: error: (method.invocation.invalid)
    d.removeFirst();
    // :: error: (method.invocation.invalid)
    d.removeFirstOccurrence("hello");
    // :: error: (method.invocation.invalid)
    d.removeLast();
    // :: error: (method.invocation.invalid)
    d.removeLastOccurrence("hello");
  }
}
