package org.checkerframework.framework.type;

import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedExecutableType;

import java.util.List;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;

/**
 * An interface for viewpoint adaptation.
 *
 * <p>Viewpoint adaptation takes a type that was written from a particular viewpoint (the
 * declaration viewpoint) and translates it to a type from another viewpoint (the receiver
 * viewpoint). For example, if a field's type uses an annotation like {@code @This}, viewpoint
 * adaptation replaces that annotation with the actual receiver type at the use site.
 *
 * <p>Viewpoint adaptation applies to member/field accesses, constructor invocations, method
 * invocations, and type parameter bound instantiations.
 */
public interface ViewpointAdapter {

    /**
     * Viewpoint-adapts a member or field access.
     *
     * <p>Developer notes: When this method is invoked on a member/field with a type given by a type
     * parameter, the type arguments are correctly substituted, and {@code memberType} is already in
     * a good shape. Only annotations on {@code memberType} should be replaced by the
     * viewpoint-adapted ones.
     *
     * @param receiverType the receiver type through which the member or field is accessed
     * @param memberElement the element of the accessed member or field
     * @param memberType the accessed type of the member or field. After the method returns, it will
     *     be mutated to the viewpoint-adapted result.
     */
    void viewpointAdaptMember(
            AnnotatedTypeMirror receiverType,
            Element memberElement,
            AnnotatedTypeMirror memberType);

    /**
     * Viewpoint-adapts a constructor invocation. Takes an unsubstituted method invocation type and
     * performs the viewpoint adaptation in place, modifying the parameter.
     *
     * @param receiverType the receiver type through which a constructor is invoked
     * @param constructorElt the element of the invoked constructor
     * @param constructorType the invoked type of the constructor with type variables not
     *     substituted. After the method returns, it will be mutated to the viewpoint-adapted
     *     constructor type.
     */
    void viewpointAdaptConstructor(
            AnnotatedTypeMirror receiverType,
            ExecutableElement constructorElt,
            AnnotatedExecutableType constructorType);

    /**
     * Viewpoint-adapts a method invocation. Takes an unsubstituted method invocation type and
     * performs the viewpoint adaptation in place, modifying the parameter.
     *
     * @param receiverType the receiver type through which a method is invoked
     * @param methodElt the element of the invoked method. Only used to determine whether this type
     *     should be viewpoint-adapted.
     * @param methodType the invoked type of the method with type variables not substituted. After
     *     the method returns, it will be mutated to the viewpoint-adapted method type.
     */
    void viewpointAdaptMethod(
            AnnotatedTypeMirror receiverType,
            ExecutableElement methodElt,
            AnnotatedExecutableType methodType);

    /**
     * Viewpoint-adapts a type parameter bound when being instantiated.
     *
     * @param receiverType the receiver type through which the type parameter is instantiated
     * @param typeParameterBounds a list of type parameter bounds. After the method returns, it will
     *     be mutated to the viewpoint-adapted type parameter bounds.
     */
    void viewpointAdaptTypeParameterBounds(
            AnnotatedTypeMirror receiverType,
            List<AnnotatedTypeParameterBounds> typeParameterBounds);

    /**
     * Viewpoint-adapts a type written from the receiver's declaration viewpoint.
     *
     * @param receiverType the receiver type through which {@code declaredType} is viewed
     * @param declaredType the type to viewpoint-adapt
     * @return the viewpoint-adapted type
     */
    AnnotatedTypeMirror viewpointAdaptType(
            AnnotatedTypeMirror receiverType, AnnotatedTypeMirror declaredType);
}
