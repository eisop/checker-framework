// The test cases GenericNull, FieldAccessTest, and InferTypeArgs often fail together.
// See the comments at GenericNull for some tips about what might be wrong.

/**
 * This test came from running the compilermsgs checker in the checker-framework/checker directory
 * It's to test the result of type argument inference. We used to have the following
 * "return.type.incompatible" found: FlowAnalysis [ extends @UnknownPropertyKey
 * CFAbstractAnalysis<Value [ extends @UnknownPropertyKey CFAbstractValue<Value [
 * extends @UnknownPropertyKey CFAbstractValue<Value> super @UnknownPropertyKey NullType ]>
 * super @UnknownPropertyKey NullType ], Store [ extends @UnknownPropertyKey CFAbstractStore<Value[
 * extends @UnknownPropertyKey CFAbstractValue<Value [ extends @UnknownPropertyKey
 * CFAbstractValue<Value> super @UnknownPropertyKey NullType ]> super @UnknownPropertyKey NullType
 * ], Store[ extends @UnknownPropertyKey CFAbstractStore<Value [ extends @UnknownPropertyKey
 * CFAbstractValue<Value [ extends @UnknownPropertyKey CFAbstractValue<Value>
 * super @UnknownPropertyKey NullType ]> super @UnknownPropertyKey NullType ], Store>
 * super @UnknownPropertyKey NullType ]> super @UnknownPropertyKey NullType ], TransferFunction[
 * extends @UnknownPropertyKey CFAbstractTransfer<Value [ extends @UnknownPropertyKey
 * CFAbstractValue<Value [ extends @UnknownPropertyKey CFAbstractValue<Value>
 * super @UnknownPropertyKey NullType ]> super @UnknownPropertyKey NullType ], Store[
 * extends @UnknownPropertyKey CFAbstractStore<Value [ extends @UnknownPropertyKey
 * CFAbstractValue<Value [ extends @UnknownPropertyKey CFAbstractValue<Value>
 * super @UnknownPropertyKey NullType ]> super @UnknownPropertyKey NullType ], Store[
 * extends @UnknownPropertyKey CFAbstractStore<Value [ extends @UnknownPropertyKey
 * CFAbstractValue<Value [ extends @UnknownPropertyKey CFAbstractValue<Value>
 * super @UnknownPropertyKey NullType ]> super @UnknownPropertyKey NullType ], Store>
 * super @UnknownPropertyKey NullType ]> super @UnknownPropertyKey NullType ], TransferFunction[
 * extends @UnknownPropertyKey CFAbstractTransfer<Value [ extends @UnknownPropertyKey
 * CFAbstractValue<Value [ extends @UnknownPropertyKey CFAbstractValue<Value>
 * super @UnknownPropertyKey NullType ]> super @UnknownPropertyKey NullType ], Store[
 * extends @UnknownPropertyKey CFAbstractStore<Value [ extends @UnknownPropertyKey
 * CFAbstractValue<Value [ extends @UnknownPropertyKey CFAbstractValue<Value>
 * super @UnknownPropertyKey NullType ]> super @UnknownPropertyKey NullType ], Store[
 * extends @UnknownPropertyKey CFAbstractStore<Value [ extends @UnknownPropertyKey
 * CFAbstractValue<Value [ extends @UnknownPropertyKey CFAbstractValue<Value>
 * super @UnknownPropertyKey NullType ]> super @UnknownPropertyKey NullType ], Store>
 * super @UnknownPropertyKey NullType ]> super @UnknownPropertyKey NullType ], TransferFunction>
 * super @UnknownPropertyKey NullType ]> super @UnknownPropertyKey NullType ]>
 * super @UnknownPropertyKey NullType ] required: FlowAnalysis [ extends @UnknownPropertyKey
 * CFAbstractAnalysis<Value[ extends @UnknownPropertyKey CFAbstractValue<Value [
 * extends @UnknownPropertyKey CFAbstractValue<Value> super @Bottom NullType ]> super @Bottom
 * NullType ], Store[ extends @UnknownPropertyKey CFAbstractStore<Value [
 * extends @UnknownPropertyKey CFAbstractValue<Value [ extends @UnknownPropertyKey
 * CFAbstractValue<Value> super @Bottom NullType ]> super @Bottom NullType ], Store [
 * extends @UnknownPropertyKey CFAbstractStore<Value[ extends @UnknownPropertyKey
 * CFAbstractValue<Value [ extends @UnknownPropertyKey CFAbstractValue<Value> super @Bottom NullType
 * ]> super @Bottom NullType ], Store> super @Bottom NullType ]> super @Bottom NullType ],
 * TransferFunction [ extends @UnknownPropertyKey CFAbstractTransfer<Value[
 * extends @UnknownPropertyKey CFAbstractValue<Value [ extends @UnknownPropertyKey
 * CFAbstractValue<Value> super @Bottom NullType ]> super @Bottom NullType ], Store[
 * extends @UnknownPropertyKey CFAbstractStore<Value [ extends @UnknownPropertyKey
 * CFAbstractValue<Value [ extends @UnknownPropertyKey CFAbstractValue<Value> super @Bottom NullType
 * ]> super @Bottom NullType ], Store [ extends @UnknownPropertyKey CFAbstractStore<Value[
 * extends @UnknownPropertyKey CFAbstractValue<Value [ extends @UnknownPropertyKey
 * CFAbstractValue<Value> super @Bottom NullType ]> super @Bottom NullType ], Store> super @Bottom
 * NullType ]> super @Bottom NullType ], TransferFunction [ extends @UnknownPropertyKey
 * CFAbstractTransfer<Value[ extends @UnknownPropertyKey CFAbstractValue<Value [
 * extends @UnknownPropertyKey CFAbstractValue<Value> super @Bottom NullType ]> super @Bottom
 * NullType ], Store[ extends @UnknownPropertyKey CFAbstractStore<Value [
 * extends @UnknownPropertyKey CFAbstractValue<Value [ extends @UnknownPropertyKey
 * CFAbstractValue<Value> super @Bottom NullType ]> super @Bottom NullType ], Store [
 * extends @UnknownPropertyKey CFAbstractStore<Value[ extends @UnknownPropertyKey
 * CFAbstractValue<Value [ extends @UnknownPropertyKey CFAbstractValue<Value> super @Bottom NullType
 * ]> super @Bottom NullType ], Store> super @Bottom NullType ]> super @Bottom NullType ],
 * TransferFunction> super @Bottom NullType ]> super @Bottom NullType ]> super @Bottom NullType ]
 */
class CFAbstractValue<V extends CFAbstractValue<V>> {}

class CFAbstractAnalysis<V extends CFAbstractValue<V>> {}

class GenericAnnotatedTypeFactoryInferTypeArgs<
        Value extends CFAbstractValue<Value>, FlowAnalysis extends CFAbstractAnalysis<Value>> {

    @SuppressWarnings("immutability:type.argument.type.incompatible")
    protected FlowAnalysis createFlowAnalysis() {
        FlowAnalysis result = invokeConstructorFor();
        return result;
    }

    @SuppressWarnings({
        "nullness:return.type.incompatible",
        "lock:return.type.incompatible",
        "immutabilitysub:type.argument.type.incompatible"
    })
    public static <T> T invokeConstructorFor() {
        return null;
    }
}
