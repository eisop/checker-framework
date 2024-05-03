import java.math.BigDecimal;
import java.util.Date;

/**
 * Computed annotations are applied in thir order: TreeAnnotator(with order inside also) - eg. Takes
 * care of annotations according to ast locations, for example, fields TypeAnnotator(with order
 * inside also) - eg. ImplicitFor using typeNames; changing method signatures according to method
 * name Defaults - eg. @DefaultFor using TypeUseLocation. Usually doesn't change previous result.
 * But May mutate result from above sometimes. For example, if we apply top annotation to type
 * variable used on local variable if we don't override getShouldDefaultTypeVarLocals() to false
 * Dataflow refinement
 */
public class AnnotationApplicationOrder {
    static Object o; // PICOTreeAnnotator takes care of static fields
    // :: error: (initialization.field.uninitialized)
    BigDecimal decimal; // PICOImplicitsTypeAnnotator takes care of it
    Date date; // QualifierDefaults takes care of it
}
