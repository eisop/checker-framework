package edu.cmu.cs.glacier.tests;

public class Arrays {
    int[] intArray;

    public Arrays() {}

    public int[] getData() {
        return intArray;
    }

    public byte[] getByteData() {
        return new byte[0];
    }

    public void setData() {
        intArray[0] = 42;
    }
}
