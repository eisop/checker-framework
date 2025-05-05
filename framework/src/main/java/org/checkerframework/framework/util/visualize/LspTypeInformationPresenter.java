package org.checkerframework.framework.util.visualize;

import com.sun.source.tree.ClassTree;
import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.LineMap;
import com.sun.source.tree.MemberReferenceTree;
import com.sun.source.tree.MemberSelectTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.TypeParameterTree;
import com.sun.source.tree.VariableTree;
import com.sun.source.util.SourcePositions;
import com.sun.tools.javac.tree.JCTree;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.framework.type.AnnotatedTypeFactory;
import org.checkerframework.framework.type.AnnotatedTypeMirror;

import javax.tools.Diagnostic;

/**
 * Presents formatted type information for various AST trees in a class.
 *
 * <p>The formatted type information is designed to be visualized by editors and IDEs that support
 * the Language Server Protocol (LSP).
 */
public class LspTypeInformationPresenter extends AbstractTypeInformationPresenter {

    /**
     * Constructs a presenter for the given factory.
     *
     * @param atypeFactory the AnnotatedTypeFactory for the current analysis
     */
    public LspTypeInformationPresenter(AnnotatedTypeFactory atypeFactory) {
        super(atypeFactory);
    }

    @Override
    protected TypeInformationReporter createTypeInformationReporter(ClassTree tree) {
        return new LspTypeInformationReporter(tree);
    }

    /** Type information reporter that uses a format suitable for the LSP server. */
    protected class LspTypeInformationReporter extends TypeInformationReporter {
        /** Computes positions of a sub-tree. */
        protected final SourcePositions sourcePositions;

        /**
         * Constructor.
         *
         * @param classTree the {@link ClassTree}
         */
        LspTypeInformationReporter(ClassTree classTree) {
            super(classTree);
            this.sourcePositions = atypeFactory.getTreeUtils().getSourcePositions();
        }

        /**
         * Reports a diagnostic message indicating the range corresponding to the given tree has the
         * given type. Specifically, the message has key "lsp.type.information", and it contains the
         * name of the checker, the given occurrenceKind, the given type, and the computed message
         * range for the tree. If the tree is an artificial tree, this does not report anything.
         *
         * @param tree the tree that is used to find the corresponding range to report
         * @param type the type that we are going to display
         * @param occurrenceKind the kind of the given type
         */
        @Override
        protected void reportTreeType(
                Tree tree, AnnotatedTypeMirror type, TypeOccurrenceKind occurrenceKind) {
            TypeOccurrenceRange messageRange = computeTypeOccurrenceRange(tree);
            if (messageRange == null) {
                // Don't report if the tree can't be found in the source file.
                // Please check the implementation of computeTypeOccurrenceRange for
                // more details.
                return;
            }

            checker.reportError(
                    tree,
                    "lsp.type.information",
                    checker.getClass().getSimpleName(),
                    occurrenceKind,
                    typeFormatter.format(type),
                    messageRange);
        }

        /**
         * Computes the 0-based inclusive message range for the given tree.
         *
         * <p>Note that the range sometimes don't cover the entire source code of the tree. For
         * example, in "int a = 0", we have a variable tree "int a", but we only want to report the
         * range of the identifier "a". This customizes the positions where we want the type
         * information to show.
         *
         * @param tree the tree for which we want to compute the message range
         * @return a message range corresponds to the tree
         */
        protected @Nullable TypeOccurrenceRange computeTypeOccurrenceRange(Tree tree) {
            long startPos = sourcePositions.getStartPosition(currentRoot, tree);
            long endPos = sourcePositions.getEndPosition(currentRoot, tree);
            if (startPos == Diagnostic.NOPOS || endPos == Diagnostic.NOPOS) {
                // The tree doesn't exist in the source file.
                // For example, a class tree may contain a child that represents
                // a default constructor which is not explicitly written out in
                // the source file.
                // For this kind of trees, there's no way to compute their range
                // in the source file.
                return null;
            }

            LineMap lineMap = currentRoot.getLineMap();
            startPos = ((JCTree) tree).getPreferredPosition();
            long startLine = lineMap.getLineNumber(startPos);
            long startCol = lineMap.getColumnNumber(startPos);
            long endLine = startLine;
            long endCol;

            // We are decreasing endCol by 1 because we want it to be inclusive
            switch (tree.getKind()) {
                case UNARY_PLUS:
                case UNARY_MINUS:
                case BITWISE_COMPLEMENT:
                case LOGICAL_COMPLEMENT:
                case MULTIPLY:
                case DIVIDE:
                case REMAINDER:
                case PLUS:
                case MINUS:
                case AND:
                case XOR:
                case OR:
                case ASSIGNMENT:
                case LESS_THAN:
                case GREATER_THAN:
                    // 1-character operators
                    endCol = startCol;
                    break;
                case PREFIX_INCREMENT:
                case PREFIX_DECREMENT:
                case POSTFIX_INCREMENT:
                case POSTFIX_DECREMENT:
                case LEFT_SHIFT:
                case RIGHT_SHIFT:
                case CONDITIONAL_AND:
                case CONDITIONAL_OR:
                case MULTIPLY_ASSIGNMENT:
                case DIVIDE_ASSIGNMENT:
                case REMAINDER_ASSIGNMENT:
                case PLUS_ASSIGNMENT:
                case MINUS_ASSIGNMENT:
                case AND_ASSIGNMENT:
                case XOR_ASSIGNMENT:
                case OR_ASSIGNMENT:
                case LESS_THAN_EQUAL:
                case GREATER_THAN_EQUAL:
                case EQUAL_TO:
                case NOT_EQUAL_TO:
                    // 2-character operators
                    endCol = startCol + 1;
                    break;
                case UNSIGNED_RIGHT_SHIFT:
                case LEFT_SHIFT_ASSIGNMENT:
                case RIGHT_SHIFT_ASSIGNMENT:
                    // 3-character operators
                    endCol = startCol + 2;
                    break;
                case UNSIGNED_RIGHT_SHIFT_ASSIGNMENT:
                    // 4-character operators
                    endCol = startCol + 3;
                    break;
                case IDENTIFIER:
                    endCol = startCol + ((IdentifierTree) tree).getName().length() - 1;
                    break;
                case VARIABLE:
                    endCol = startCol + ((VariableTree) tree).getName().length() - 1;
                    break;
                case MEMBER_SELECT:
                    // The preferred start column of MemberSelectTree locates the "."
                    // character before the member identifier. So we increase startCol
                    // by 1 to point to the start of the member identifier.
                    startCol += 1;
                    endCol = startCol + ((MemberSelectTree) tree).getIdentifier().length() - 1;
                    break;
                case MEMBER_REFERENCE:
                    MemberReferenceTree memberReferenceTree = (MemberReferenceTree) tree;

                    final int identifierLength;
                    if (memberReferenceTree.getMode() == MemberReferenceTree.ReferenceMode.NEW) {
                        identifierLength = 3;
                    } else {
                        identifierLength = memberReferenceTree.getName().length();
                    }

                    // The preferred position of a MemberReferenceTree is the head of
                    // its expression, which is not ideal. Here we compute the range of
                    // its identifier using the end position and the length of the identifier.
                    endLine = lineMap.getLineNumber(endPos);
                    endCol = lineMap.getColumnNumber(endPos) - 1;
                    startLine = endLine;
                    startCol = endCol - identifierLength + 1;
                    break;
                case TYPE_PARAMETER:
                    endCol = startCol + ((TypeParameterTree) tree).getName().length() - 1;
                    break;
                case METHOD:
                    endCol = startCol + ((MethodTree) tree).getName().length() - 1;
                    break;
                case METHOD_INVOCATION:
                    return computeTypeOccurrenceRange(
                            ((MethodInvocationTree) tree).getMethodSelect());
                default:
                    endLine = lineMap.getLineNumber(endPos);
                    endCol = lineMap.getColumnNumber(endPos) - 1;
                    break;
            }

            // convert 1-based positions to 0-based positions
            return TypeOccurrenceRange.of(startLine - 1, startCol - 1, endLine - 1, endCol - 1);
        }
    }
}
