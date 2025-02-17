package org.checkerframework.framework.util.element;

import com.sun.tools.javac.code.Attribute;
import com.sun.tools.javac.code.Attribute.TypeCompound;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.TargetType;
import com.sun.tools.javac.code.TypeAnnotationPosition;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.lang.model.element.Element;
import org.checkerframework.framework.type.AnnotatedTypeFactory;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedExecutableType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedTypeVariable;
import org.checkerframework.framework.util.element.ElementAnnotationUtil.UnexpectedAnnotationLocationException;
import org.checkerframework.javacutil.BugInCF;
import org.checkerframework.javacutil.ElementUtils;
import org.plumelib.util.StringsPlume;

/**
 * Adds annotations from element to the return type, formal parameter types, type parameters, and
 * throws clauses of the AnnotatedExecutableType type.
 */
public class MethodApplier extends TargetedElementAnnotationApplier {

  /**
   * Apply annotations from {@code element} to {@code type}.
   *
   * @param type the type to annotate
   * @param element the corresponding element
   * @param atypeFactory the type factory
   * @throws UnexpectedAnnotationLocationException if there is trouble
   */
  public static void apply(
      AnnotatedTypeMirror type, Element element, AnnotatedTypeFactory atypeFactory)
      throws UnexpectedAnnotationLocationException {
    new MethodApplier(type, element, atypeFactory).extractAndApply();
  }

  /**
   * Returns true if typeMirror represents an {@link AnnotatedExecutableType} and element represents
   * a {@link Symbol.MethodSymbol}.
   *
   * @param typeMirror the type to test
   * @param element the corresponding element
   * @return true if the MethodApplier accepts the type and element
   */
  public static boolean accepts(AnnotatedTypeMirror typeMirror, Element element) {
    return element instanceof Symbol.MethodSymbol && typeMirror instanceof AnnotatedExecutableType;
  }

  /** The type factory. */
  private final AnnotatedTypeFactory atypeFactory;

  /** Method being annotated, this symbol contains all relevant annotations. */
  private final Symbol.MethodSymbol methodSymbol;

  /** Method being annotated. */
  private final AnnotatedExecutableType methodType;

  /**
   * Constructor.
   *
   * @param type the type to annotate
   * @param element the corresponding element
   * @param atypeFactory the type factory
   */
  /*package-private*/ MethodApplier(
      AnnotatedTypeMirror type, Element element, AnnotatedTypeFactory atypeFactory) {
    super(type, element);
    this.atypeFactory = atypeFactory;
    this.methodSymbol = (Symbol.MethodSymbol) element;
    this.methodType = (AnnotatedExecutableType) type;
  }

  /** The annotated targets. */
  private static final TargetType[] annotatedTargets =
      new TargetType[] {TargetType.METHOD_RECEIVER, TargetType.METHOD_RETURN, TargetType.THROWS};

  /**
   * Returns receiver, returns, and throws. See extract and apply as we also annotate type params.
   *
   * @return receiver, returns, and throws
   */
  @Override
  protected TargetType[] annotatedTargets() {
    return annotatedTargets;
  }

  /** The valid targets. */
  private static final TargetType[] validTargets =
      new TargetType[] {
        TargetType.LOCAL_VARIABLE,
        TargetType.RESOURCE_VARIABLE,
        TargetType.EXCEPTION_PARAMETER,
        TargetType.NEW,
        TargetType.CAST,
        TargetType.INSTANCEOF,
        TargetType.METHOD_INVOCATION_TYPE_ARGUMENT,
        TargetType.CONSTRUCTOR_INVOCATION_TYPE_ARGUMENT,
        TargetType.METHOD_REFERENCE,
        TargetType.CONSTRUCTOR_REFERENCE,
        TargetType.METHOD_REFERENCE_TYPE_ARGUMENT,
        TargetType.CONSTRUCTOR_REFERENCE_TYPE_ARGUMENT,
        TargetType.METHOD_TYPE_PARAMETER,
        TargetType.METHOD_TYPE_PARAMETER_BOUND,
        TargetType.METHOD_FORMAL_PARAMETER,
        // TODO: from generic anonymous classes; remove when
        // we can depend on only seeing classfiles that were
        // generated by a javac that contains a fix for:
        // https://bugs.openjdk.org/browse/JDK-8198945
        TargetType.CLASS_EXTENDS,
        // TODO: Test case from Issue 3277 produces invalid position.
        // Ignore until this javac bug is fixed:
        // https://bugs.openjdk.org/browse/JDK-8233945
        TargetType.UNKNOWN,
        // Annotations on parameters to record constructors are marked as fields.
        TargetType.FIELD
      };

  /**
   * Returns all possible annotation positions for a method except those in annotatedTargets.
   *
   * @return all possible annotation positions for a method except those in annotatedTargets
   */
  @Override
  protected TargetType[] validTargets() {
    return validTargets;
  }

  /**
   * Returns the annotations on the method symbol (element).
   *
   * @return the annotations on the method symbol (element)
   */
  @Override
  protected Iterable<Attribute.TypeCompound> getRawTypeAttributes() {
    return methodSymbol.getRawTypeAttributes();
  }

  @Override
  protected boolean isAccepted() {
    return MethodApplier.accepts(type, element);
  }

  /**
   * Sets the method's element, annotates its return type, parameters, type parameters, and throws
   * annotations.
   */
  @Override
  public void extractAndApply() throws UnexpectedAnnotationLocationException {
    methodType.setElement(methodSymbol); // Preserves previous behavior

    // Add declaration annotations to the return type if
    if (methodType.getReturnType() instanceof AnnotatedTypeVariable) {
      applyTypeVarUseOnReturnType();
    }
    ElementAnnotationUtil.addDeclarationAnnotationsFromElement(
        methodType.getReturnType(), methodSymbol.getAnnotationMirrors());

    List<AnnotatedTypeMirror> params = methodType.getParameterTypes();
    for (int i = 0; i < params.size(); ++i) {
      // Add declaration annotations to the parameter type
      ElementAnnotationUtil.addDeclarationAnnotationsFromElement(
          params.get(i), methodSymbol.getParameters().get(i).getAnnotationMirrors());
    }

    // ensures that we check that there are only valid target types on this class, there are no
    // "invalid" locations
    super.extractAndApply();

    ElementAnnotationUtil.applyAllElementAnnotations(
        methodType.getParameterTypes(), methodSymbol.getParameters(), atypeFactory);
    ElementAnnotationUtil.applyAllElementAnnotations(
        methodType.getTypeVariables(), methodSymbol.getTypeParameters(), atypeFactory);
  }

  // NOTE that these are the only locations not handled elsewhere, otherwise we call apply
  @Override
  protected void handleTargeted(List<TypeCompound> targeted)
      throws UnexpectedAnnotationLocationException {
    List<TypeCompound> unmatched = new ArrayList<>();
    Map<TargetType, List<TypeCompound>> targetTypeToAnno =
        ElementAnnotationUtil.partitionByTargetType(
            targeted,
            unmatched,
            TargetType.METHOD_RECEIVER,
            TargetType.METHOD_RETURN,
            TargetType.THROWS);

    ElementAnnotationUtil.annotateViaTypeAnnoPosition(
        methodType.getReceiverType(), targetTypeToAnno.get(TargetType.METHOD_RECEIVER));
    ElementAnnotationUtil.annotateViaTypeAnnoPosition(
        methodType.getReturnType(), targetTypeToAnno.get(TargetType.METHOD_RETURN));
    applyThrowsAnnotations(targetTypeToAnno.get(TargetType.THROWS));

    if (!unmatched.isEmpty()) {
      throw new BugInCF(
          "Unexpected annotations ( "
              + StringsPlume.join(",", unmatched)
              + " ) for"
              + "type ( "
              + type
              + " ) and element ( "
              + element
              + " ) ");
    }
  }

  /** For each thrown type, collect all the annotations for that type and apply them. */
  private void applyThrowsAnnotations(List<Attribute.TypeCompound> annos)
      throws UnexpectedAnnotationLocationException {
    List<AnnotatedTypeMirror> thrown = methodType.getThrownTypes();
    if (thrown.isEmpty()) {
      return;
    }

    Map<AnnotatedTypeMirror, List<TypeCompound>> typeToAnnos = new LinkedHashMap<>();
    for (AnnotatedTypeMirror thrownType : thrown) {
      typeToAnnos.put(thrownType, new ArrayList<>());
    }

    for (TypeCompound anno : annos) {
      TypeAnnotationPosition annoPos = anno.position;
      if (annoPos.type_index >= 0 && annoPos.type_index < thrown.size()) {
        AnnotatedTypeMirror thrownType = thrown.get(annoPos.type_index);
        typeToAnnos.get(thrownType).add(anno);
      } else {
        throw new BugInCF(
            "MethodApplier.applyThrowsAnnotation: "
                + "invalid throws index "
                + annoPos.type_index
                + " for annotation: "
                + anno
                + " for element: "
                + ElementUtils.getQualifiedName(element));
      }
    }

    for (Map.Entry<AnnotatedTypeMirror, List<TypeCompound>> typeToAnno : typeToAnnos.entrySet()) {
      ElementAnnotationUtil.annotateViaTypeAnnoPosition(typeToAnno.getKey(), typeToAnno.getValue());
    }
  }

  /**
   * If the return type is a use of a type variable first apply the bound annotations from the type
   * variables declaration.
   */
  private void applyTypeVarUseOnReturnType() throws UnexpectedAnnotationLocationException {
    new TypeVarUseApplier(methodType.getReturnType(), methodSymbol, atypeFactory).extractAndApply();
  }
}
