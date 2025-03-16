/* TODO: minimize the failure from
 * checker/tests/index-initializedfields/RequireJavadoc.java
 * and make this an independent, minimal reproduction.
 */
class Simple {
    private Simple() {}

    public static void main(String[] args) {
        Simple s = new Simple();
    }
}
