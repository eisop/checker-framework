import org.checkerframework.checker.initialization.qual.NotOnlyInitialized;
import org.checkerframework.checker.initialization.qual.UnderInitialization;

public class MutabilityCircularInit {
    class List {
        @NotOnlyInitialized ListNode head;

        List() {
            head = new ListNode(this);
        }
    }

    class ListNode {
        @NotOnlyInitialized List list;

        ListNode(@UnderInitialization List list) {
            this.list = list;
        }
    }
}
