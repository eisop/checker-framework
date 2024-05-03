public class LocalVariableTypeVariable<N extends Number> {
    void foo() {
        // I disable applying local variable defaults to type variable if the type variable is used
        // on local variable
        N f;
    }
}
