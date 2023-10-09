package org.checkerframework.checker.calledmethods.builder;

import com.sun.source.tree.NewClassTree;
import com.sun.source.tree.VariableTree;
import java.beans.Introspector;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Name;
import javax.lang.model.element.TypeElement;
import org.checkerframework.checker.calledmethods.CalledMethodsAnnotatedTypeFactory;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedExecutableType;
import org.checkerframework.javacutil.AnnotationUtils;
import org.checkerframework.javacutil.ElementUtils;

/**
 * Lombok support for the Called Methods Checker. This class adds CalledMethods annotations to the
 * code generated by Lombok.
 */
public class LombokSupport implements BuilderFrameworkSupport {

  /** The type factory. */
  private final CalledMethodsAnnotatedTypeFactory atypeFactory;

  /**
   * Create a new LombokSupport.
   *
   * @param atypeFactory the typechecker's type factory
   */
  public LombokSupport(CalledMethodsAnnotatedTypeFactory atypeFactory) {
    this.atypeFactory = atypeFactory;
  }

  // The list is copied from lombok.core.handlers.HandlerUtil. The list cannot be used from that
  // class directly because Lombok does not provide class files for its own implementation, to
  // prevent itself from being accidentally added to clients' compile classpaths. This design
  // decision means that it is impossible to depend directly on Lombok internals.
  /** The list of annotations that Lombok treats as non-null. */
  public static final List<String> NONNULL_ANNOTATIONS =
      Collections.unmodifiableList(
          Arrays.asList(
              "android.annotation.NonNull",
              "android.support.annotation.NonNull",
              "com.sun.istack.internal.NotNull",
              "edu.umd.cs.findbugs.annotations.NonNull",
              "javax.annotation.Nonnull",
              // "javax.validation.constraints.NotNull", // The field might contain a
              // null value until it is persisted.
              "lombok.NonNull",
              "org.checkerframework.checker.nullness.qual.NonNull",
              "org.eclipse.jdt.annotation.NonNull",
              "org.eclipse.jgit.annotations.NonNull",
              "org.jetbrains.annotations.NotNull",
              "org.jmlspecs.annotation.NonNull",
              "org.netbeans.api.annotations.common.NonNull",
              "org.springframework.lang.NonNull"));

  /**
   * A map from elements that have a lombok.Builder.Default annotation to the simple property name
   * that should be treated as defaulted.
   *
   * <p>This cache is kept because the usual method for checking that an element has been defaulted
   * (calling declarationFromElement and examining the resulting VariableTree) only works if a
   * corresponding Tree is available (for code that is only available as bytecode, no such Tree is
   * available and that method returns null). See the code in {@link
   * #getLombokRequiredProperties(Element)} that handles fields.
   */
  private final Map<Element, Name> defaultedElements = new HashMap<>(2);

  @Override
  public boolean isBuilderBuildMethod(ExecutableElement candidateBuildElement) {
    TypeElement candidateGeneratedBuilderElement =
        (TypeElement) candidateBuildElement.getEnclosingElement();

    if ((ElementUtils.hasAnnotation(candidateGeneratedBuilderElement, "lombok.Generated")
            || ElementUtils.hasAnnotation(candidateBuildElement, "lombok.Generated"))
        && candidateGeneratedBuilderElement.getSimpleName().toString().endsWith("Builder")) {
      return candidateBuildElement.getSimpleName().contentEquals("build");
    }
    return false;
  }

  @Override
  public void handleBuilderBuildMethod(AnnotatedExecutableType builderBuildType) {
    ExecutableElement buildElement = builderBuildType.getElement();

    TypeElement generatedBuilderElement = (TypeElement) buildElement.getEnclosingElement();
    // The class with the @lombok.Builder annotation...
    Element annotatedWithBuilderElement = generatedBuilderElement.getEnclosingElement();

    List<String> requiredProperties = getLombokRequiredProperties(annotatedWithBuilderElement);
    AnnotationMirror newCalledMethodsAnno =
        atypeFactory.createAccumulatorAnnotation(requiredProperties);
    builderBuildType.getReceiverType().addAnnotation(newCalledMethodsAnno);
  }

  @Override
  public boolean isToBuilderMethod(ExecutableElement candidateToBuilderElement) {
    return candidateToBuilderElement.getSimpleName().contentEquals("toBuilder")
        && (ElementUtils.hasAnnotation(candidateToBuilderElement, "lombok.Generated")
            || ElementUtils.hasAnnotation(
                candidateToBuilderElement.getEnclosingElement(), "lombok.Generated"));
  }

  @Override
  public void handleToBuilderMethod(AnnotatedExecutableType toBuilderType) {
    AnnotatedTypeMirror returnType = toBuilderType.getReturnType();
    ExecutableElement buildElement = toBuilderType.getElement();
    TypeElement generatedBuilderElement = (TypeElement) buildElement.getEnclosingElement();
    handleToBuilderType(returnType, generatedBuilderElement);
  }

  /**
   * Add, to a type, a CalledMethods annotation that states that all required setters have been
   * called. The type can be the return type of toBuilder or of the corresponding generated "copy"
   * constructor.
   *
   * @param type type to update
   * @param classElement corresponding AutoValue class
   */
  private void handleToBuilderType(AnnotatedTypeMirror type, Element classElement) {
    List<String> requiredProperties = getLombokRequiredProperties(classElement);
    AnnotationMirror calledMethodsAnno =
        atypeFactory.createAccumulatorAnnotation(requiredProperties);
    type.replaceAnnotation(calledMethodsAnno);
  }

  /**
   * Computes the required properties of a @lombok.Builder class, i.e., the names of the fields
   * with @lombok.NonNull annotations.
   *
   * @param lombokClassElement the class with the @lombok.Builder annotation
   * @return a list of required property names
   */
  private List<String> getLombokRequiredProperties(Element lombokClassElement) {
    List<String> requiredPropertyNames = new ArrayList<>();
    List<String> defaultedPropertyNames = new ArrayList<>();
    for (Element member : lombokClassElement.getEnclosedElements()) {
      if (member.getKind() == ElementKind.FIELD) {
        // Lombok never generates non-null fields with initializers in builders, unless the
        // field is annotated with @Default or @Singular, which are handled elsewhere.  So,
        // this code doesn't need to consider whether the field has or does not have
        // initializers.
        for (AnnotationMirror anm :
            atypeFactory.getElementUtils().getAllAnnotationMirrors(member)) {
          if (NONNULL_ANNOTATIONS.contains(AnnotationUtils.annotationName(anm))) {
            requiredPropertyNames.add(member.getSimpleName().toString());
          }
        }
      } else if (member.getKind() == ElementKind.METHOD
          && ElementUtils.hasAnnotation(member, "lombok.Generated")) {
        String methodName = member.getSimpleName().toString();
        // If a field foo has an @Builder.Default annotation, Lombok always generates a
        // method called $default$foo.
        if (methodName.startsWith("$default$")) {
          String propName = methodName.substring(9); // $default$ has 9 characters
          defaultedPropertyNames.add(propName);
        }
      } else if (member.getKind().isClass() && member.toString().endsWith("Builder")) {
        // Note that the test above will fail to catch builders generated by Lombok that
        // have custom names using the builderClassName attribute. TODO: find a way to
        // handle such builders too.

        // If a field bar has an @Singular annotation, Lombok always generates a method
        // called clearBar in the builder class itself. Therefore, search the builder for
        // such a method, and extract the appropriate property name to treat as defaulted.
        for (Element builderMember : member.getEnclosedElements()) {
          if (builderMember.getKind() == ElementKind.METHOD
              && ElementUtils.hasAnnotation(builderMember, "lombok.Generated")) {
            String methodName = builderMember.getSimpleName().toString();
            if (methodName.startsWith("clear")) {
              String propName =
                  Introspector.decapitalize(methodName.substring(5)); // clear has 5 characters
              defaultedPropertyNames.add(propName);
            }
          } else if (builderMember.getKind() == ElementKind.FIELD) {
            VariableTree variableTree =
                (VariableTree) atypeFactory.declarationFromElement(builderMember);
            if (variableTree != null && variableTree.getInitializer() != null) {
              Name propName = variableTree.getName();
              defaultedPropertyNames.add(propName.toString());
              defaultedElements.put(builderMember, propName);
            } else if (defaultedElements.containsKey(builderMember)) {
              defaultedPropertyNames.add(defaultedElements.get(builderMember).toString());
            }
          }
        }
      }
    }
    requiredPropertyNames.removeAll(defaultedPropertyNames);
    return requiredPropertyNames;
  }

  @Override
  public void handleConstructor(NewClassTree tree, AnnotatedTypeMirror type) {}
}
