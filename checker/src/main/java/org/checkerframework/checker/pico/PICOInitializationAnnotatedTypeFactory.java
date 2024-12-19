package org.checkerframework.checker.pico;

import com.sun.source.tree.ClassTree;
import com.sun.source.tree.VariableTree;
import com.sun.source.util.TreePath;

import org.checkerframework.checker.initialization.InitializationAnnotatedTypeFactory;
import org.checkerframework.checker.initialization.InitializationChecker;
import org.checkerframework.checker.initialization.InitializationStore;
import org.checkerframework.checker.pico.qual.Mutable;
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
 * The InitializationAnnotatedTypeFactory for the PICO type system. This class is mainly created to
 * override getUninitializedFields() method for PICO specific definite assignment check.
 */
public class PICOInitializationAnnotatedTypeFactory extends InitializationAnnotatedTypeFactory {
    /** The @Mutable annotation. */
    private final AnnotationMirror MUTABLE;

    /**
     * Constructor for PICOInitializationAnnotatedTypeFactory.
     *
     * @param checker the BaseTypeChecker this visitor works with
     */
    public PICOInitializationAnnotatedTypeFactory(BaseTypeChecker checker) {
        super(checker);
        Elements elements = checker.getElementUtils();
        MUTABLE = AnnotationBuilder.fromClass(elements, Mutable.class);
        postInit();
    }

    /**
     * {@inheritDoc}
     *
     * <p>In @Immutable and @ReceiverDependentMutable class, all fields should be initialized in the
     * constructor except for fields explicitly annotated with @Assignable and static fields.
     */
    @Override
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

        // Filter out fields annotated with @Assignable or static fields or fields in @Mutable class
        uninitializedFields.removeIf(
                var -> {
                    ClassTree enclosingClass = TreePathUtil.enclosingClass(getPath(var));
                    TypeElement typeElement = TreeUtils.elementFromDeclaration(enclosingClass);
                    AnnotatedTypeMirror bound =
                            PICOTypeUtil.getBoundTypeOfTypeDeclaration(typeElement, factory);
                    // If the class is not annotated with @Immutable or @ReceiverDependentMutable,
                    // return false
                    if (bound.hasAnnotation(MUTABLE)) {
                        return true;
                    } else {
                        Element varElement = TreeUtils.elementFromDeclaration(var);
                        // If the field is annotated with @Assignable, return false
                        if (PICOTypeUtilnested.isAssignableField(varElement, this)
                                || ElementUtils.isStatic(varElement)) {
                            return true;
                        } else {
                            return false;
                        }
                    }
                });
        return uninitializedFields;
    }

    // TODO this is a hack for calling static class method for lambda expression, consider remove
    // with refactor of PICOTypeUtil
    /** Nested class for calling static method in PICOTypeUtil. */
    public static class PICOTypeUtilnested extends PICOTypeUtil {}
}
