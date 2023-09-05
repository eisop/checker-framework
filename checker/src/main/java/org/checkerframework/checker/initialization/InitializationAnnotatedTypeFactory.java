package org.checkerframework.checker.initialization;

import com.sun.source.tree.ClassTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.Tree;

import org.checkerframework.checker.initialization.qual.HoldsForDefaultValues;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.dataflow.analysis.TransferResult;
import org.checkerframework.dataflow.cfg.node.ReturnNode;
import org.checkerframework.framework.flow.CFAbstractValue;
import org.checkerframework.framework.flow.CFValue;
import org.checkerframework.framework.qual.MonotonicQualifier;
import org.checkerframework.framework.qual.PolymorphicQualifier;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.type.GenericAnnotatedTypeFactory;
import org.checkerframework.framework.util.AnnotatedTypes;
import org.checkerframework.javacutil.Pair;

import java.lang.annotation.Annotation;
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
     * Gets the factory of the {@link InitializationFieldAccessSubchecker}.
     *
     * @return the factory of the {@link InitializationFieldAccessSubchecker}
     */
    protected InitializationFieldAccessAnnotatedTypeFactory getFieldAccessFactory() {
        InitializationChecker checker = getChecker();
        BaseTypeChecker targetChecker = checker.getSubchecker(checker.getTargetCheckerClass());
        return targetChecker.getTypeFactoryOfSubchecker(InitializationFieldAccessSubchecker.class);
    }

    // Don't perform the same flow analysis twice.
    // Instead, reuse results from InitializationFieldAccessChecker

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
    public List<Pair<ReturnNode, TransferResult<CFValue, InitializationStore>>>
            getReturnStatementStores(MethodTree methodTree) {
        return getFieldAccessFactory().getReturnStatementStores(methodTree);
    }

    /**
     * Determines whether the specified variable's current value is initialized.
     *
     * <p>Returns {@code true} iff the variable's current value is initialized. This holds for
     * variables whose value has a non-top qualifier that does not have the meta-annotation {@link
     * HoldsForDefaultValues} as well as variables whose declaration has a qualifier that {@link
     * HoldsForDefaultValues}.
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
        AnnotatedTypeMirror declType = factory.getAnnotatedTypeLhs(var);

        Set<? extends AnnotationMirror> topAnnotations =
                factory.getQualifierHierarchy().getTopAnnotations();

        for (Class<? extends Annotation> invariant : factory.getSupportedTypeQualifiers()) {
            // Skip default-value, monotonic, polymorphic, and top qualifiers
            if (invariant.getAnnotation(HoldsForDefaultValues.class) != null
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
