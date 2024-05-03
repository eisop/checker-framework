public class CloneProblem {
    public boolean removeAll(CloneProblem other) throws CloneNotSupportedException {
        if (true) {
            CloneProblem l = (CloneProblem) other.overridenClone();
            // Gets method invocation invalid error
            l.foo();
        }
        return true;
    }

    // This return @Mutable Object
    public Object overridenClone() {
        return null;
    }

    void foo() {}
}
