package com.example;

public class Demo {
    // Under package-level @DefaultQualifier(Interned.class), s is @Interned String.
    void acceptInterned(String s) {
        // ok
    }

    void test() {
        String notInterned = new String("test");
        acceptInterned(notInterned);
    }
}
