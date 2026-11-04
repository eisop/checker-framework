import java.util.Arrays;
import java.util.List;

// Type argument inference of each generic initializer, a diamond constructor invocation or a
// generic method invocation, uses the array's component type as its target type. Computing that
// target type must not infer the other initializers again: if it did, the time to check these
// arrays would grow quadratically or exponentially with the number of initializers.
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

    // Each initializer is inferred once: getting the type of one initializer does not infer the
    // others again.
    Object[] lists = {
        Arrays.asList(new Pair<>("a", 0)),
        Arrays.asList(new Pair<>("b", 1)),
        Arrays.asList(new Pair<>("c", 2)),
        Arrays.asList(new Pair<>("d", 3)),
        Arrays.asList(new Pair<>("e", 4)),
        Arrays.asList(new Pair<>("f", 5)),
        Arrays.asList(new Pair<>("g", 6)),
        Arrays.asList(new Pair<>("h", 7)),
        Arrays.asList(new Pair<>("i", 8)),
        Arrays.asList(new Pair<>("j", 9)),
        Arrays.asList(new Pair<>("k", 10)),
        Arrays.asList(new Pair<>("l", 11)),
        Arrays.asList(new Pair<>("m", 12)),
        Arrays.asList(new Pair<>("n", 13)),
        Arrays.asList(new Pair<>("o", 14)),
        Arrays.asList(new Pair<>("p", 15)),
        Arrays.asList(new Pair<>("q", 16)),
        Arrays.asList(new Pair<>("r", 17)),
        Arrays.asList(new Pair<>("s", 18)),
        Arrays.asList(new Pair<>("t", 19)),
        Arrays.asList(new Pair<>("u", 20)),
        Arrays.asList(new Pair<>("v", 21)),
        Arrays.asList(new Pair<>("w", 22)),
        Arrays.asList(new Pair<>("x", 23)),
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
