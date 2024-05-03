// @skip-test TypeArgInferenceUtil#assignedTo() right now doesn't handle correctly
// the case where one method invocation is used as the actual receiver for another
// method invocation but the first method invocation is not directly called method,
// e.g. in a paranthesis.
public class PolyMutableBug {
    void foo(A a) {
        // Having parentheis here causes StackOverFlowError
        // It causes ((MemberSelectTree) methodInvocation.getMethodSelect()).getExpression()
        // in TypeArgInferenceUtil to return a ParenthesizedTree instead of MethodInvocationTree
        (a.subtract()).multiply();
    }
}

class A {
    A subtract() {
        return this;
    }

    A multiply() {
        return this;
    }
}
