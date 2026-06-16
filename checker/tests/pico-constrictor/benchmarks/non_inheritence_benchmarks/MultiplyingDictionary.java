import org.checkerframework.checker.pico.qual.Readonly;

import java.util.Map;

public class MultiplyingDictionary {
    private Map<Integer, Integer> dictionary;

    // @viewmethod
    public int get(@Readonly MultiplyingDictionary this, int key) {
        return this.dictionary.get(key) * 2;
    }

    public void add(int key, int value) {
        this.dictionary.put(key, value);
    }
}
