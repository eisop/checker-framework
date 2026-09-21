@interface Issue612Min {
    class D {
        D() {
            g(new Object());
        }

        static void g(Object... xs) {}
    }
}
