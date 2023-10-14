@SuppressWarnings("initialization")
public class Issue1590 {

    private String a;

    public Issue1590() {
        // valid because of suppressed warnings
        init();
        // :: error: (dereference.of.nullable)
        a.length();
    }

    public void init() {
        a = "gude";
    }
}
