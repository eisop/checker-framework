package com.example;

import org.checkerframework.checker.nullness.qual.NonNull;

public class Demo {

    public String generateGreeting(@NonNull String name) {
        if (name.isEmpty()) {
            return "Hello, World!";
        }
        return "Hello, " + name;
    }

    public static void main(String[] args) {
        Demo test = new Demo();
        String greeting = test.generateGreeting(null);
        System.out.println(greeting);
    }
}
