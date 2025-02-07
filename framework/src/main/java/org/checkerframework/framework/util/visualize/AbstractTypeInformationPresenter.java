package org.checkerframework.framework.util.visualize;

import com.sun.source.tree.AssignmentTree;
import com.sun.source.tree.BinaryTree;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.CompoundAssignmentTree;
import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.LambdaExpressionTree;
import com.sun.source.tree.LiteralTree;
import com.sun.source.tree.MemberReferenceTree;
import com.sun.source.tree.MemberSelectTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.ReturnTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.TypeParameterTree;
import com.sun.source.tree.UnaryTree;
import com.sun.source.tree.VariableTree;
import com.sun.source.util.TreePath;
import com.sun.source.util.TreePathScanner;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.framework.flow.CFAbstractAnalysis;
import org.checkerframework.framework.flow.CFAbstractStore;
import org.checkerframework.framework.flow.CFAbstractTransfer;
import org.checkerframework.framework.flow.CFAbstractValue;
import org.checkerframework.framework.type.AnnotatedTypeFactory;
import org.checkerframework.framework.type.AnnotatedTypeFormatter;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedExecutableType;
import org.checkerframework.framework.type.DefaultAnnotatedTypeFormatter;
import org.checkerframework.framework.type.GenericAnnotatedTypeFactory;
import org.checkerframework.javacutil.BugInCF;
import org.checkerframework.javacutil.TreePathUtil;
import org.checkerframework.javacutil.TreeUtils;
import org.plumelib.util.ArraySet;

import java.util.Arrays;

/** Presents formatted type information for various AST trees in a class. */
public abstract class AbstractTypeInformationPresenter implements TypeInformationPresenter {

    /** The {@link AnnotatedTypeFactory} for the current analysis. */
    protected final AnnotatedTypeFactory atypeFactory;

    /**
     * The {@link GenericAnnotatedTypeFactory} for the current analysis. null if the factory is not
     * an instance of {@link GenericAnnotatedTypeFactory}; otherwise, {@code factory} and {@code
     * genFactory} refer to the same object.
     */
    protected final @Nullable GenericAnnotatedTypeFactory<
                    ? extends CFAbstractValue<?>,
                    ? extends CFAbstractStore<? extends CFAbstractValue<?>, ?>,
                    ? extends CFAbstractTransfer<?, ?, ?>,
                    ? extends CFAbstractAnalysis<?, ?, ?>>
            genFactory;

    /** This formats the ATMs that the presenter is going to present. */
    protected final AnnotatedTypeFormatter typeFormatter;

    /**
     * Constructs a presenter for the given factory.
     *
     * @param atypeFactory the {@link AnnotatedTypeFactory} for the current analysis
     */
    public AbstractTypeInformationPresenter(AnnotatedTypeFactory atypeFactory) {
        this.atypeFactory = atypeFactory;
        if (atypeFactory instanceof GenericAnnotatedTypeFactory<?, ?, ?, ?>) {
            this.genFactory = (GenericAnnotatedTypeFactory<?, ?, ?, ?>) atypeFactory;
        } else {
            this.genFactory = null;
        }
        this.typeFormatter = createTypeFormatter();
    }

    /**
     * The entry point for presenting type information of trees in the given class.
     *
     * @param tree a {@link ClassTree} that has been annotated by the factory
     * @param treePath a {@link TreePath} to {@code tree}
     */
    @Override
    public void process(ClassTree tree, TreePath treePath) {
        TypeInformationReporter visitor = createTypeInformationReporter(tree);
        visitor.scan(treePath, null);
    }

    /**
     * Creates the {@link TypeInformationReporter} to use.
     *
     * @param tree a {@link ClassTree} that has been annotated by the factory
     * @return the {@link TypeInformationReporter} to use
     */
    protected abstract TypeInformationReporter createTypeInformationReporter(ClassTree tree);

    /**
     * Creates the {@link AnnotatedTypeFormatter} to use for output.
     *
     * @return the {@link AnnotatedTypeFormatter} to use for output
     */
    protected AnnotatedTypeFormatter createTypeFormatter() {
        return new DefaultAnnotatedTypeFormatter(true, true);
    }

    /**
     * A visitor which traverses a class tree and reports type information of various sub-trees.
     *
     * <p>Note: Since nested class trees will be type-checked separately, this visitor does not dive
     * into any nested class trees.
     */
    protected abstract class TypeInformationReporter extends TreePathScanner<Void, Void> {

        /** The class tree in which it traverses and reports type information. */
        protected final ClassTree classTree;

        /**
         * Root of the current class tree. This is a helper for computing positions of a sub-tree.
         */
        protected final CompilationUnitTree currentRoot;

        /** The checker that's currently running. */
        protected final BaseTypeChecker checker;

        /**
         * Constructs a new reporter for the given class tree.
         *
         * @param classTree the {@link ClassTree}
         */
        public TypeInformationReporter(ClassTree classTree) {
            this.classTree = classTree;
            this.checker = atypeFactory.getChecker();
            this.currentRoot = this.checker.getPathToCompilationUnit().getCompilationUnit();
        }

        /**
         * Report the {@code type} of {@code tree} in a particular {@code occurrenceKind}.
         *
         * @param tree the tree
         * @param type the type
         * @param occurrenceKind the occurrence kind
         */
        protected abstract void reportTreeType(
                Tree tree, AnnotatedTypeMirror type, TypeOccurrenceKind occurrenceKind);

        @Override
        public Void visitClass(ClassTree tree, Void unused) {
            @SuppressWarnings("interning:not.interned")
            boolean isNestedClass = tree != classTree;
            if (isNestedClass) {
                // Since nested class trees will be type-checked separately, this visitor does
                // not dive into any nested class trees.
                return null;
            }
            return super.visitClass(tree, unused);
        }

        @Override
        public Void visitTypeParameter(TypeParameterTree tree, Void unused) {
            reportTreeType(
                    tree,
                    atypeFactory.getAnnotatedTypeFromTypeTree(tree),
                    TypeOccurrenceKind.DECLARED_TYPE);
            return super.visitTypeParameter(tree, unused);
        }

        @Override
        public Void visitVariable(VariableTree tree, Void unused) {
            // TODO: "int x = 1" is a VariableTree, but there is no AssignmentTree and it
            // TODO: is difficult to locate the "=" symbol.
            AnnotatedTypeMirror varType =
                    genFactory != null
                            ? genFactory.getAnnotatedTypeLhs(tree)
                            : atypeFactory.getAnnotatedType(tree);
            reportTreeType(tree, varType, TypeOccurrenceKind.DECLARED_TYPE);
            return super.visitVariable(tree, unused);
        }

        @Override
        public Void visitMethod(MethodTree tree, Void unused) {
            reportTreeType(
                    tree, atypeFactory.getAnnotatedType(tree), TypeOccurrenceKind.DECLARED_TYPE);
            return super.visitMethod(tree, unused);
        }

        @Override
        public Void visitMethodInvocation(MethodInvocationTree tree, Void unused) {
            reportTreeType(
                    tree,
                    atypeFactory.methodFromUse(tree).executableType,
                    TypeOccurrenceKind.USE_TYPE);
            return super.visitMethodInvocation(tree, unused);
        }

        @Override
        public Void visitAssignment(AssignmentTree tree, Void unused) {
            AnnotatedTypeMirror varType =
                    genFactory != null
                            ? genFactory.getAnnotatedTypeLhs(tree.getVariable())
                            : atypeFactory.getAnnotatedType(tree.getVariable());
            reportTreeType(tree, varType, TypeOccurrenceKind.ASSIGN_LHS_DECLARED_TYPE);
            reportTreeType(
                    tree,
                    atypeFactory.getAnnotatedType(tree.getExpression()),
                    TypeOccurrenceKind.ASSIGN_RHS_TYPE);
            return super.visitAssignment(tree, unused);
        }

        @Override
        public Void visitCompoundAssignment(CompoundAssignmentTree tree, Void unused) {
            reportTreeType(tree, atypeFactory.getAnnotatedType(tree), TypeOccurrenceKind.USE_TYPE);
            AnnotatedTypeMirror varType =
                    genFactory != null
                            ? genFactory.getAnnotatedTypeLhs(tree.getVariable())
                            : atypeFactory.getAnnotatedType(tree.getVariable());
            reportTreeType(tree, varType, TypeOccurrenceKind.ASSIGN_LHS_DECLARED_TYPE);
            reportTreeType(
                    tree,
                    atypeFactory.getAnnotatedType(tree.getExpression()),
                    TypeOccurrenceKind.ASSIGN_RHS_TYPE);
            return super.visitCompoundAssignment(tree, unused);
        }

        @Override
        public Void visitUnary(UnaryTree tree, Void unused) {
            Tree.Kind treeKind = tree.getKind();
            switch (treeKind) {
                case UNARY_PLUS:
                case UNARY_MINUS:
                case BITWISE_COMPLEMENT:
                case LOGICAL_COMPLEMENT:
                case PREFIX_INCREMENT:
                case PREFIX_DECREMENT:
                    reportTreeType(
                            tree, atypeFactory.getAnnotatedType(tree), TypeOccurrenceKind.USE_TYPE);
                    break;
                case POSTFIX_INCREMENT:
                case POSTFIX_DECREMENT:
                    reportTreeType(
                            tree, atypeFactory.getAnnotatedType(tree), TypeOccurrenceKind.USE_TYPE);
                    if (genFactory != null) {
                        reportTreeType(
                                tree,
                                genFactory.getAnnotatedTypeRhsUnaryAssign(tree),
                                TypeOccurrenceKind.ASSIGN_RHS_TYPE);
                    }
                    break;
                default:
                    throw new BugInCF(
                            "Unsupported unary tree type "
                                    + treeKind
                                    + " for "
                                    + TypeInformationPresenter.class.getCanonicalName());
            }
            return super.visitUnary(tree, unused);
        }

        @Override
        public Void visitBinary(BinaryTree tree, Void unused) {
            reportTreeType(tree, atypeFactory.getAnnotatedType(tree), TypeOccurrenceKind.USE_TYPE);
            return super.visitBinary(tree, unused);
        }

        @Override
        public Void visitMemberSelect(MemberSelectTree tree, Void unused) {
            if (TreeUtils.isFieldAccess(tree)) {
                reportTreeType(
                        tree, atypeFactory.getAnnotatedType(tree), TypeOccurrenceKind.USE_TYPE);
            } else if (TreeUtils.isMethodAccess(tree)) {
                reportTreeType(
                        tree,
                        atypeFactory.getAnnotatedType(tree),
                        TypeOccurrenceKind.DECLARED_TYPE);
            }

            return super.visitMemberSelect(tree, unused);
        }

        @Override
        public Void visitMemberReference(MemberReferenceTree tree, Void unused) {
            // the declared type of the functional interface
            reportTreeType(
                    tree, atypeFactory.getAnnotatedType(tree), TypeOccurrenceKind.DECLARED_TYPE);
            // the use type of the functional interface
            reportTreeType(
                    tree,
                    atypeFactory.getFnInterfaceFromTree(tree).first,
                    TypeOccurrenceKind.USE_TYPE);
            return super.visitMemberReference(tree, unused);
        }

        @Override
        public Void visitIdentifier(IdentifierTree tree, Void unused) {
            switch (TreeUtils.elementFromUse(tree).getKind()) {
                case ENUM_CONSTANT:
                case FIELD:
                case PARAMETER:
                case LOCAL_VARIABLE:
                case EXCEPTION_PARAMETER:
                case RESOURCE_VARIABLE:
                case CONSTRUCTOR:
                    reportTreeType(
                            tree, atypeFactory.getAnnotatedType(tree), TypeOccurrenceKind.USE_TYPE);
                    break;
                case METHOD:
                    reportTreeType(
                            tree,
                            atypeFactory.getAnnotatedType(tree),
                            TypeOccurrenceKind.DECLARED_TYPE);
                    break;
                default:
                    break;
            }
            return super.visitIdentifier(tree, unused);
        }

        @Override
        public Void visitLiteral(LiteralTree tree, Void unused) {
            reportTreeType(tree, atypeFactory.getAnnotatedType(tree), TypeOccurrenceKind.USE_TYPE);
            return super.visitLiteral(tree, unused);
        }

        /** The tree kinds for methods and lambda expressions. */
        private final ArraySet<Tree.Kind> methodAndLambdaExpression =
                new ArraySet<>(Arrays.asList(Tree.Kind.METHOD, Tree.Kind.LAMBDA_EXPRESSION));

        @Override
        public Void visitReturn(ReturnTree tree, Void unused) {
            // No output for void methods.
            if (tree.getExpression() == null) {
                return super.visitReturn(tree, unused);
            }

            Tree enclosing =
                    TreePathUtil.enclosingOfKind(getCurrentPath(), methodAndLambdaExpression);

            AnnotatedTypeMirror ret = null;
            if (enclosing.getKind() == Tree.Kind.METHOD) {
                MethodTree enclosingMethod = TreePathUtil.enclosingMethod(getCurrentPath());
                ret = atypeFactory.getMethodReturnType(enclosingMethod, tree);
            } else {
                AnnotatedExecutableType result =
                        atypeFactory.getFunctionTypeFromTree((LambdaExpressionTree) enclosing);
                ret = result.getReturnType();
            }

            if (ret != null) {
                reportTreeType(tree, ret, TypeOccurrenceKind.DECLARED_TYPE);
            }
            return super.visitReturn(tree, unused);
        }
    }
}
