// Test case for Issue 1098:
// https://github.com/typetools/checker-framework/issues/1098

@SuppressWarnings({"nullness", "initialization.fields.uninitialized"})
class MyObject {
    Class<?> getMyClass() {
        return null;
    }
}

class Issue1098NoJdk {
    <T> void cls2(Class<T> p1, T p2) {}

    void use2(MyObject ths) {
        cls2(ths.getMyClass(), null);
    }
}
