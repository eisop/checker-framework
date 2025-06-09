import org.checkerframework.framework.testchecker.h1h2checker.quals.*;

public class Issue412 {

    @H1Bot Issue412() {
        new Inner();
    }

    class Inner {
        Inner(@H1Top Issue412 Issue412.this) {
            @H1Bot Issue412 object = Issue412.this;
        }
    }

    public static void main(String[] args) {
        new Issue412();
    }
}
