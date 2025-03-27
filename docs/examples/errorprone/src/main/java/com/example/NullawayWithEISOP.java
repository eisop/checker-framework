package com.example;

import org.checkerframework.checker.nullness.qual.NonNull;

public class NullawayWithEISOP {

    public String generateGreeting(@NonNull String name) {
        if (name.isEmpty()) {
            return "Hello, stranger!";
        }
        return "Hello, " + name;
    }

    public static void main(String[] args) {
        NullawayWithEISOP test = new NullawayWithEISOP();
        String greeting = test.generateGreeting(null);
        System.out.println(greeting);
    }
}
