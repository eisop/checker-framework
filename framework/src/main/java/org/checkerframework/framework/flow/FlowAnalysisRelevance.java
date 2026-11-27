package org.checkerframework.framework.flow;

import com.sun.source.tree.AnnotationTree;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.LambdaExpressionTree;
import com.sun.source.tree.MemberReferenceTree;
import com.sun.source.tree.MemberSelectTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.NewClassTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.VariableTree;
import com.sun.source.util.TreeScanner;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.dataflow.cfg.UnderlyingAST.CFGMethod;
import org.checkerframework.framework.flow.CFAbstractAnalysis.FieldInitialValue;
import org.checkerframework.framework.qual.DefaultQualifier;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedDeclaredType;
import org.checkerframework.framework.type.GenericAnnotatedTypeFactory;
import org.checkerframework.framework.type.visitor.SimpleAnnotatedTypeScanner;
import org.checkerframework.javacutil.AnnotationMirrorSet;
import org.checkerframework.javacutil.ElementUtils;
import org.checkerframework.javacutil.TreeUtils;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Name;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeKind;

/**
 * Decides whether the dataflow analysis of a method can compute a value that has a relevant
 * qualifier. A type system decides which qualifiers are irrelevant; for example, the top qualifier
 * of a type system in which the analysis only ever refines a type to a subtype of a qualifier that
 * is written somewhere.
 *
 * <p>The analysis computes a value with a relevant qualifier only if a type with a relevant
 * qualifier flows into the method, or if the method calls a method that the transfer function
 * treats specially, such as a method with a contract. A type flows into the method from a
 * declaration that the method refers to, from a class that it mentions, from a supertype of such a
 * class, from a type that is written in the method, from a default, or from the initial store.
 *
 * @param <V> the type of the abstract values of the analysis
 */
public abstract class FlowAnalysisRelevance<V extends CFAbstractValue<V>> {

    /** The tree kind of a record deconstruction pattern, or null before Java 21. */
    private static final Tree.@Nullable Kind DECONSTRUCTION_PATTERN = deconstructionPatternKind();

    /** The type factory. */
    private final GenericAnnotatedTypeFactory<V, ?, ?, ?> atypeFactory;

    /** The name {@code this}. */
    private final Name thisName;

    /** The name {@code super}. */
    private final Name superName;

    /** The name {@code class}. */
    private final Name className;

    /**
     * Maps a field, a method, a constructor, or a class to whether it is relevant: whether a type
     * with a relevant qualifier can flow from it into code that refers to it. An element that is
     * not a key has not been examined, or was examined together with a relevant element.
     */
    private final IdentityHashMap<Element, Boolean> relevantElements = new IdentityHashMap<>();

    /** Maps an element to whether it has a {@code @DefaultQualifier} annotation. */
    private final IdentityHashMap<Element, Boolean> defaultQualifierElements =
            new IdentityHashMap<>();

    /** The classes that {@link #isRelevantElement} still has to examine. */
    private final ArrayDeque<TypeElement> pendingClasses = new ArrayDeque<>();

    /** The classes that {@link #isRelevantElement} has examined in the current call. */
    private final Set<TypeElement> examinedClasses =
            Collections.newSetFromMap(new IdentityHashMap<>());

    /**
     * The methods that are declared in a method whose analysis is redundant. Their analysis is
     * redundant, too. Compared by reference.
     */
    private final Set<MethodTree> methodsInRedundantMethods =
            Collections.newSetFromMap(new IdentityHashMap<>());

    /** The list that {@link #anyRelevantFieldValue} describes. */
    private @Nullable List<FieldInitialValue<V>> examinedFieldValues;

    /** The number of elements of {@link #examinedFieldValues} that have been examined. */
    private int examinedFieldValuesCount;

    /** Whether one of the examined elements of {@link #examinedFieldValues} is relevant. */
    private boolean anyRelevantFieldValue;

    /**
     * Returns true if a type mentions a relevant qualifier, or mentions a class that is known to be
     * relevant. Adds the classes that the type mentions and that have not been examined to {@link
     * #pendingClasses}.
     */
    private final SimpleAnnotatedTypeScanner<Boolean, Void> typeScanner;

    /** Scans a method. */
    private final MethodScanner methodScanner = new MethodScanner();

    /**
     * Creates a FlowAnalysisRelevance.
     *
     * @param atypeFactory the type factory
     */
    @SuppressWarnings("this-escape") // the type scanner runs after construction
    protected FlowAnalysisRelevance(GenericAnnotatedTypeFactory<V, ?, ?, ?> atypeFactory) {
        this.atypeFactory = atypeFactory;
        this.thisName = atypeFactory.getElementUtils().getName("this");
        this.superName = atypeFactory.getElementUtils().getName("super");
        this.className = atypeFactory.getElementUtils().getName("class");
        this.typeScanner =
                new SimpleAnnotatedTypeScanner<>(
                        (type, p) -> isRelevantTypeComponent(type), Boolean::logicalOr, false);
    }

    /**
     * Returns the tree kind of a record deconstruction pattern.
     *
     * @return the tree kind of a record deconstruction pattern, or null before Java 21
     */
    private static Tree.@Nullable Kind deconstructionPatternKind() {
        for (Tree.Kind kind : Tree.Kind.values()) {
            if (kind.name().equals("DECONSTRUCTION_PATTERN")) {
                return kind;
            }
        }
        return null;
    }

    /** Forgets the methods of the previous compilation unit. */
    public void startCompilationUnit() {
        methodsInRedundantMethods.clear();
        examinedFieldValues = null;
    }

    /**
     * Returns true if the dataflow analysis of {@code method} cannot compute a value that has a
     * relevant qualifier.
     *
     * @param method a method that is not a constructor and that has no captured store
     * @param fieldValues the abstract values of the fields of the class of {@code method}
     * @return true if the dataflow analysis of {@code method} cannot compute a value that has a
     *     relevant qualifier
     */
    public boolean isFlowAnalysisRedundant(
            CFGMethod method, List<FieldInitialValue<V>> fieldValues) {
        if (anyRelevantFieldValue(fieldValues)) {
            return false;
        }
        MethodTree methodTree = method.getMethod();
        if (methodsInRedundantMethods.contains(methodTree)) {
            return true;
        }
        ExecutableElement methodElt = TreeUtils.elementFromDeclaration(methodTree);
        for (Element e = methodElt; e != null; e = e.getEnclosingElement()) {
            if (hasDefaultQualifier(e)) {
                return false;
            }
            if (ElementUtils.isTypeElement(e) && isRelevantElement(e)) {
                return false;
            }
        }
        methodScanner.start(methodTree);
        methodScanner.scan(methodTree, null);
        if (methodScanner.relevant) {
            return false;
        }
        methodsInRedundantMethods.addAll(methodScanner.nestedMethods);
        return true;
    }

    /**
     * Returns true if a field value has a relevant qualifier. The initial store of a method
     * contains these values.
     *
     * @param fieldValues the abstract values of fields; elements are only ever appended to it
     * @return true if a field value has a relevant qualifier
     */
    @SuppressWarnings("interning:not.interned") // the same list is passed for a class
    private boolean anyRelevantFieldValue(List<FieldInitialValue<V>> fieldValues) {
        if (examinedFieldValues != fieldValues) {
            examinedFieldValues = fieldValues;
            examinedFieldValuesCount = 0;
            anyRelevantFieldValue = false;
        }
        for (;
                !anyRelevantFieldValue && examinedFieldValuesCount < fieldValues.size();
                examinedFieldValuesCount++) {
            FieldInitialValue<V> fieldValue = fieldValues.get(examinedFieldValuesCount);
            anyRelevantFieldValue =
                    isRelevantValue(fieldValue.declared)
                            || (fieldValue.initializer != null
                                    && isRelevantValue(fieldValue.initializer));
        }
        return anyRelevantFieldValue;
    }

    /**
     * Returns true if {@code value} has a relevant qualifier.
     *
     * @param value an abstract value
     * @return true if {@code value} has a relevant qualifier
     */
    protected boolean isRelevantValue(V value) {
        return isRelevantAnnotationSet(value.getAnnotations());
    }

    /**
     * Returns true if {@code anno} is irrelevant: a value with only irrelevant qualifiers is what
     * the type factory computes without the analysis.
     *
     * @param anno a qualifier of the type system
     * @return true if {@code anno} is irrelevant
     */
    protected abstract boolean isIrrelevantQualifier(AnnotationMirror anno);

    /**
     * Returns true if the transfer function treats {@code tree} specially, so that the analysis can
     * compute a value with a relevant qualifier for its arguments or its receiver.
     *
     * @param tree a method invocation
     * @return true if the transfer function can refine a value at {@code tree}
     */
    protected boolean isRelevantInvocation(MethodInvocationTree tree) {
        return false;
    }

    /**
     * Returns true if {@code annos} contains a relevant qualifier.
     *
     * @param annos a set of annotations
     * @return true if {@code annos} contains a relevant qualifier
     */
    protected final boolean isRelevantAnnotationSet(AnnotationMirrorSet annos) {
        for (AnnotationMirror anno : annos) {
            if (!isIrrelevantQualifier(anno)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns true if {@code type}, not considering its component types, has a relevant qualifier
     * or is a class that is known to be relevant. Adds its class to {@link #pendingClasses} if the
     * class has not been examined.
     *
     * @param type a type
     * @return true if {@code type} itself makes a declaration relevant
     */
    private boolean isRelevantTypeComponent(AnnotatedTypeMirror type) {
        // The null type is only the implicit lower bound of a type variable or wildcard. No
        // expression has that type.
        if (type.getKind() != TypeKind.NULL && isRelevantAnnotationSet(type.getAnnotations())) {
            return true;
        }
        if (type.getKind() == TypeKind.DECLARED) {
            TypeElement typeElt =
                    (TypeElement) ((AnnotatedDeclaredType) type).getUnderlyingType().asElement();
            Boolean known = relevantElements.get(typeElt);
            if (known != null) {
                return known;
            }
            pendingClasses.add(typeElt);
        }
        return false;
    }

    /**
     * Returns true if {@code elt} has a {@code @DefaultQualifier} annotation.
     *
     * @param elt an element
     * @return true if {@code elt} has a {@code @DefaultQualifier} annotation
     */
    private boolean hasDefaultQualifier(Element elt) {
        Boolean known = defaultQualifierElements.get(elt);
        if (known == null) {
            known =
                    atypeFactory.getDeclAnnotation(elt, DefaultQualifier.class) != null
                            || atypeFactory.getDeclAnnotation(elt, DefaultQualifier.List.class)
                                    != null;
            defaultQualifierElements.put(elt, known);
        }
        return known;
    }

    /**
     * Returns true if a type with a relevant qualifier can flow from {@code elt} into code that
     * refers to it:
     *
     * <ul>
     *   <li>the type of {@code elt} mentions a relevant qualifier,
     *   <li>{@code elt} is a method or constructor that has a contract,
     *   <li>the type of {@code elt} mentions a class for which this method returns true, or
     *   <li>{@code elt} is a class that has a supertype that mentions such a qualifier or such a
     *       class.
     * </ul>
     *
     * @param elt a field, a method, a constructor, or a class
     * @return true if a type with a relevant qualifier can flow from {@code elt}
     */
    private boolean isRelevantElement(Element elt) {
        Boolean known = relevantElements.get(elt);
        if (known != null) {
            return known;
        }
        boolean relevant;
        if (ElementUtils.isTypeElement(elt)) {
            pendingClasses.add((TypeElement) elt);
            relevant = false;
        } else {
            relevant = typeScanner.visit(atypeFactory.getAnnotatedType(elt));
            if (!relevant && elt instanceof ExecutableElement) {
                relevant =
                        !atypeFactory
                                .getContractsFromMethod()
                                .getContracts((ExecutableElement) elt)
                                .isEmpty();
            }
        }
        while (!relevant && !pendingClasses.isEmpty()) {
            TypeElement typeElt = pendingClasses.remove();
            Boolean knownClass = relevantElements.get(typeElt);
            if (knownClass != null) {
                relevant = knownClass;
                continue;
            }
            if (!examinedClasses.add(typeElt)) {
                continue;
            }
            AnnotatedDeclaredType classType = atypeFactory.getAnnotatedType(typeElt);
            boolean relevantClass = typeScanner.visit(classType);
            for (AnnotatedDeclaredType supertype : classType.directSupertypes()) {
                relevantClass = relevantClass || typeScanner.visit(supertype);
            }
            if (relevantClass) {
                relevantElements.put(typeElt, true);
                relevant = true;
            }
        }
        pendingClasses.clear();
        if (!relevant) {
            // Every examined class was examined completely, together with every class that it
            // depends on.
            for (TypeElement typeElt : examinedClasses) {
                relevantElements.put(typeElt, false);
            }
        }
        examinedClasses.clear();
        relevantElements.put(elt, relevant);
        return relevant;
    }

    /**
     * Scans a method, including the classes and lambdas in it, and determines whether it refers to
     * anything relevant.
     */
    private final class MethodScanner extends TreeScanner<Void, Void> {

        /** Whether the method refers to anything relevant. */
        boolean relevant;

        /** The methods that are declared in the scanned method. */
        final List<MethodTree> nestedMethods = new ArrayList<>();

        /** The scanned method. */
        private @Nullable MethodTree root;

        /** The variables that are declared in the scanned method. */
        private final Set<Element> declaredVariables =
                Collections.newSetFromMap(new IdentityHashMap<>());

        /**
         * Prepares this scanner for scanning {@code method}.
         *
         * @param method the method to scan
         */
        void start(MethodTree method) {
            relevant = false;
            nestedMethods.clear();
            declaredVariables.clear();
            root = method;
        }

        @Override
        public Void scan(Tree tree, Void p) {
            if (relevant || tree == null) {
                return null;
            }
            if (tree.getKind() == DECONSTRUCTION_PATTERN) {
                // The type of a binding variable can come from a record component.
                relevant = true;
                return null;
            }
            return super.scan(tree, p);
        }

        @Override
        public Void visitAnnotation(AnnotationTree tree, Void p) {
            AnnotationMirror anno = TreeUtils.annotationFromAnnotationTree(tree);
            if (atypeFactory.isSupportedQualifierOrAlias(anno)
                    || atypeFactory.areSameByClass(anno, DefaultQualifier.class)
                    || atypeFactory.areSameByClass(anno, DefaultQualifier.List.class)) {
                relevant = true;
                return null;
            }
            return super.visitAnnotation(tree, p);
        }

        @Override
        @SuppressWarnings("interning:not.interned") // AST node comparison
        public Void visitMethod(MethodTree tree, Void p) {
            if (tree != root) {
                nestedMethods.add(tree);
            }
            // A stub file can add qualifiers that are not written in the method.
            if (isRelevantElement(TreeUtils.elementFromDeclaration(tree))) {
                relevant = true;
                return null;
            }
            return super.visitMethod(tree, p);
        }

        @Override
        public Void visitClass(ClassTree tree, Void p) {
            if (isRelevantElement(TreeUtils.elementFromDeclaration(tree))) {
                relevant = true;
                return null;
            }
            return super.visitClass(tree, p);
        }

        @Override
        public Void visitVariable(VariableTree tree, Void p) {
            declaredVariables.add(TreeUtils.elementFromDeclaration(tree));
            return super.visitVariable(tree, p);
        }

        @Override
        public Void visitIdentifier(IdentifierTree tree, Void p) {
            visitUse(TreeUtils.elementFromTree((Tree) tree), tree.getName());
            return null;
        }

        @Override
        public Void visitMemberSelect(MemberSelectTree tree, Void p) {
            visitUse(TreeUtils.elementFromTree((Tree) tree), tree.getIdentifier());
            return super.visitMemberSelect(tree, p);
        }

        @Override
        public Void visitMethodInvocation(MethodInvocationTree tree, Void p) {
            if (isRelevantInvocation(tree)) {
                relevant = true;
                return null;
            }
            return super.visitMethodInvocation(tree, p);
        }

        @Override
        public Void visitLambdaExpression(LambdaExpressionTree tree, Void p) {
            // The types of implicitly typed lambda parameters come from the function type.
            if (isRelevantElement(TreeUtils.findFunction(tree, atypeFactory.getProcessingEnv()))) {
                relevant = true;
                return null;
            }
            return super.visitLambdaExpression(tree, p);
        }

        @Override
        public Void visitNewClass(NewClassTree tree, Void p) {
            if (isRelevantElement(TreeUtils.getSuperConstructor(tree))) {
                relevant = true;
                return null;
            }
            return super.visitNewClass(tree, p);
        }

        @Override
        public Void visitMemberReference(MemberReferenceTree tree, Void p) {
            Element elt = TreeUtils.elementFromTree((Tree) tree);
            if (elt != null && isRelevantElement(elt)) {
                relevant = true;
                return null;
            }
            return super.visitMemberReference(tree, p);
        }

        /**
         * Sets {@link #relevant} if a use of {@code elt} makes the method relevant.
         *
         * @param elt the element that an identifier or a member select refers to, or null
         * @param name the name that is used
         */
        private void visitUse(@Nullable Element elt, Name name) {
            if (elt == null) {
                return;
            }
            switch (elt.getKind()) {
                case FIELD:
                    if (name == thisName || name == superName || name == className) {
                        return;
                    }
                    relevant = isRelevantElement(elt);
                    return;
                case ENUM_CONSTANT:
                case METHOD:
                case CONSTRUCTOR:
                    relevant = isRelevantElement(elt);
                    return;
                case PACKAGE:
                case TYPE_PARAMETER:
                    return;
                default:
                    if (ElementUtils.isTypeElement(elt)) {
                        relevant = isRelevantElement(elt);
                    } else if (!declaredVariables.contains(elt)) {
                        // A variable that is declared outside of the scanned method. Its type and
                        // its value are not examined.
                        relevant = true;
                    }
                    return;
            }
        }
    }
}
