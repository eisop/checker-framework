// TODO: remove once this checker runs `all-systems` tests.
public class RawtypeCrash<V> {
    // :: error: (type.argument.type.incompatible)
    RawtypeCrash<? extends V> nullObj = null;
    // :: error: (assignment.type.incompatible)
    Object obj = ((RawtypeCrash) null).nullObj;
}
