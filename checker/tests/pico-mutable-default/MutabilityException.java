public class MutabilityException {
    void throwException() {
        try {
        } catch (Exception e) {
            throw new java.lang.Error(e);
        }
    }
}
