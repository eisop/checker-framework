import org.checkerframework.checker.immutability.qual.Immutable;

import java.util.HashMap;
import java.util.Map;

@Immutable
class A {
    boolean isIn = false;

    @Override
    public int hashCode() {
        return isIn ? 1 : 0;
    }

    @Override
    public boolean equals(Object obj) {
        // No cast.unsafe
        return isIn == ((A) obj).isIn;
    }
}

public class HashCodeSafetyExample {
    public static void main(String[] args) {
        A a = new A();
        HashMap<A, String> m = new HashMap<>();
        m.put(a, "hello");
        System.out.println("HashCode before: " + a.hashCode());
        // :: error: (illegal.field.write)
        a.isIn = true;
        System.out.println("HashCode after: " + a.hashCode());
        System.out.println(m.get(a));
        m.put(new A(), "WORLD");
        System.out.println("Iterating entries:");
        // Even though using object a whose hashcode is mutated returns null,
        // iterating over entryset did return correct mapping between keys and values,
        // which is strange and looks like inconsistency.
        for (Map.Entry<A, String> entry : m.entrySet()) {
            System.out.println("key: " + entry.getKey());
            System.out.println("value: " + entry.getValue());
        }
    }
}
