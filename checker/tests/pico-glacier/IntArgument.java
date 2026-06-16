package edu.cmu.cs.glacier.tests;

public class IntArgument {
    public void takeInt(int x) {}

    public void useInt() {
        for (int i = 0; i < 10; i++) {
            takeInt(i);
        }
    }
}
