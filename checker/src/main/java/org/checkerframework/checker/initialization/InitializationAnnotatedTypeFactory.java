package org.checkerframework.checker.initialization;

import com.sun.source.tree.ClassTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.VariableTree;
import com.sun.source.util.TreePath;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.tree.JCTree;

import org.checkerframework.checker.initialization.qual.HoldsForDefaultValue;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.dataflow.analysis.TransferResult;
import org.checkerframework.dataflow.cfg.node.ClassNameNode;
import org.checkerframework.dataflow.cfg.node.FieldAccessNode;
import org.checkerframework.dataflow.cfg.node.ImplicitThisNode;
import org.checkerframework.dataflow.cfg.node.Node;
import org.checkerframework.dataflow.cfg.node.ReturnNode;
import org.checkerframework.framework.flow.CFAbstractStore;
import org.checkerframework.framework.flow.CFAbstractValue;
import org.checkerframework.framework.flow.CFValue;
import org.checkerframework.framework.qual.MonotonicQualifier;
import org.checkerframework.framework.qual.PolymorphicQualifier;
import org.checkerframework.framework.source.SourceChecker;
import org.checkerframework.framework.type.AnnotatedTypeFactory;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.type.GenericAnnotatedTypeFactory;
import org.checkerframework.framework.util.AnnotatedTypes;
import org.checkerframework.javacutil.BugInCF;
import org.checkerframework.javacutil.ElementUtils;
import org.checkerframework.javacutil.TreePathUtil;
import org.checkerframework.javacutil.TreeUtils;
import org.plumelib.util.IPair;

import java.lang.annotation.Annotation;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.VariableElement;

/**
 * The annotated type factory for the freedom-before-commitment type system. When using the
 * freedom-before-commitment type system as a subchecker, you must ensure that the parent checker
 * hooks into it properly. See {@link InitializationChecker} for further information.
 */
public class InitializationAnnotatedTypeFactory extends InitializationParentAnnotatedTypeFactory {

    /**
     * Create a new InitializationAnnotatedTypeFactory.
     *
     * @param checker the checker to which the new type factory belongs
     */
    public InitializationAnnotatedTypeFactory(BaseTypeChecker checker) {
        super(checker);
        postInit();
    }

    @Override
    public InitializationChecker getChecker() {
        return (InitializationChecker) super.getChecker();
    }

    /**
     * Gets the factory of the {@link InitializationFieldAccessSubchecker}, whose flow-analysis
     * results we reuse to avoid performing the same flow analysis twice.
     *
     * <p>If type checking has not yet started, the subcheckers are uninitialized, and this returns
     * {@code null}. More concretely, this method only returns a non-null value after {@link
     * SourceChecker#initChecker()} has been called on all subcheckers. Since the flow analysis is
     * initialized in {@link AnnotatedTypeFactory#postInit()}, and the type factory is created after
     * all subcheckers have been initialized, this method will always return a non-null value unless
     * a subclass attempts to use it for some purpose other than accessing the flow analysis.
     *
     * @return the factory of the {@link InitializationFieldAccessSubchecker}, or {@code null} if
     *     not yet initialized
     * @see #createFlowAnalysis()
     * @see #performFlowAnalysis(ClassTree)
     * @see #getRegularExitStore(Tree)
     * @see #getExceptionalExitStore(Tree)
     * @see #getReturnStatementStores(MethodTree)
     */
    protected @Nullable InitializationFieldAccessAnnotatedTypeFactory getFieldAccessFactory() {
        InitializationChecker checker = getChecker();
        BaseTypeChecker targetChecker = checker.getSubchecker(checker.getTargetCheckerClass());
        return targetChecker.getTypeFactoryOfSubcheckerOrNull(
                InitializationFieldAccessSubchecker.class);
    }

    @Override
    protected InitializationAnalysis createFlowAnalysis() {
        return getFieldAccessFactory().getAnalysis();
    }

    @Override
    protected void performFlowAnalysis(ClassTree classTree) {
        flowResult = getFieldAccessFactory().getFlowResult();
    }

    @Override
    public InitializationStore getRegularExitStore(Tree tree) {
        return getFieldAccessFactory().getRegularExitStore(tree);
    }

    @Override
    public InitializationStore getExceptionalExitStore(Tree tree) {
        return getFieldAccessFactory().getExceptionalExitStore(tree);
    }

    @Override
    public List<IPair<ReturnNode, TransferResult<CFValue, InitializationStore>>>
            getReturnStatementStores(MethodTree methodTree) {
        return getFieldAccessFactory().getReturnStatementStores(methodTree);
    }

    /**
     * {@inheritDoc}
     *
     * <p>This implementaiton also takes the target checker into account.
     *
     * @see #getUninitializedFields(InitializationStore, CFAbstractStore, TreePath, boolean,
     *     Collection)
     */
    @Override
    protected void setSelfTypeInInitializationCode(
            Tree tree, AnnotatedTypeMirror.AnnotatedDeclaredType selfType, TreePath path) {
        ClassTree enclosingClass = TreePathUtil.enclosingClass(path);
        Type classType = ((JCTree) enclosingClass).type;
        AnnotationMirror annotation;

        // If all fields are initialized-only, and they are all initialized,
        // then:
        //  - if the class is final, this is @Initialized
        //  - otherwise, this is @UnderInitialization(CurrentClass) as
        //    there might still be subclasses that need initialization.
        if (areAllFieldsInitializedOnly(enclosingClass)) {
            GenericAnnotatedTypeFactory<?, ?, ?, ?> targetFactory =
                    checker.getTypeFactoryOfSubcheckerOrNull(
                            ((InitializationChecker) checker).getTargetCheckerClass());
            InitializationStore initStore = getStoreBefore(tree);
            CFAbstractStore<?, ?> targetStore = targetFactory.getStoreBefore(tree);
            if (initStore != null
                    && targetStore != null
                    && getUninitializedFields(
                                    initStore, targetStore, path, false, Collections.emptyList())
                            .isEmpty()) {
                if (classType.isFinal()) {
                    annotation = INITIALIZED;
                } else {
                    annotation = createUnderInitializationAnnotation(classType);
                }
            } else if (initStore != null
                    && getUninitializedFields(initStore, path, false, Collections.emptyList())
                            .isEmpty()) {
                if (classType.isFinal()) {
                    annotation = INITIALIZED;
                } else {
                    annotation = createUnderInitializationAnnotation(classType);
                }
            } else {
                annotation = null;
            }
        } else {
            annotation = null;
        }

        if (annotation == null) {
            annotation = getUnderInitializationAnnotationOfSuperType(classType);
        }
        selfType.replaceAnnotation(annotation);
    }

    /**
     * Returns the fields that are not yet initialized in a given store, taking into account the
     * target checker.
     *
     * <p>A field f is initialized if
     *
     * <ul>
     *   <li>f is initialized in the initialization store, i.e., it has been assigned;
     *   <li>the value of f in the target store has a non-top qualifier that does not have the
     *       meta-annotation {@link HoldsForDefaultValue}; or
     *   <li>the declared qualifier of f in the target hierarchy either has the meta-annotation
     *       {@link HoldsForDefaultValue} or is a top qualifier.
     * </ul>
     *
     * <p>See {@link #getUninitializedFields(InitializationStore, TreePath, boolean, Collection)}
     * for a method that does not require the target checker.
     *
     * @param initStore a store for the initialization checker
     * @param targetStore a store for the target checker corresponding to initStore
     * @param path the current path, used to determine the current class
     * @param isStatic whether to report static fields or instance fields
     * @param receiverAnnotations the annotations on the receiver
     * @return the fields that are not yet initialized in a given store
     */
    public List<VariableTree> getUninitializedFields(
            InitializationStore initStore,
            CFAbstractStore<?, ?> targetStore,
            TreePath path,
            boolean isStatic,
            Collection<? extends AnnotationMirror> receiverAnnotations) {
        List<VariableTree> uninitializedFields =
                super.getUninitializedFields(initStore, path, isStatic, receiverAnnotations);

        GenericAnnotatedTypeFactory<?, ?, ?, ?> factory =
                checker.getTypeFactoryOfSubcheckerOrNull(
                        ((InitializationChecker) checker).getTargetCheckerClass());

        if (factory == null) {
            throw new BugInCF(
                    "Did not find target type factory for checker "
                            + ((InitializationChecker) checker).getTargetCheckerClass());
        }

        // Remove primitives
        if (!((InitializationChecker) checker).checkPrimitives()) {
            uninitializedFields.removeIf(var -> getAnnotatedType(var).getKind().isPrimitive());
        }

        // Filter out fields which are initialized according to subchecker
        uninitializedFields.removeIf(
                var -> {
                    ClassTree enclosingClass = TreePathUtil.enclosingClass(getPath(var));
                    Node receiver;
                    if (ElementUtils.isStatic(TreeUtils.elementFromDeclaration(var))) {
                        receiver = new ClassNameNode(enclosingClass);
                    } else {
                        receiver =
                                new ImplicitThisNode(
                                        TreeUtils.elementFromDeclaration(enclosingClass).asType());
                    }
                    VariableElement varElement = TreeUtils.elementFromDeclaration(var);
                    FieldAccessNode fa = new FieldAccessNode(var, varElement, receiver);
                    CFAbstractValue<?> value = targetStore.getValue(fa);
                    return isInitialized(factory, value, varElement);
                });

        return uninitializedFields;
    }

    /**
     * Determines whether the specified variable's current value is initialized.
     *
     * <p>Returns {@code true} iff the variable's current value is initialized. This holds for
     * variables whose value has a non-top qualifier that does not have the meta-annotation {@link
     * HoldsForDefaultValue} (e.g., variables with a {@code NonNull} value), as well as variables
     * whose declaration has a qualifier that has the meta-annotation {@link HoldsForDefaultValue}
     * (e.g., variables whose declared type is {@code Nullable}).
     *
     * @param factory the parent checker's factory
     * @param value the variable's current value
     * @param var the variable to check
     * @return whether the specified variable is yet to be initialized
     */
    public static boolean isInitialized(
            GenericAnnotatedTypeFactory<?, ?, ?, ?> factory,
            CFAbstractValue<?> value,
            VariableElement var) {
        AnnotatedTypeMirror declType = factory.getAnnotatedType(var);

        Set<? extends AnnotationMirror> topAnnotations =
                factory.getQualifierHierarchy().getTopAnnotations();

        for (Class<? extends Annotation> invariant : factory.getSupportedTypeQualifiers()) {
            // Skip default-value, monotonic, polymorphic, and top qualifiers
            if (invariant.getAnnotation(HoldsForDefaultValue.class) != null
                    || invariant.getAnnotation(MonotonicQualifier.class) != null
                    || invariant.getAnnotation(PolymorphicQualifier.class) != null
                    || topAnnotations.stream()
                            .anyMatch(
                                    annotation -> factory.areSameByClass(annotation, invariant))) {
                continue;
            }

            boolean hasInvariantInStore =
                    value != null
                            && value.getAnnotations().stream()
                                    .anyMatch(
                                            annotation ->
                                                    factory.areSameByClass(annotation, invariant));
            boolean hasInvariantAtDeclaration =
                    AnnotatedTypes.findEffectiveLowerBoundAnnotations(
                                    factory.getQualifierHierarchy(), declType)
                            .stream()
                            .anyMatch(annotation -> factory.areSameByClass(annotation, invariant));

            if (hasInvariantAtDeclaration && !hasInvariantInStore) {
                return false;
            }
        }

        return true;
    }
}
