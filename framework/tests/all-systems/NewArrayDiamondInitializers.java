import java.util.Arrays;
import java.util.List;

// Type argument inference of each generic initializer, a diamond constructor invocation or a
// generic method invocation, uses the array's component type as its target type. Computing that
// target type must not infer the other initializers again: if it did, the time to check these
// arrays would grow exponentially with the number of initializers.
public class NewArrayDiamondInitializers {
    static class Pair<A, B> {
        Pair(A a, B b) {}
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    static final Pair<String, Integer>[] RAW =
            new Pair[] {
                new Pair<>("a", 1),
                new Pair<>("b", 2),
                new Pair<>("c", 3),
                new Pair<>("d", 4),
                new Pair<>("e", 5),
                new Pair<>("f", 6),
                new Pair<>("g", 7),
                new Pair<>("h", 8),
                new Pair<>("i", 9),
                new Pair<>("j", 10),
                new Pair<>("k", 11),
                new Pair<>("l", 12),
            };

    Object[] withoutNew = {
        new Pair<>("a", 1),
        new Pair<>("b", 2),
        new Pair<>("c", 3),
        new Pair<>("d", 4),
        new Pair<>("e", 5),
        new Pair<>("f", 6),
        new Pair<>("g", 7),
        new Pair<>("h", 8),
        new Pair<>("i", 9),
        new Pair<>("j", 10),
        new Pair<>("k", 11),
        new Pair<>("l", 12),
    };

    Object[] methods = {
        Arrays.asList("a"),
        Arrays.asList("b"),
        Arrays.asList("c"),
        Arrays.asList("d"),
        Arrays.asList("e"),
        Arrays.asList("f"),
        Arrays.asList("g"),
        Arrays.asList("h"),
        Arrays.asList("i"),
        Arrays.asList("j"),
        Arrays.asList("k"),
        Arrays.asList("l"),
    };

    // The Lock Checker does not infer the type arguments of a diamond in a nested array
    // initializer.
    @SuppressWarnings("lock:type.arguments.not.inferred")
    Object[][] nested = {
        {new Pair<>("a", 1), new Pair<>("b", 2), new Pair<>("c", 3), new Pair<>("d", 4)},
        {new Pair<>("e", 5), new Pair<>("f", 6), new Pair<>("g", 7), new Pair<>("h", 8)},
    };

    List<Object> argument =
            Arrays.asList(
                    new Object[] {
                        new Pair<>("a", 1),
                        new Pair<>("b", 2),
                        new Pair<>("c", 3),
                        new Pair<>("d", 4),
                        new Pair<>("e", 5),
                        new Pair<>("f", 6),
                        new Pair<>("g", 7),
                        new Pair<>("h", 8),
                        new Pair<>("i", 9),
                        new Pair<>("j", 10),
                        new Pair<>("k", 11),
                        new Pair<>("l", 12),
                    });
}
