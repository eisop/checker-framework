package edu.cmu.cs.glacier.tests;

public class IntReturn {
    public int getInt() {
        return 42;
    }

    public void useInt() {
        int x = getInt();
    }
}
