@interface Issue612Min {
    class D {
        D() {
            g(new Object());
        }

        void g(Object... xs) {}
    }
}
