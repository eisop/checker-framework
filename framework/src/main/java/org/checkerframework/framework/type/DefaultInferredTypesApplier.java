package org.checkerframework.framework.type;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.TypeVariable;
import javax.lang.model.type.WildcardType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedTypeVariable;
import org.checkerframework.framework.util.AnnotatedTypes;
import org.checkerframework.javacutil.AnnotationMirrorSet;
import org.checkerframework.javacutil.BugInCF;
import org.checkerframework.javacutil.TypesUtils;

/** Utility class for applying the annotations inferred by dataflow to a given type. */
public class DefaultInferredTypesApplier {

  // At the moment, only Inference uses the omitSubtypingCheck option.
  // In actuality the subtyping check should be unnecessary since inferred
  // types should be subtypes of their declaration.
  private final boolean omitSubtypingCheck;

  private final QualifierHierarchy hierarchy;
  private final AnnotatedTypeFactory factory;

  public DefaultInferredTypesApplier(QualifierHierarchy hierarchy, AnnotatedTypeFactory factory) {
    this(false, hierarchy, factory);
  }

  public DefaultInferredTypesApplier(
      boolean omitSubtypingCheck, QualifierHierarchy hierarchy, AnnotatedTypeFactory factory) {
    this.omitSubtypingCheck = omitSubtypingCheck;
    this.hierarchy = hierarchy;
    this.factory = factory;
  }

  /**
   * For each top in qualifier hierarchy, traverse inferred and copy the required annotations over
   * to type.
   *
   * @param type the type to which annotations are being applied
   * @param inferredSet the type inferred by data flow
   * @param inferredTypeMirror underlying inferred type
   */
  public void applyInferredType(
      AnnotatedTypeMirror type, AnnotationMirrorSet inferredSet, TypeMirror inferredTypeMirror) {
    if (inferredSet == null) {
      return;
    }
    if (inferredTypeMirror.getKind() == TypeKind.WILDCARD) {
      // Dataflow might infer a wildcard that extends a type variable for types that are
      // actually type variables.  Use the type variable instead.
      while (inferredTypeMirror.getKind() == TypeKind.WILDCARD
          && (((WildcardType) inferredTypeMirror).getExtendsBound() != null)) {
        inferredTypeMirror = ((WildcardType) inferredTypeMirror).getExtendsBound();
      }
    }
    for (AnnotationMirror top : hierarchy.getTopAnnotations()) {
      AnnotationMirror inferred = hierarchy.findAnnotationInHierarchy(inferredSet, top);

      apply(type, inferred, inferredTypeMirror, top);
    }
  }

  private void apply(
      AnnotatedTypeMirror type,
      AnnotationMirror inferred,
      TypeMirror inferredTypeMirror,
      AnnotationMirror top) {
    AnnotationMirror primary = type.getAnnotationInHierarchy(top);
    if (inferred == null) {

      if (primary == null) {
        // Type doesn't have a primary either, nothing to remove
      } else if (type.getKind() == TypeKind.TYPEVAR) {
        removePrimaryAnnotationTypeVar(
            (AnnotatedTypeVariable) type, inferredTypeMirror, top, primary);
      } else {
        removePrimaryTypeVarApplyUpperBound(type, inferredTypeMirror, top, primary);
      }
    } else {
      if (primary == null) {
        AnnotationMirrorSet lowerbounds =
            AnnotatedTypes.findEffectiveLowerBoundAnnotations(hierarchy, type);
        primary = hierarchy.findAnnotationInHierarchy(lowerbounds, top);
      }
      if ((omitSubtypingCheck || hierarchy.isSubtype(inferred, primary))) {
        type.replaceAnnotation(inferred);
      }
    }
  }

  private void removePrimaryTypeVarApplyUpperBound(
      AnnotatedTypeMirror type,
      TypeMirror inferredTypeMirror,
      AnnotationMirror top,
      AnnotationMirror notInferred) {
    if (inferredTypeMirror.getKind() != TypeKind.TYPEVAR) {
      throw new BugInCF("Inferred value should not be missing annotations: " + inferredTypeMirror);
    }
    if (TypesUtils.isCapturedTypeVariable(inferredTypeMirror)) {
      return;
    }

    TypeVariable typeVar = (TypeVariable) inferredTypeMirror;
    AnnotatedTypeVariable typeVariableDecl =
        (AnnotatedTypeVariable) factory.getAnnotatedType(typeVar.asElement());
    AnnotationMirror upperBound = typeVariableDecl.getEffectiveAnnotationInHierarchy(top);

    if (omitSubtypingCheck || hierarchy.isSubtype(upperBound, notInferred)) {
      type.replaceAnnotation(upperBound);
    }
  }

  private void removePrimaryAnnotationTypeVar(
      AnnotatedTypeVariable annotatedTypeVariable,
      TypeMirror inferredTypeMirror,
      AnnotationMirror top,
      AnnotationMirror previousAnnotation) {
    if (inferredTypeMirror.getKind() != TypeKind.TYPEVAR) {
      throw new BugInCF("Missing annos");
    }
    TypeVariable typeVar = (TypeVariable) inferredTypeMirror;
    AnnotatedTypeVariable typeVariableDecl =
        (AnnotatedTypeVariable) factory.getAnnotatedType(typeVar.asElement());
    AnnotationMirror upperBound = typeVariableDecl.getEffectiveAnnotationInHierarchy(top);
    if (omitSubtypingCheck || hierarchy.isSubtype(upperBound, previousAnnotation)) {
      // TODO: clean up this method and whole class.
      AnnotationMirror ub = typeVariableDecl.getUpperBound().getAnnotationInHierarchy(top);
      AnnotationMirror lb = typeVariableDecl.getLowerBound().getAnnotationInHierarchy(top);
      AnnotatedTypeMirror atvUB = annotatedTypeVariable.getUpperBound();
      AnnotatedTypeMirror atvLB = annotatedTypeVariable.getLowerBound();
      AnnotationMirror atvUBAnno = atvUB.getAnnotationInHierarchy(top);
      AnnotationMirror atvLBAnno = atvLB.getAnnotationInHierarchy(top);

      annotatedTypeVariable.removeAnnotationInHierarchy(top);
      atvUB.addAnnotation(atvUBAnno);
      atvLB.addAnnotation(atvLBAnno);
      apply(atvUB, ub, typeVar.getUpperBound(), top);
      apply(atvLB, lb, typeVar.getLowerBound(), top);
    }
  }
}
