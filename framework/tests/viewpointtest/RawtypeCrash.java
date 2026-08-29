// TODO: remove once this checker runs `all-systems` tests.
public class RawtypeCrash<V> {
    RawtypeCrash<? extends V> nullObj = null;
    Object obj = ((RawtypeCrash) null).nullObj;
}
