package org.checkerframework.idesupport;

import com.sun.source.tree.AssignmentTree;
import com.sun.source.tree.BinaryTree;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.LineMap;
import com.sun.source.tree.LiteralTree;
import com.sun.source.tree.MemberReferenceTree;
import com.sun.source.tree.MemberSelectTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.TypeParameterTree;
import com.sun.source.tree.UnaryTree;
import com.sun.source.tree.VariableTree;
import com.sun.source.util.SourcePositions;
import com.sun.source.util.TreeScanner;
import com.sun.tools.javac.tree.JCTree;

import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.framework.type.AnnotatedTypeFormatter;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.type.DefaultAnnotatedTypeFormatter;
import org.checkerframework.framework.type.GenericAnnotatedTypeFactory;
import org.checkerframework.javacutil.TreeUtils;

import javax.tools.Diagnostic;

/**
 * Presents formatted type information for various AST trees in a class.
 *
 * <p>The formatted type information is designed to be visualized by editors and IDEs that support
 * Language Server Protocol (LSP).
 */
public class TypeInformationPresenter {

    /** The AnnotatedTypeFactory for the current analysis. */
    private final GenericAnnotatedTypeFactory<?, ?, ?, ?> factory;

    /** This formats the ATMs that the presenter is going to present. */
    private final AnnotatedTypeFormatter typeFormatter;

    /**
     * Constructs a presenter for the given factory.
     *
     * @param factory The AnnotatedTypeFactory for the current analysis.
     */
    public TypeInformationPresenter(GenericAnnotatedTypeFactory<?, ?, ?, ?> factory) {
        this.factory = factory;
        this.typeFormatter = new DefaultAnnotatedTypeFormatter(true, true);
    }

    /**
     * The entry point for presenting type information of trees in the given class.
     *
     * @param tree A ClassTree that has been type-checked by the factory.
     */
    public void process(ClassTree tree) {
        TypeInformationReporter visitor = new TypeInformationReporter(tree);
        visitor.scan(tree, null);
    }

    /**
     * Stores an inclusive range [(startLine, startCol), (endLine, endCol)] in the source code to
     * which a piece of type information refers. All indices are 0-based since LSP uses 0-based
     * positions.
     */
    private static class MessageRange {
        /** 0-based line number of the start position. */
        private final long startLine;

        /** 0-based column number of the start position. */
        private final long startCol;

        /** 0-based line number of the end position. */
        private final long endLine;

        /** 0-based column number of the end position. */
        private final long endCol;

        /**
         * Constructs a new MessageRange with the given position information.
         *
         * @param startLine 0-based line number of the start position
         * @param startCol 0-based column number of the start position
         * @param endLine 0-based line number of the end position
         * @param endCol 0-based column number of the end position
         */
        private MessageRange(long startLine, long startCol, long endLine, long endCol) {
            this.startLine = startLine;
            this.startCol = startCol;
            this.endLine = endLine;
            this.endCol = endCol;
        }

        /**
         * Constructs a new MessageRange with the given position information.
         *
         * @param startLine 0-based line number of the start position
         * @param startCol 0-based column number of the start position
         * @param endLine 0-based line number of the end position
         * @param endCol 0-based column number of the end position
         * @return a new MessageRange with the given position information
         */
        private static MessageRange of(long startLine, long startCol, long endLine, long endCol) {
            return new MessageRange(startLine, startCol, endLine, endCol);
        }

        @Override
        public String toString() {
            return String.format("(%d, %d, %d, %d)", startLine, startCol, endLine, endCol);
        }
    }

    /**
     * A visitor which traverses a class tree and reports type information of various sub-trees.
     *
     * <p>Note: Since nested class trees will be type-checked separately, this visitor does not dive
     * into any nested class trees.
     */
    private class TypeInformationReporter extends TreeScanner<Void, Void> {

        /** The class tree in which it traverses and reports type information. */
        private final ClassTree classTree;

        /**
         * Root of the current class tree. This is a helper for computing positions of a sub-tree.
         */
        private final CompilationUnitTree currentRoot;

        /** This is a helper for computing positions of a sub-tree. */
        private final SourcePositions sourcePositions;

        /**
         * Constructs a new reporter for the given class tree.
         *
         * @param classTree a ClassTree
         */
        public TypeInformationReporter(ClassTree classTree) {
            this.classTree = classTree;
            this.currentRoot = factory.getChecker().getPathToCompilationUnit().getCompilationUnit();
            this.sourcePositions = factory.getTreeUtils().getSourcePositions();
        }

        /**
         * Sends out a report that indicates the range corresponds to the given node has the given
         * type. If the node is an artificial tree, don't report anything.
         *
         * @param node The tree that is used to find the corresponding range to report.
         * @param type The type that we are going to display.
         */
        private void reportNodeType(Tree node, AnnotatedTypeMirror type) {
            MessageRange messageRange = computeMessageRange(node);
            if (messageRange == null) {
                // don't report if the node doesn't exist in source file
                return;
            }

            BaseTypeChecker checker = factory.getChecker();
            checker.reportError(
                    node,
                    "lsp.type.information",
                    checker.getClass().getSimpleName(),
                    typeFormatter.format(type),
                    messageRange);
        }

        /**
         * Computes the 0-based inclusive message range for the given node.
         *
         * <p>Note that the range sometimes don't cover the entire source code of the node. For
         * example, in "int a = 0", we have a variable tree "int a", but we only want to report the
         * range of the identifier "a". This customizes the positions where we want the type
         * information to show.
         *
         * @param node The tree for which we want to compute the message range.
         * @return A message range corresponds to the node.
         */
        private MessageRange computeMessageRange(Tree node) {
            long startPos = sourcePositions.getStartPosition(currentRoot, node);
            long endPos = sourcePositions.getEndPosition(currentRoot, node);
            if (startPos == Diagnostic.NOPOS || endPos == Diagnostic.NOPOS) {
                // node doesn't exist in source file
                return null;
            }

            LineMap lineMap = currentRoot.getLineMap();
            startPos = ((JCTree) node).getPreferredPosition();
            long startLine = lineMap.getLineNumber(startPos);
            long startCol = lineMap.getColumnNumber(startPos);
            long endLine = startLine;
            long endCol;

            // We are decreasing endCol by 1 because we want it to be inclusive
            switch (node.getKind()) {
                case IDENTIFIER:
                    endCol = startCol + ((IdentifierTree) node).getName().length() - 1;
                    break;
                case VARIABLE:
                    endCol = startCol + ((VariableTree) node).getName().length() - 1;
                    break;
                case MEMBER_SELECT:
                    // The preferred start column of MemberSelectTree locates the "."
                    // character before the member identifier. So we increase startCol
                    // by 1 to point to the start of the member identifier.
                    startCol += 1;
                    endCol = startCol + ((MemberSelectTree) node).getIdentifier().length() - 1;
                    break;
                case MEMBER_REFERENCE:
                    endCol = startCol + ((MemberReferenceTree) node).getName().length() - 1;
                    break;
                case TYPE_PARAMETER:
                    endCol = startCol + ((TypeParameterTree) node).getName().length() - 1;
                    break;
                case METHOD:
                    endCol = startCol + ((MethodTree) node).getName().length() - 1;
                    break;
                case METHOD_INVOCATION:
                    return computeMessageRange(((MethodInvocationTree) node).getMethodSelect());
                default:
                    endLine = lineMap.getLineNumber(endPos);
                    endCol = lineMap.getColumnNumber(endPos) - 1;
                    break;
            }

            // convert 1-based positions to 0-based positions
            return MessageRange.of(startLine - 1, startCol - 1, endLine - 1, endCol - 1);
        }

        @Override
        public Void visitIdentifier(IdentifierTree node, Void unused) {
            switch (TreeUtils.elementFromUse(node).getKind()) {
                case ENUM_CONSTANT:
                case FIELD:
                case PARAMETER:
                case LOCAL_VARIABLE:
                case EXCEPTION_PARAMETER:
                case RESOURCE_VARIABLE:
                case METHOD:
                case CONSTRUCTOR:
                    reportNodeType(node, factory.getAnnotatedType(node));
                    break;
                default:
                    break;
            }
            return super.visitIdentifier(node, unused);
        }

        @Override
        public Void visitVariable(VariableTree node, Void unused) {
            reportNodeType(node, factory.getAnnotatedTypeLhs(node));
            return super.visitVariable(node, unused);
        }

        @Override
        public Void visitLiteral(LiteralTree node, Void unused) {
            reportNodeType(node, factory.getAnnotatedType(node));
            return super.visitLiteral(node, unused);
        }

        @Override
        public Void visitMemberSelect(MemberSelectTree node, Void unused) {
            reportNodeType(node, factory.getAnnotatedType(node));
            return super.visitMemberSelect(node, unused);
        }

        @Override
        public Void visitMemberReference(MemberReferenceTree node, Void unused) {
            reportNodeType(node, factory.getAnnotatedType(node));
            return super.visitMemberReference(node, unused);
        }

        @Override
        public Void visitMethodInvocation(MethodInvocationTree node, Void unused) {
            reportNodeType(node, factory.methodFromUse(node).executableType);
            return super.visitMethodInvocation(node, unused);
        }

        @Override
        public Void visitTypeParameter(TypeParameterTree node, Void unused) {
            reportNodeType(node, factory.getAnnotatedTypeFromTypeTree(node));
            return super.visitTypeParameter(node, unused);
        }

        @Override
        public Void visitAssignment(AssignmentTree node, Void unused) {
            return super.visitAssignment(node, unused);
        }

        @Override
        public Void visitMethod(MethodTree node, Void unused) {
            reportNodeType(node, factory.getAnnotatedType(node));
            return super.visitMethod(node, unused);
        }

        @Override
        public Void visitUnary(UnaryTree node, Void unused) {
            // TODO: how to implement this method correctly?
            // TODO: try store after
            return super.visitUnary(node, unused);
        }

        @Override
        public Void visitBinary(BinaryTree node, Void unused) {
            // TODO: how to implement this method correctly?
            return super.visitBinary(node, unused);
        }

        @Override
        public Void visitClass(ClassTree node, Void unused) {
            @SuppressWarnings("interning:not.interned")
            boolean isNestedClass = node != classTree;
            if (isNestedClass) {
                return null;
            }
            return super.visitClass(node, unused);
        }
    }
}
