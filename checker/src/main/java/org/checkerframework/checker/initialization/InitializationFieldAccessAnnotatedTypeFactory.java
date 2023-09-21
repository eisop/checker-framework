package org.checkerframework.checker.initialization;

import com.sun.source.tree.ClassTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.MemberSelectTree;
import com.sun.source.tree.Tree;

import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.dataflow.analysis.AnalysisResult;
import org.checkerframework.framework.flow.CFValue;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedExecutableType;
import org.checkerframework.framework.type.GenericAnnotatedTypeFactory;
import org.checkerframework.framework.type.treeannotator.TreeAnnotator;
import org.checkerframework.javacutil.AnnotationUtils;
import org.checkerframework.javacutil.TreeUtils;

import javax.lang.model.element.Element;
import javax.lang.model.type.TypeMirror;

/** The type factory for the {@link InitializationFieldAccessSubchecker}. */
public class InitializationFieldAccessAnnotatedTypeFactory
        extends InitializationParentAnnotatedTypeFactory {

    /**
     * Create a new InitializationFieldAccessAnnotatedTypeFactory.
     *
     * @param checker the checker to which the new type factory belongs
     */
    public InitializationFieldAccessAnnotatedTypeFactory(BaseTypeChecker checker) {
        super(checker);
        postInit();
    }

    @Override
    protected InitializationAnalysis createFlowAnalysis() {
        return new InitializationAnalysis(checker, this);
    }

    @Override
    protected void performFlowAnalysis(ClassTree classTree) {
        // Only perform the analysis if initialization checking is turned on.
        if (!checker.hasOption("assumeInitialized")) {
            super.performFlowAnalysis(classTree);
        }
    }

    /**
     * Returns the flow analysis.
     *
     * @return the flow analysis
     * @see #getFlowResult()
     */
    InitializationAnalysis getAnalysis() {
        return analysis;
    }

    /**
     * Returns the result of the flow analysis. Invariant:
     *
     * <pre>
     *  scannedClasses.get(c) == FINISHED for some class c &rArr; flowResult != null
     * </pre>
     *
     * Note that flowResult contains analysis results for Trees from multiple classes which are
     * produced by multiple calls to performFlowAnalysis.
     *
     * @return the result of the flow analysis
     * @see #getAnalysis()
     */
    AnalysisResult<CFValue, InitializationStore> getFlowResult() {
        return flowResult;
    }

    /**
     * This annotator should be added to {@link GenericAnnotatedTypeFactory#createTreeAnnotator} for
     * the target checker. It ensures that the fields of an uninitialized receiver have the top type
     * in the parent checker's hierarchy.
     *
     * @see InitializationChecker#getTargetCheckerClass()
     */
    public static class CommitmentFieldAccessTreeAnnotator extends TreeAnnotator {

        /**
         * Creates a new CommitmentFieldAccessTreeAnnotator.
         *
         * @param atypeFactory the type factory belonging to the init checker's parent
         */
        public CommitmentFieldAccessTreeAnnotator(
                GenericAnnotatedTypeFactory<?, ?, ?, ?> atypeFactory) {
            super(atypeFactory);
        }

        @Override
        public Void visitIdentifier(IdentifierTree tree, AnnotatedTypeMirror p) {
            super.visitIdentifier(tree, p);
            computeFieldAccessType(tree, p);
            return null;
        }

        @Override
        public Void visitMemberSelect(MemberSelectTree tree, AnnotatedTypeMirror p) {
            super.visitMemberSelect(tree, p);
            computeFieldAccessType(tree, p);
            return null;
        }

        /**
         * Adapts the type in the target checker hierarchy of a field access depending on the
         * field's declared type and the receiver's initialization type.
         *
         * @param tree the field access
         * @param type the field access's unadapted type
         */
        private void computeFieldAccessType(ExpressionTree tree, AnnotatedTypeMirror type) {
            GenericAnnotatedTypeFactory<?, ?, ?, ?> factory =
                    (GenericAnnotatedTypeFactory<?, ?, ?, ?>) atypeFactory;

            // Don't adapt anything if initialization checking is turned off.
            if (factory.getChecker().hasOption("assumeInitialized")) {
                return;
            }

            // Don't adapt anything if "tree" is not actually a field access.

            // Don't adapt uses of the identifiers "this" or "super" that are not field accesses
            // (e.g., constructor calls or uses of an outer this).
            if (tree instanceof IdentifierTree) {
                IdentifierTree identTree = (IdentifierTree) tree;
                if (identTree.getName().contentEquals("this")
                        || identTree.getName().contentEquals("super")) {
                    return;
                }
            }

            // Don't adapt method accesses.
            if (type instanceof AnnotatedExecutableType) {
                return;
            }

            // Don't adapt trees that do not have a (explicit or implicit) receiver (e.g., local
            // variables).
            InitializationFieldAccessAnnotatedTypeFactory initFactory =
                    atypeFactory
                            .getChecker()
                            .getTypeFactoryOfSubchecker(InitializationFieldAccessSubchecker.class);
            AnnotatedTypeMirror receiver = initFactory.getReceiverType(tree);
            if (receiver == null) {
                return;
            }

            // Don't adapt trees whose receiver is initialized.
            if (!initFactory.isUnknownInitialization(receiver)
                    && !initFactory.isUnderInitialization(receiver)) {
                return;
            }

            // Don't adapt trees with an explicit UnknownInitialization annotation on the field
            Element element = TreeUtils.elementFromUse(tree);
            AnnotatedTypeMirror fieldAnnotations = factory.getAnnotatedType(element);
            if (AnnotationUtils.containsSameByName(
                    fieldAnnotations.getAnnotations(), initFactory.UNKNOWN_INITIALIZATION)) {
                return;
            }

            TypeMirror fieldOwnerType = element.getEnclosingElement().asType();
            boolean isReceiverInitToOwner =
                    initFactory.isInitializedForFrame(receiver, fieldOwnerType);

            // If the field has been initialized, don't clear annotations.
            // This is ok even if the field was initialized with a non-invariant
            // value because in that case, there must have been an error before.
            // E.g.:
            //     { f1 = f2;
            //       f2 = f1; }
            // Here, we will get an error for the first assignment, but we won't get another
            // error for the second assignment.
            // See the AssignmentDuringInitialization test case.
            Tree declaration = initFactory.declarationFromElement(element);
            InitializationStore store = initFactory.getStoreBefore(tree);
            boolean isFieldInitialized =
                    store != null
                            && TreeUtils.isSelfAccess(tree)
                            && initFactory
                                    .getInitializedFields(store, initFactory.getPath(tree))
                                    .contains(declaration);
            if (!isReceiverInitToOwner
                    && !isFieldInitialized
                    && !factory.isComputingAnnotatedTypeMirrorOfLhs()) {
                // The receiver is not initialized for this frame and the type being computed is
                // not a LHS.
                // Replace all annotations with the top annotation for that hierarchy.
                type.clearAnnotations();
                type.addAnnotations(factory.getQualifierHierarchy().getTopAnnotations());
            }
        }
    }
}
