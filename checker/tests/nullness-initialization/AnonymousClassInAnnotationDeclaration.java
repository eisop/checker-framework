// An anonymous class in the initializer of a constant of an annotation declaration is analyzed and
// checked like any other class, though it is not a member of the annotation declaration.

@interface AnonymousClassInAnnotationDeclaration {
    Object CONSTANT =
            new Object() {
                // :: error: (initialization.field.uninitialized)
                Object f;

                Object g;

                {
                    g = new Object();
                }

                @Override
                public String toString() {
                    return f.toString() + g.toString();
                }
            };
}
