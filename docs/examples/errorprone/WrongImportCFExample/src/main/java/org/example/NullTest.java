package org.example;

import org.checkerframework.checker.nullness.qual.NonNull;

public class NullTest {

    public String generateGreeting(@NonNull String name) {
        if (name.isEmpty()) {
            return "Hello, stranger!";
        }
        return "Hello, " + name;
    }

    public static void main(String[] args) {
        NullTest test = new NullTest();
        String greeting = test.generateGreeting(null);
        System.out.println(greeting);
    }
}
