interface Issue612A {}

@interface Issue612B {
    Class<? extends Issue612A> value();
}

class Issue612C implements Issue612A {}

class Issue612E {}

@interface Issue612Z {
    abstract class Issue612D implements Issue612A {

        public Issue612D() {
            g(new Issue612E());
        }

        static void g(Object... xs) {}
    }
}
