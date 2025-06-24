// Minimized test case from
//
// https://github.com/eisop/guava/blob/290cfe5c926de28cfdda491535901b09bab90ef9/guava/src/com/google/common/reflect/TypeToken.java#L1228
// which failed in https://github.com/eisop/checker-framework/pull/1066 with:
//
// guava/guava/src/com/google/common/reflect/TypeToken.java:[1228,29] error: [[value,
// allcheckers]:return.type.incompatible] incompatible types in return.
//   type of expression: @UnknownVal TypeToken<capture#07[ extends capture#08[ extends T[ extends
// @UnknownVal Object super @UnknownVal Void] super @BottomVal Void] super @BottomVal Void]>
//   method return type: @UnknownVal TypeToken<?[ extends T[ extends @UnknownVal Object super
// @BottomVal Void] super @BottomVal Void]>

abstract class WildcardInReturn<T> {

    abstract WildcardInReturn<?> of(String key);

    abstract WildcardInReturn<? extends T> getSubtype(Class<?> subclass);

    WildcardInReturn<? extends T> getSubtypeFromLowerBounds(Class<?> subclass, String key) {
        @SuppressWarnings("unchecked") // T's lower bound is <? extends T>
        WildcardInReturn<? extends T> bound = (WildcardInReturn<? extends T>) of(key);
        // Java supports only one lowerbound anyway.
        return bound.getSubtype(subclass);
    }
}
