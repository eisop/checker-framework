package org.checkerframework.checker.pico;

import com.sun.source.tree.ClassTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.NewClassTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.UnaryTree;
import com.sun.source.tree.VariableTree;
import com.sun.source.util.TreePath;
import com.sun.tools.javac.code.Symbol;

import org.checkerframework.checker.pico.qual.Assignable;
import org.checkerframework.checker.pico.qual.Immutable;
import org.checkerframework.checker.pico.qual.ObjectIdentityMethod;
import org.checkerframework.framework.qual.DefaultFor;
import org.checkerframework.framework.qual.TypeKind;
import org.checkerframework.framework.type.AnnotatedTypeFactory;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedDeclaredType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedExecutableType;
import org.checkerframework.framework.util.AnnotatedTypes;
import org.checkerframework.javacutil.AnnotationProvider;
import org.checkerframework.javacutil.ElementUtils;
import org.checkerframework.javacutil.TreePathUtil;
import org.checkerframework.javacutil.TreeUtils;
import org.checkerframework.javacutil.TypesUtils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;

public class PICOTypeUtil {

    private static final Set<Tree.Kind> sideEffectingUnaryOperators;

    static {
        sideEffectingUnaryOperators = new HashSet<>();
        sideEffectingUnaryOperators.add(Tree.Kind.POSTFIX_INCREMENT);
        sideEffectingUnaryOperators.add(Tree.Kind.PREFIX_INCREMENT);
        sideEffectingUnaryOperators.add(Tree.Kind.POSTFIX_DECREMENT);
        sideEffectingUnaryOperators.add(Tree.Kind.PREFIX_DECREMENT);
    }

    /**
     * Determine if Typekind is one of the @DefaultFor typeKinds in @Immutable annotation.
     *
     * @param atm AnnotatedTypeMirror to be checked
     * @return true if TypeKind is one of the @DefaultFor typeKinds in @Immutable annotation, false
     *     otherwise
     */
    private static boolean isInTypeKindsOfDefaultForOfImmutable(AnnotatedTypeMirror atm) {
        DefaultFor defaultFor = Immutable.class.getAnnotation(DefaultFor.class);
        assert defaultFor != null;
        for (TypeKind typeKind : defaultFor.typeKinds()) {
            if (typeKind.name().equals(atm.getKind().name())) return true;
        }
        return false;
    }

    /**
     * Determine if Type is one of the @DefaultFor types in @Immutable annotation.
     *
     * @param atm AnnotatedTypeMirror to be checked
     * @return true if Type is one of the @DefaultFor types in @Immutable annotation, false
     *     otherwise
     */
    private static boolean isInTypesOfDefaultForOfImmutable(AnnotatedTypeMirror atm) {
        if (!atm.getKind().name().equals(TypeKind.DECLARED.name())) {
            return false;
        }
        DefaultFor defaultFor = Immutable.class.getAnnotation(DefaultFor.class);
        assert defaultFor != null;
        Class<?>[] types = defaultFor.types();
        String fqn = TypesUtils.getQualifiedName((DeclaredType) atm.getUnderlyingType());
        for (Class<?> type : types) {
            if (type.getCanonicalName().contentEquals(fqn)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Determine if the underlying type is implicitly immutable. This method is consistent with the
     * types and typeKinds that are in @DefaultFor in the definition of @Immutable qualifier.
     *
     * @param atm AnnotatedTypeMirror to be checked
     * @return true if the underlying type is implicitly immutable, false otherwise
     */
    public static boolean isImplicitlyImmutableType(AnnotatedTypeMirror atm) {
        return isInTypeKindsOfDefaultForOfImmutable(atm) || isInTypesOfDefaultForOfImmutable(atm);
    }

    /**
     * Returns the bound of type declaration enclosing the node. If no annotation exists on type
     * declaration, bound is defaulted to @Mutable instead of having empty annotations. This method
     * simply gets/defaults annotation on bounds of classes, but doesn't validate the correctness of
     * the annotation. They are validated in {@link PICONoInitVisitor#processClassTree(ClassTree)}
     * method.
     *
     * @param node tree whose enclosing type declaration's bound annotation is to be extracted
     * @param atypeFactory pico type factory
     * @return annotation on the bound of enclosing type declaration
     */
    public static AnnotatedTypeMirror getBoundTypeOfEnclosingTypeDeclaration(
            Tree node, AnnotatedTypeFactory atypeFactory) {
        TypeElement typeElement = null;
        if (node instanceof MethodTree) {
            MethodTree methodTree = (MethodTree) node;
            ExecutableElement element = TreeUtils.elementFromDeclaration(methodTree);
            typeElement = ElementUtils.enclosingTypeElement(element);
        } else if (node instanceof VariableTree) {
            VariableTree variableTree = (VariableTree) node;
            VariableElement variableElement = TreeUtils.elementFromDeclaration(variableTree);
            assert variableElement != null && variableElement.getKind().isField();
            typeElement = ElementUtils.enclosingTypeElement(variableElement);
        }

        if (typeElement != null) {
            return getBoundTypeOfTypeDeclaration(typeElement, atypeFactory);
        }

        return null;
    }

    /**
     * Returns the bound of type declaration enclosing the element. If no annotation exists on type
     * declaration, bound is defaulted to @Mutable instead of having empty annotations. This method
     * simply gets/defaults annotation on bounds of classes, but doesn't validate the correctness of
     * the annotation. They are validated in {@link PICONoInitVisitor#processClassTree(ClassTree)}
     * method.
     *
     * @param element element whose enclosing type declaration's bound annotation is to be extracted
     * @param atypeFactory pico type factory
     * @return annotation on the bound of enclosing type declaration
     */
    public static AnnotatedTypeMirror getBoundTypeOfEnclosingTypeDeclaration(
            Element element, AnnotatedTypeFactory atypeFactory) {
        TypeElement typeElement = ElementUtils.enclosingTypeElement(element);
        if (typeElement != null) {
            return getBoundTypeOfTypeDeclaration(typeElement, atypeFactory);
        }
        return null;
    }

    public static List<AnnotatedTypeMirror> getBoundTypesOfDirectSuperTypes(
            TypeElement current, AnnotatedTypeFactory atypeFactory) {
        List<AnnotatedTypeMirror> boundsOfSupers = new ArrayList<>();
        TypeMirror supertypecls;
        try {
            supertypecls = current.getSuperclass();
        } catch (Symbol.CompletionFailure cf) {
            // Copied from ElementUtils#getSuperTypes(Elements, TypeElement)
            // Looking up a supertype failed. This sometimes happens
            // when transitive dependencies are not on the classpath.
            // As javac didn't complain, let's also not complain.
            // TODO: Use an expanded ErrorReporter to output a message.
            supertypecls = null;
        }

        if (supertypecls != null && !supertypecls.getKind().name().equals(TypeKind.NONE.name())) {
            TypeElement supercls = (TypeElement) ((DeclaredType) supertypecls).asElement();
            boundsOfSupers.add(getBoundTypeOfTypeDeclaration(supercls, atypeFactory));
        }

        for (TypeMirror supertypeitf : current.getInterfaces()) {
            TypeElement superitf = (TypeElement) ((DeclaredType) supertypeitf).asElement();
            boundsOfSupers.add(getBoundTypeOfTypeDeclaration(superitf, atypeFactory));
        }
        return boundsOfSupers;
    }

    public static AnnotatedTypeMirror getBoundTypeOfTypeDeclaration(
            ClassTree classTree, AnnotatedTypeFactory atypeFactory) {
        TypeElement typeElement = TreeUtils.elementFromDeclaration(classTree);
        return getBoundTypeOfTypeDeclaration(typeElement, atypeFactory);
    }

    /**
     * Returns the bound of type declaration. If no annotation exists on type declaration, bound is
     * defaulted to @Mutable instead of having empty annotations. This method simply gets/defaults
     * annotation on bounds of classes, but doesn't validate the correctness of the annotation. They
     * are validated in {@link PICONoInitVisitor#processClassTree(ClassTree)} method.
     *
     * @param element type declaration whose bound annotation is to be extracted
     * @param atypeFactory pico type factory
     * @return annotation on the bound of type declaration
     */
    public static AnnotatedTypeMirror getBoundTypeOfTypeDeclaration(
            Element element, AnnotatedTypeFactory atypeFactory) {
        // Reads bound annotation from source code or stub files
        // Implicitly immutable types have @Immutable in its bound
        // All other elements that are: not implicitly immutable types specified in definition of
        // @Immutable qualifier;
        // Or has no bound annotation on its type element declaration either in source tree or stub
        // file(jdk.astub) have @Mutable in its bound
        return atypeFactory.getAnnotatedType(element);

        // It's a bit strange that bound annotations on implicilty immutable types
        // are not specified in the stub file. For implicitly immutable types, having bounds in stub
        // file suppresses type cast warnings, because in base implementation, it checks cast type
        // is whether
        // from element itself. If yes, no matter what the casted type is, the warning is
        // suppressed, which is
        // also not wanted. BUT, they are applied @Immutable as their bounds CORRECTLY, because we
        // have TypeAnnotator!

        // TODO This method doesn't have logic of handling anonymous class! We should implement it,
        // maybe in different
        // places, at some time.
    }

    public static AnnotatedTypeMirror getBoundTypeOfTypeDeclaration(
            TypeMirror typeMirror, AnnotatedTypeFactory atypeFactory) {
        return getBoundTypeOfTypeDeclaration(TypesUtils.getTypeElement(typeMirror), atypeFactory);
    }

    /** Helper method to determine a method using method name */
    public static boolean isMethodOrOverridingMethod(
            AnnotatedExecutableType methodType,
            String methodName,
            AnnotatedTypeFactory annotatedTypeFactory) {
        return isMethodOrOverridingMethod(
                methodType.getElement(), methodName, annotatedTypeFactory);
    }

    public static boolean isMethodOrOverridingMethod(
            ExecutableElement executableElement,
            String methodName,
            AnnotatedTypeFactory annotatedTypeFactory) {
        // Check if it is the target method
        if (executableElement.toString().contentEquals(methodName)) return true;
        // Check if it is overriding the target method
        // Because AnnotatedTypes.overriddenMethods returns all the methods overriden in the class
        // hierarchy, we need to
        // iterate over the set to check if it's overriding corresponding methods specifically in
        // java.lang.Object class
        Iterator<Map.Entry<@Immutable AnnotatedDeclaredType, ExecutableElement>> overriddenMethods =
                AnnotatedTypes.overriddenMethods(
                                annotatedTypeFactory.getElementUtils(),
                                annotatedTypeFactory,
                                executableElement)
                        .entrySet()
                        .iterator();
        while (overriddenMethods.hasNext()) {
            if (overriddenMethods.next().getValue().toString().contentEquals(methodName)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Determine if the type is enum or enum constant.
     *
     * @param annotatedTypeMirror The annotated type mirror to check
     * @return true if the type is enum or enum constant, false otherwise
     */
    public static boolean isEnumOrEnumConstant(AnnotatedTypeMirror annotatedTypeMirror) {
        Element element =
                ((AnnotatedDeclaredType) annotatedTypeMirror).getUnderlyingType().asElement();
        return element != null
                && (element.getKind() == ElementKind.ENUM_CONSTANT
                        || element.getKind() == ElementKind.ENUM);
    }

    public static boolean isEnumOrEnumConstant(TypeMirror type) {
        if (!(type instanceof DeclaredType)) {
            return false;
        }
        Element element = ((DeclaredType) type).asElement();
        return element != null
                && (element.getKind() == ElementKind.ENUM_CONSTANT
                        || element.getKind() == ElementKind.ENUM);
    }

    /**
     * Check if a field is final or not.
     *
     * @param variableElement The field element
     * @return true if the field is final, false otherwise
     */
    public static boolean isFinalField(Element variableElement) {
        assert variableElement instanceof VariableElement;
        return ElementUtils.isFinal(variableElement);
    }

    /**
     * Check if a field is assignable. A field is assignable if it is static and not final, or has
     * explicit @Assignable
     *
     * @param variableElement The field element
     * @param provider The annotation provider
     * @return true if the field is assignable
     */
    public static boolean isAssignableField(Element variableElement, AnnotationProvider provider) {
        if (!(variableElement instanceof VariableElement)) {
            return false;
        }
        boolean hasExplicitAssignableAnnotation =
                provider.getDeclAnnotation(variableElement, Assignable.class) != null;
        if (!ElementUtils.isStatic(variableElement)) {
            // Instance fields must have explicit @Assignable annotation to be assignable
            return hasExplicitAssignableAnnotation;
        } else {
            // If there is explicit @Assignable annotation on static fields, then it's assignable;
            // If there isn't,
            // and the static field is not final, we treat it as if it's assignable field.
            return hasExplicitAssignableAnnotation || !isFinalField(variableElement);
        }
    }

    /**
     * Check if a field is @ReceiverDependantAssignable. Static fields always returns false.
     *
     * @param variableElement The field element
     * @param provider The annotation provider
     * @return true if the field is @ReceiverDependantAssignable
     */
    public static boolean isReceiverDependantAssignable(
            Element variableElement, AnnotationProvider provider) {
        assert variableElement instanceof VariableElement;
        if (ElementUtils.isStatic(variableElement)) {
            // Static fields can never be @ReceiverDependantAssignable!
            return false;
        }
        return !isAssignableField(variableElement, provider) && !isFinalField(variableElement);
    }

    /**
     * Check if a field has one and only one assignability qualifier. Only the following
     * combinations are valid:
     *
     * <p>1. Explicit @Assignable 2. Final field 3. @ReceiverDependentAssignable, where there is no
     * explicit annotation in the source code
     *
     * @param field The field element
     * @param provider The annotation provider
     * @return true if the field has one and only one assignability qualifier
     */
    public static boolean hasOneAndOnlyOneAssignabilityQualifier(
            VariableElement field, AnnotationProvider provider) {
        boolean valid = false;
        if (isAssignableField(field, provider)
                && !isFinalField(field)
                && !isReceiverDependantAssignable(field, provider)) {
            valid = true;
        } else if (!isAssignableField(field, provider)
                && isFinalField(field)
                && !isReceiverDependantAssignable(field, provider)) {
            valid = true;
        } else if (!isAssignableField(field, provider)
                && !isFinalField(field)
                && isReceiverDependantAssignable(field, provider)) {
            assert !ElementUtils.isStatic(field);
            valid = true;
        }
        return valid;
    }

    /**
     * Check if a field is a field that can be assigned to. A field is assignable if it is static
     * and not final, or has explicit @Assignable
     *
     * @param tree The tree of the field
     * @param provider The annotation provider
     * @return true if the field is assignable
     */
    public static boolean isAssigningAssignableField(
            ExpressionTree tree, AnnotationProvider provider) {
        Element fieldElement = TreeUtils.elementFromUse(tree);
        if (fieldElement == null) return false;
        return isAssignableField(fieldElement, provider);
    }

    public static boolean isEnclosedByAnonymousClass(Tree tree, AnnotatedTypeFactory atypeFactory) {
        TreePath path = atypeFactory.getPath(tree);
        if (path != null) {
            ClassTree classTree = TreePathUtil.enclosingClass(path);
            return classTree != null;
        }
        return false;
    }

    public static AnnotatedDeclaredType getBoundOfEnclosingAnonymousClass(
            Tree tree, AnnotatedTypeFactory atypeFactory) {
        TreePath path = atypeFactory.getPath(tree);
        if (path == null) {
            return null;
        }
        AnnotatedDeclaredType enclosingType = null;
        Tree newclassTree = TreePathUtil.enclosingOfKind(path, Tree.Kind.NEW_CLASS);
        if (newclassTree != null) {
            enclosingType = (AnnotatedDeclaredType) atypeFactory.getAnnotatedType(newclassTree);
        }
        return enclosingType;
    }

    public static boolean inStaticScope(TreePath treePath) {
        boolean in = false;
        if (TreePathUtil.isTreeInStaticScope(treePath)) {
            in = true;
            // Exclude case in which enclosing class is static
            ClassTree classTree = TreePathUtil.enclosingClass(treePath);
            if (classTree != null
                    && classTree.getModifiers().getFlags().contains(Modifier.STATIC)) {
                in = false;
            }
        }
        return in;
    }

    public static boolean isSideEffectingUnaryTree(final UnaryTree tree) {
        return sideEffectingUnaryOperators.contains(tree.getKind());
    }

    /**
     * !! Experimental !!
     *
     * <p>CF's isAnonymous cannot recognize some anonymous classes with annotation on new clause.
     * This one hopefully will provide a workaround when the class tree is available.
     *
     * <p>This will work if an anonymous class decl tree is always a child node of a {@code
     * NewClassTree}. i.e. an anonymous class declaration is always inside a new clause.
     *
     * @param tree a class decl tree.
     * @param atypeFactory used to get the path. Any factory should be ok.
     * @return whether the class decl tree is of an anonymous class
     */
    public static boolean isAnonymousClassTree(ClassTree tree, AnnotatedTypeFactory atypeFactory) {
        TreePath path = atypeFactory.getPath(tree);
        Tree parent = path.getParentPath().getLeaf();
        return parent instanceof NewClassTree && ((NewClassTree) parent).getClassBody() != null;
    }

    /**
     * !! Experimental !! Check whether the type is actually an array.
     *
     * @param type AnnotatedDeclaredType
     * @param typeFactory any AnnotatedTypeFactory
     * @return true if the type is array
     */
    public static boolean isArrayType(
            AnnotatedDeclaredType type, AnnotatedTypeFactory typeFactory) {
        Element ele =
                typeFactory.getProcessingEnv().getTypeUtils().asElement(type.getUnderlyingType());

        // If it is a user-declared "Array" class without package, a class / source file should be
        // there.
        // Otherwise, it is the java inner type.
        return ele instanceof Symbol.ClassSymbol
                && ElementUtils.getQualifiedName(ele).equals("Array")
                && ((Symbol.ClassSymbol) ele).classfile == null
                && ((Symbol.ClassSymbol) ele).sourcefile == null;
    }

    public static boolean isObjectIdentityMethod(
            MethodTree node, AnnotatedTypeFactory annotatedTypeFactory) {
        ExecutableElement element = TreeUtils.elementFromDeclaration(node);
        return isObjectIdentityMethod(element, annotatedTypeFactory);
    }

    public static boolean isObjectIdentityMethod(
            ExecutableElement executableElement, AnnotatedTypeFactory annotatedTypeFactory) {
        return hasObjectIdentityMethodDeclAnnotation(executableElement, annotatedTypeFactory)
                || isMethodOrOverridingMethod(executableElement, "hashCode()", annotatedTypeFactory)
                || isMethodOrOverridingMethod(
                        executableElement, "equals(java.lang.Object)", annotatedTypeFactory);
    }

    private static boolean hasObjectIdentityMethodDeclAnnotation(
            ExecutableElement element, AnnotatedTypeFactory annotatedTypeFactory) {
        return annotatedTypeFactory.getDeclAnnotation(element, ObjectIdentityMethod.class) != null;
    }
}
