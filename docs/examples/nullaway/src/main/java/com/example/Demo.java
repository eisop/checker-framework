package com.example;

public class Demo {

    public String generateGreeting(String name) {
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
