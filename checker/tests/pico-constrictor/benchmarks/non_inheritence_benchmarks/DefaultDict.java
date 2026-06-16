import org.checkerframework.checker.pico.qual.Readonly;

import java.util.Map;

// Done

public class DefaultDict {
    private int defaultVal;
    private Map<Integer, Integer> dict;

    // @viewmethod
    public int getValue(@Readonly DefaultDict this, int key) {
        if (this.dict.containsKey(key)) {
            return this.dict.get(key);
        } else {
            // :: error: (method.invocation.invalid) The modification should be non-observable, but
            // pico disallow this as dict should be in the abstract state? or should it?
            this.dict.put(key, this.defaultVal);
            return this.defaultVal;
        }
    }

    // @immutable
    public @Readonly Map<Integer, Integer> getUnderlyingDict(@Readonly DefaultDict this) {
        return this.dict;
    }
}
