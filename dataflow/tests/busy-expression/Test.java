class Test {
    public int test() {
        int a = 2, b = 3;
        int x, y;
        if (a != b) {
            x = b >> a;
            y = a - b;
        } else {
            y = b >> a;
            a = 0;
            x = a - b;
        }

        // test exceptional exit block
        int d;
        try {
            d = y / x;
        } catch (ArithmeticException e) {
            d = 10000000;
        }
        return d;
    }
}
