import org.checkerframework.checker.pico.qual.Immutable;
import org.checkerframework.checker.pico.qual.Mutable;
import org.checkerframework.checker.pico.qual.Readonly;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

// The situations of creating immutable lists, sets, hashmaps are similar.
// If they are called add/put operations, then they can't be immutable. So,
// it's important to initialize their contents when calling constructors.
// One general solution is to create local mutable lists, sets or hashmaps,
// add contents into them and pass it to a wrapper object that is immutable.
// See ImmutableListProblem(Object o1, Object o2, Object o3) for example.
@Immutable public class ImmutableList {

    List<String> list;

    @Immutable ImmutableList() {
        list = new @Immutable ArrayList<String>();
        // :: error: (method.invocation.invalid)
        list.add("hi"); // Any add() operation after list is constructed forbids the list to be
        // immutable
    }

    @Immutable ImmutableList(Object o1) {
        // One way to construct and immutable list is to pass the contents to the constructor
        list = new @Immutable ArrayList<String>(Arrays.asList("hi"));
    }

    @Immutable ImmutableList(Object o1, Object o2) {
        // Another way is to use Arrays.asList()
        list = Arrays.asList("hi");
    }

    @Immutable ImmutableList(Object o1, Object o2, Object o3) {
        List<String> localList = new @Mutable ArrayList<String>();
        localList.add("hi");
        localList.add("how");
        localList.add("are");
        localList.add("you");
        // Third way is to create a local mutable list, and wrap it with the immutable list but has
        // the same content as the mutable list
        list = new @Immutable ArrayList<String>(localList);
    }

    @Immutable List<String> createImmutableList(@Readonly ImmutableList this) {
        List<String> localList = new @Mutable ArrayList<String>();
        localList.add("hi");
        localList.add("how");
        localList.add("are");
        localList.add("you");
        // After initializing, forcably cast @Mutable list into @Immutable as long as no @Mutable
        // aliases exist
        // :: warning: (cast.unsafe)
        return (@Immutable List<String>) localList;
    }
}
