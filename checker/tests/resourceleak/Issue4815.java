// Test case for https://tinyurl.com/cfissue/4815

import org.checkerframework.checker.mustcall.qual.MustCall;
import org.checkerframework.checker.mustcall.qual.Owning;

import java.util.List;

public class Issue4815 {
    public <T extends Component> void initialize(
            List<T> list, @Owning @MustCall("initialize") T object) {
        object.initialize();
        list.add(object);
    }

    private static class Component {
        void initialize() {}
    }
}
