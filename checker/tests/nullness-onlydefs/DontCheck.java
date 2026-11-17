public class DontCheck {

    interface Result {
        Object get();
    }

    static Object member() {
        return null;
    }

    static Result anonymous() {
        return new Result() {
            public Object get() {
                return null;
            }
        };
    }

    static Result local() {
        class Local implements Result {
            public Object get() {
                return null;
            }
        }
        return new Local();
    }
}
