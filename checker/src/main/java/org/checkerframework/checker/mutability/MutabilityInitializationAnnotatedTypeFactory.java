package org.checkerframework.checker.mutability;

import com.sun.source.tree.ClassTree;
import com.sun.source.tree.VariableTree;
import com.sun.source.util.TreePath;

import org.checkerframework.checker.initialization.InitializationAnnotatedTypeFactory;
import org.checkerframework.checker.initialization.InitializationChecker;
import org.checkerframework.checker.initialization.InitializationStore;
import org.checkerframework.checker.mutability.qual.Mutable;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.framework.flow.CFAbstractStore;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.type.GenericAnnotatedTypeFactory;
import org.checkerframework.javacutil.AnnotationBuilder;
import org.checkerframework.javacutil.BugInCF;
import org.checkerframework.javacutil.ElementUtils;
import org.checkerframework.javacutil.TreePathUtil;
import org.checkerframework.javacutil.TreeUtils;

import java.util.Collection;
import java.util.List;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.Elements;

/**
 * The InitializationAnnotatedTypeFactory for the mutability type system. This class is mainly
 * created to override getUninitializedFields() method for mutability-specific definite assignment
 * check.
 */
public class MutabilityInitializationAnnotatedTypeFactory
        extends InitializationAnnotatedTypeFactory {
    /** The {@link Mutable} annotation. */
    private final AnnotationMirror MUTABLE;

    /**
     * Constructor for MutabilityInitializationAnnotatedTypeFactory.
     *
     * @param checker the BaseTypeChecker this visitor works with
     */
    @SuppressWarnings("this-escape")
    public MutabilityInitializationAnnotatedTypeFactory(BaseTypeChecker checker) {
        super(checker);
        Elements elements = checker.getElementUtils();
        MUTABLE = AnnotationBuilder.fromClass(elements, Mutable.class);
        postInit();
    }

    /**
     * {@inheritDoc}
     *
     * <p>This method applies the Mutability Checker's initialization policy after obtaining the
     * fields that the Initialization Checker considers uninitialized:
     *
     * <ul>
     *   <li>Fields of {@code @Mutable} classes are not subject to the Mutability Checker's
     *       initialization requirement. Java's definite-assignment rules already require every
     *       blank {@code final} field to be initialized.
     *   <li>{@code @Assignable} fields are excluded because their semantics explicitly permit
     *       assignment after initialization.
     *   <li>Static fields are excluded because they belong to the class rather than to an instance
     *       whose initialization state is being checked.
     * </ul>
     *
     * <p>Therefore, only non-static, non-{@code @Assignable} fields of {@code @Immutable} and
     * {@code @ReceiverDependentMutable} classes are required to be initialized by a constructor.
     *
     * <p>This method intentionally ignores {@code targetStore}. The target store is useful for a
     * type system such as Nullness, where flow information can prove that an otherwise unassigned
     * field satisfies its declared invariant. Mutability qualifiers do not establish that a field
     * was assigned: for example, the default value {@code null} may satisfy {@code @Immutable}, but
     * that does not initialize an immutable object's abstract state. Therefore, this method starts
     * with the assignment-only result from the Initialization Checker and then applies the
     * Mutability-specific exclusions above.
     */
    @Override
    public List<VariableTree> getUninitializedFields(
            InitializationStore initStore,
            CFAbstractStore<?, ?> targetStore,
            TreePath path,
            boolean isStatic,
            Collection<? extends AnnotationMirror> receiverAnnotations) {
        // Intentionally call the assignment-only overload. A qualifier in targetStore may describe
        // the mutability of a field's default value, but it does not prove that the field was
        // assigned.
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

        // Remove fields that the mutability checker does not require constructors to initialize.
        uninitializedFields.removeIf(
                var -> {
                    ClassTree enclosingClass = TreePathUtil.enclosingClass(getPath(var));
                    TypeElement typeElement = TreeUtils.elementFromDeclaration(enclosingClass);
                    AnnotatedTypeMirror bound = factory.getAnnotatedType(typeElement);
                    if (bound.hasAnnotation(MUTABLE)) {
                        // Java already enforces definite assignment for blank final fields. Other
                        // fields of mutable classes are not subject to the Mutability Checker's
                        // initialization requirement.
                        return true;
                    } else {
                        Element varElement = TreeUtils.elementFromDeclaration(var);
                        // Assignable fields permit post-initialization assignment. Static fields
                        // belong to the class, not to the instance whose state is being checked.
                        return ((MutabilityNoInitAnnotatedTypeFactory) factory)
                                        .isAssignableField(varElement)
                                || ElementUtils.isStatic(varElement);
                    }
                });
        return uninitializedFields;
    }
}
