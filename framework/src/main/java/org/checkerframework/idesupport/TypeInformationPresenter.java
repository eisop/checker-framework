package org.checkerframework.idesupport;

import com.sun.source.tree.AssignmentTree;
import com.sun.source.tree.BinaryTree;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.ExpressionTree;
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

public class TypeInformationPresenter {

    private final GenericAnnotatedTypeFactory<?, ?, ?, ?> factory;

    private final AnnotatedTypeFormatter typeFormatter;

    public TypeInformationPresenter(GenericAnnotatedTypeFactory<?, ?, ?, ?> factory) {
        this.factory = factory;
        this.typeFormatter = new DefaultAnnotatedTypeFormatter(true, true);
    }

    public void process(ClassTree tree) {
        TypeInformationReporter visitor = new TypeInformationReporter(tree);
        visitor.scan(tree, null);
    }

    private static class NodePosition {
        private final long startLine;
        private final long startCol;
        private final long endLine;
        private final long endCol;

        private NodePosition(long startLine, long startCol, long endLine, long endCol) {
            this.startLine = startLine;
            this.startCol = startCol;
            this.endLine = endLine;
            this.endCol = endCol;
        }

        static NodePosition of(long line, long col, long endLine, long endCol) {
            return new NodePosition(line, col, endLine, endCol);
        }

        @Override
        public String toString() {
            return String.format("(%d, %d, %d, %d)", startLine, startCol, endLine, endCol);
        }
    }

    private class TypeInformationReporter extends TreeScanner<Void, Void> {

        private final ClassTree classTree;

        private final CompilationUnitTree currentRoot;

        private final SourcePositions sourcePositions;

        public TypeInformationReporter(ClassTree classTree) {
            this.classTree = classTree;
            this.currentRoot = factory.getChecker().getPathToCompilationUnit().getCompilationUnit();
            this.sourcePositions = factory.getTreeUtils().getSourcePositions();
        }

        private void reportNodeType(Tree node, AnnotatedTypeMirror type) {
            NodePosition nodePosition = computeNodePosition(node);
            if (nodePosition == null) {
                // don't report if the node doesn't exist in source file
                return;
            }

            BaseTypeChecker checker = factory.getChecker();
            checker.reportError(
                    node,
                    "lsp.type.information",
                    checker.getClass().getSimpleName(),
                    typeFormatter.format(type),
                    nodePosition);
        }

        // Computes the inclusive start and end positions for the given node
        private NodePosition computeNodePosition(Tree node) {
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

            // Note: we are decreasing endCol by 1 because we want it to be inclusive
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
                    return computeNodePosition(((MethodInvocationTree) node).getMethodSelect());
                default:
                    endLine = lineMap.getLineNumber(endPos);
                    endCol = lineMap.getColumnNumber(endPos) - 1;
                    break;
            }
            return NodePosition.of(startLine, startCol, endLine, endCol);
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
            reportNodeType(node, factory.getAnnotatedType(node));
            return super.visitVariable(node, unused);
        }

        @Override
        public Void visitLiteral(LiteralTree node, Void unused) {
            reportNodeType(node, factory.getAnnotatedType(node));
            return super.visitLiteral(node, unused);
        }

        @Override
        public Void visitAssignment(AssignmentTree node, Void unused) {
            ExpressionTree lhs = node.getVariable();
            reportNodeType(lhs, factory.getAnnotatedTypeLhs(lhs));
            return super.visitAssignment(node, unused);
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
        public Void visitMethod(MethodTree node, Void unused) {
            reportNodeType(node, factory.getAnnotatedType(node));
            return super.visitMethod(node, unused);
        }

        @Override
        public Void visitUnary(UnaryTree node, Void unused) {
            // TODO: where to place the type info?
            return super.visitUnary(node, unused);
        }

        @Override
        public Void visitBinary(BinaryTree node, Void unused) {
            // TODO: where to place the type info?
            return super.visitBinary(node, unused);
        }

        @Override
        public Void visitClass(ClassTree node, Void unused) {
            if (node != classTree) {
                return null;
            }
            return super.visitClass(node, unused);
        }
    }
}
