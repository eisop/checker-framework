package org.checkerframework.checker.initialization;

import org.checkerframework.checker.initialization.qual.Initialized;
import org.checkerframework.checker.initialization.qual.NotOnlyInitialized;
import org.checkerframework.dataflow.cfg.visualize.CFGVisualizer;
import org.checkerframework.dataflow.expression.ClassName;
import org.checkerframework.dataflow.expression.FieldAccess;
import org.checkerframework.dataflow.expression.JavaExpression;
import org.checkerframework.dataflow.expression.ThisReference;
import org.checkerframework.framework.flow.CFAbstractStore;
import org.checkerframework.framework.flow.CFValue;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.type.GenericAnnotatedTypeFactory;
import org.checkerframework.framework.util.AnnotatedTypes;
import org.plumelib.util.ToStringComparator;

import java.util.HashSet;
import java.util.Set;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeKind;

/**
 * A store that extends {@code CFAbstractStore} and additionally tracks which fields of the 'self'
 * reference have been initialized.
 *
 * @see InitializationTransfer
 */
public class InitializationStore extends CFAbstractStore<CFValue, InitializationStore> {

    /** The set of fields that are initialized. */
    protected final Set<VariableElement> initializedFields;

    /**
     * Creates a new InitializationStore.
     *
     * @param analysis the analysis class this store belongs to
     * @param sequentialSemantics should the analysis use sequential Java semantics?
     */
    public InitializationStore(InitializationAnalysis analysis, boolean sequentialSemantics) {
        super(analysis, sequentialSemantics);
        initializedFields = new HashSet<>(4);
    }

    /**
     * {@inheritDoc}
     *
     * <p>If the receiver is a field, and has an invariant annotation, then it can be considered
     * initialized.
     */
    @Override
    public void insertValue(JavaExpression je, CFValue value, boolean permitNondeterministic) {
        if (!shouldInsert(je, value, permitNondeterministic)) {
            return;
        }

        super.insertValue(je, value, permitNondeterministic);

        if (je instanceof FieldAccess) {
            FieldAccess fa = (FieldAccess) je;
            if (fa.getReceiver() instanceof ThisReference
                    || fa.getReceiver() instanceof ClassName) {
                addInitializedField(fa.getField());
            }
        }
    }

    /**
     * A copy constructor.
     *
     * @param other the store to copy
     */
    public InitializationStore(InitializationStore other) {
        super(other);
        initializedFields = new HashSet<>(other.initializedFields);
    }

    /**
     * Mark the field identified by the element {@code field} as initialized if it belongs to the
     * current class, or is static (in which case there is no aliasing issue and we can just add all
     * static fields).
     *
     * @param field a field that is initialized
     */
    public void addInitializedField(FieldAccess field) {
        boolean fieldOnThisReference = field.getReceiver() instanceof ThisReference;
        boolean staticField = field.isStatic();
        if (fieldOnThisReference || staticField) {
            initializedFields.add(field.getField());
        }
    }

    /**
     * Mark the field identified by the element {@code f} as initialized (the caller needs to ensure
     * that the field belongs to the current class, or is a static field).
     *
     * @param f a field that is initialized
     */
    public void addInitializedField(VariableElement f) {
        initializedFields.add(f);
    }

    /** Is the field identified by the element {@code f} initialized? */
    public boolean isFieldInitialized(Element f) {
        return initializedFields.contains(f);
    }

    @Override
    protected boolean supersetOf(CFAbstractStore<CFValue, InitializationStore> o) {
        if (!(o instanceof InitializationStore)) {
            return false;
        }
        InitializationStore other = (InitializationStore) o;

        for (Element field : other.initializedFields) {
            if (!initializedFields.contains(field)) {
                return false;
            }
        }

        return super.supersetOf(other);
    }

    @Override
    public InitializationStore leastUpperBound(InitializationStore other) {
        InitializationStore result = super.leastUpperBound(other);

        result.initializedFields.addAll(other.initializedFields);
        result.initializedFields.retainAll(initializedFields);

        return result;
    }

    @Override
    protected CFValue newFieldValueAfterMethodCall(
            FieldAccess fieldAccess,
            GenericAnnotatedTypeFactory<CFValue, InitializationStore, ?, ?> atypeFactory,
            CFValue value) {
        if (isDeclaredInitialized(fieldAccess)) {
            return value;
        }

        return super.newFieldValueAfterMethodCall(fieldAccess, atypeFactory, value);
    }

    /**
     * Determine whether the given field is declared as {@link Initialized} (taking into account
     * viewpoint adaption for {@link NotOnlyInitialized}).
     *
     * @param fieldAccess the field to check
     * @return whether the given field is declared as {@link Initialized} (taking into account
     *     viewpoint adaption for {@link NotOnlyInitialized})
     */
    protected boolean isDeclaredInitialized(FieldAccess fieldAccess) {
        InitializationParentAnnotatedTypeFactory atypeFactory =
                (InitializationParentAnnotatedTypeFactory) analysis.getTypeFactory();
        AnnotatedTypeMirror receiverType;
        if (thisValue != null && thisValue.getUnderlyingType().getKind() != TypeKind.ERROR) {
            receiverType =
                    AnnotatedTypeMirror.createType(
                            thisValue.getUnderlyingType(), atypeFactory, false);
            for (AnnotationMirror anno : thisValue.getAnnotations()) {
                receiverType.replaceAnnotation(anno);
            }
        } else if (!fieldAccess.isStatic()) {
            receiverType =
                    AnnotatedTypeMirror.createType(
                                    fieldAccess.getReceiver().getType(), atypeFactory, false)
                            .getErased();
            receiverType.addAnnotations(atypeFactory.getQualifierHierarchy().getTopAnnotations());
        } else {
            receiverType = null;
        }

        // We must use AnnotatedTypes.asMemberOf instead of
        // factory.getAnnotatedTypeLhs
        // to soundly handle @NotOnlyInitialized.
        AnnotatedTypeMirror declaredType =
                AnnotatedTypes.asMemberOf(
                        atypeFactory.types, atypeFactory, receiverType, fieldAccess.getField());
        return declaredType.hasAnnotation(atypeFactory.INITIALIZED);
    }

    @Override
    protected String internalVisualize(CFGVisualizer<CFValue, InitializationStore, ?> viz) {
        String superVisualize = super.internalVisualize(viz);

        String initializedVisualize =
                viz.visualizeStoreKeyVal(
                        "initialized fields", ToStringComparator.sorted(initializedFields));

        if (superVisualize.isEmpty()) {
            return String.join(viz.getSeparator(), initializedVisualize);
        } else {
            return String.join(viz.getSeparator(), superVisualize, initializedVisualize);
        }
    }

    /**
     * Returns the analysis associated with this store.
     *
     * @return the analysis associated with this store
     */
    public InitializationAnalysis getAnalysis() {
        return (InitializationAnalysis) analysis;
    }
}
