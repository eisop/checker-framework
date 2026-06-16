// import org.checkerframework.checker.pico.qual.Mutable;
// import org.checkerframework.checker.pico.qual.PolyMutable;
//
// import java.util.List;
//
// public class PolyMutableWithArgTest {
//
//    public static @PolyMutable("elt") Object @PolyMutable("container") [] identity(
//            @PolyMutable("elt") Object @PolyMutable("container") [] seq) {
//        return seq;
//    }
//
//    void testIdentity(
//            @Mutable Object[] arr1,
//            @Mutable Object @Mutable [] arr2,
//            Object[] arr3,
//            Object @Mutable [] arr4) {
//        arr1 = identity(arr1);
//        arr2 = identity(arr2);
//        arr3 = identity(arr3);
//        arr4 = identity(arr4);
//    }
//
//    @SuppressWarnings("return.type.incompatible")
//    public static <E> @PolyMutable("elt") Object @PolyMutable("container") [] toArray(
//            @PolyMutable("container") List<@PolyMutable("elt") E> list) {
//        return null;
//    }
// }
