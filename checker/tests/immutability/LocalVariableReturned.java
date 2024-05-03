public class LocalVariableReturned {

    LocalVariableReturned foo(int a, int b) {
        LocalVariableReturned t = null;
        if (a == b) {
            t = new LocalVariableReturned();
        }
        return t;
    }
}
