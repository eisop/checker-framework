class Test {
    public void test(int input) {
        int x, a, b, output;
        x = input;
        a = x-1;
        b = x-2;
        if (x > 0) {
            output = a*b-x;
            x = x-1;
        }
        output = a*b;
    }
}
