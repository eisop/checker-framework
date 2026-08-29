import viewpointtest.quals.*;

@SuppressWarnings({"super.invocation.invalid", "inconsistent.constructor.type"})
public class AnnotationOnSupertype {

    static class SuperClass {}

    interface SuperInterface {}

    static class GenericSuperClass<T> {}

    @A static class AnnotatedExtends
            // :: error: (annotation.on.supertype)
            extends @A SuperClass {}

    @A static class AnnotatedImplements
            // :: error: (annotation.on.supertype)
            implements @A SuperInterface {}

    // An annotation on a type argument of the supertype is allowed.
    @A static class AnnotatedTypeArgument extends GenericSuperClass<@A Object> {}
}
