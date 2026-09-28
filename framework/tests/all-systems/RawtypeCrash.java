@SuppressWarnings("all") // Raw type use is intentional: this is a no-crash regression test.
public class RawtypeCrash<V> {
    RawtypeCrash<? extends V> nullObj = null;
    Object obj = ((RawtypeCrash) null).nullObj;
}
