import viewpointtest.quals.A;
import viewpointtest.quals.B;
import viewpointtest.quals.ReceiverDependentQual;

@SuppressWarnings({
    "inconsistent.constructor.type",
    "super.invocation.invalid",
    "cast.unsafe.constructor.invocation"
})
public class SubclassFieldInheritance {

    static class GenericBox<T> {}

    @ReceiverDependentQual static class SuperClass {
        @ReceiverDependentQual Object inheritedField;

        @ReceiverDependentQual GenericBox<@ReceiverDependentQual Object> inheritedGenericField;

        @ReceiverDependentQual Object @ReceiverDependentQual [] inheritedArrayField;

        @A Object fixedAField;

        @ReceiverDependentQual Object getField() {
            return inheritedField;
        }

        void setField(@ReceiverDependentQual Object o) {
            this.inheritedField = o;
        }
    }

    // :: error: (annotation.on.supertype)
    @A static class SubA extends @A SuperClass {
        @ReceiverDependentQual Object subFieldInit = new @A Object();

        // :: error: (assignment.type.incompatible)
        @ReceiverDependentQual Object badSubFieldInit = new @B Object();
    }

    // :: error: (annotation.on.supertype)
    @B static class SubB extends @B SuperClass {
        @ReceiverDependentQual Object subFieldInit = new @B Object();

        // :: error: (assignment.type.incompatible)
        @ReceiverDependentQual Object badSubFieldInit = new @A Object();
    }

    void testSubA(SubA a) {
        @A Object aObj = a.inheritedField;
        // :: error: (assignment.type.incompatible)
        @B Object badBObj = a.inheritedField;

        @A GenericBox<@A Object> aBox = a.inheritedGenericField;
        // :: error: (assignment.type.incompatible)
        @B GenericBox<@A Object> badBBox = a.inheritedGenericField;
        // :: error: (assignment.type.incompatible)
        @A GenericBox<@B Object> badABox = a.inheritedGenericField;

        @A Object @A [] aArray = a.inheritedArrayField;
        // :: error: (assignment.type.incompatible)
        @B Object @A [] badBArray = a.inheritedArrayField;
        // :: error: (assignment.type.incompatible)
        @A Object @B [] badAArray = a.inheritedArrayField;

        @A Object aFixed = a.fixedAField;
        // :: error: (assignment.type.incompatible)
        @B Object badBFixed = a.fixedAField;

        @A Object aMethod = a.getField();
        // :: error: (assignment.type.incompatible)
        @B Object badBMethod = a.getField();

        a.setField(new @A Object());
        // :: error: (argument.type.incompatible)
        a.setField(new @B Object());
    }

    void testSubB(SubB b) {
        @B Object bObj = b.inheritedField;
        // :: error: (assignment.type.incompatible)
        @A Object badAObj = b.inheritedField;

        @B GenericBox<@B Object> bBox = b.inheritedGenericField;
        // :: error: (assignment.type.incompatible)
        @A GenericBox<@B Object> badABox = b.inheritedGenericField;
        // :: error: (assignment.type.incompatible)
        @B GenericBox<@A Object> badBBox = b.inheritedGenericField;

        @B Object @B [] bArray = b.inheritedArrayField;
        // :: error: (assignment.type.incompatible)
        @A Object @B [] badAArray = b.inheritedArrayField;
        // :: error: (assignment.type.incompatible)
        @B Object @A [] badBArray = b.inheritedArrayField;

        // fixedAField remains @A Object even on @B receiver.
        @A Object aFixed = b.fixedAField;
        // :: error: (assignment.type.incompatible)
        @B Object badBFixed = b.fixedAField;

        @B Object bMethod = b.getField();
        // :: error: (assignment.type.incompatible)
        @A Object badAMethod = b.getField();

        b.setField(new @B Object());
        // :: error: (argument.type.incompatible)
        b.setField(new @A Object());
    }

    void testSubclassSuperAssignment() {
        @A SuperClass aSuper = new SubA();
        @B SuperClass bSuper = new SubB();

        // :: error: (assignment.type.incompatible)
        @B SuperClass badBSuper = new SubA();
        // :: error: (assignment.type.incompatible)
        @A SuperClass badASuper = new SubB();
    }
}
