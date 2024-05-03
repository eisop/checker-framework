class Word {
    Object get(int i) {
        return null;
    }
}

public class Primitive3 {

    void foo(Word word) {
        String[] params = {};
        // TODO
        // I reenable type cast safety checking when the cast type is implicitly immutable.
        // Why should we suppress warning just because cast type is implicitly immutable?
        // That doesn't make any sense. Am I right?
        // No cast.unsafe
        params[0] = (String) word.get(0);
    }
}
