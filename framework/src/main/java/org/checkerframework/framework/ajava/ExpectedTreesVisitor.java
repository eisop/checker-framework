package org.checkerframework.framework.ajava;

import com.sun.source.tree.AnnotatedTypeTree;
import com.sun.source.tree.AnnotationTree;
import com.sun.source.tree.ArrayTypeTree;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.DoWhileLoopTree;
import com.sun.source.tree.EmptyStatementTree;
import com.sun.source.tree.ExpressionStatementTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.ForLoopTree;
import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.IfTree;
import com.sun.source.tree.ImportTree;
import com.sun.source.tree.LambdaExpressionTree;
import com.sun.source.tree.MemberSelectTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.ModifiersTree;
import com.sun.source.tree.NewArrayTree;
import com.sun.source.tree.NewClassTree;
import com.sun.source.tree.StatementTree;
import com.sun.source.tree.SwitchTree;
import com.sun.source.tree.SynchronizedTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.VariableTree;
import com.sun.source.tree.WhileLoopTree;

import org.checkerframework.javacutil.TreeUtils;
import org.checkerframework.javacutil.TreeUtilsAfterJava11.BindingPatternUtils;
import org.checkerframework.javacutil.TreeUtilsAfterJava11.SwitchExpressionUtils;

import java.util.HashSet;
import java.util.Set;

/**
 * After this visitor visits a tree, {@link #getTrees} returns all the trees that should match with
 * some JavaParser node. Some trees shouldn't be matched with a JavaParser node because there isn't
 * a corresponding JavaParser node. These trees are excluded.
 *
 * <p>The primary purpose is to test the {@link JointJavacJavaParserVisitor} class when the {@code
 * -AajavaChecks} flag is used. That class traverses a javac tree and JavaParser AST simultaneously,
 * so the trees this class stores can be used to test if the entirety of the javac tree was visited.
 */
public class ExpectedTreesVisitor extends TreeScannerWithDefaults {
    /** The set of trees that should be matched to a JavaParser node when visiting both. */
    private Set<Tree> trees = new HashSet<>();

    /**
     * Returns the visited trees that should match to some JavaParser node.
     *
     * @return the visited trees that should match to some JavaParser node
     */
    public Set<Tree> getTrees() {
        return trees;
    }

    /**
     * Records that {@code tree} should have a corresponding JavaParser node.
     *
     * @param tree the tree to record
     */
    @Override
    public void defaultAction(Tree tree) {
        trees.add(tree);
    }

    @Override
    public Void visitAnnotation(AnnotationTree tree, Void p) {
        // Skip annotations because ajava files are not required to have the same annotations as
        // their corresponding Java files.
        return null;
    }

    @Override
    public Void visitBindingPattern17(Tree tree, Void p) {
        super.visitBindingPattern17(tree, p);
        // JavaParser doesn't have a node for the VariableTree.
        trees.remove(BindingPatternUtils.getVariable(tree));
        return null;
    }

    @Override
    public Void visitClass(ClassTree tree, Void p) {
        defaultAction(tree);
        scan(tree.getModifiers(), p);
        scan(tree.getTypeParameters(), p);
        scan(tree.getExtendsClause(), p);
        scan(tree.getImplementsClause(), p);
        if (tree.getKind() == Tree.Kind.ENUM) {
            // Enum constants expand to a VariableTree like
            //    public static final MY_ENUM_CONSTANT = new MyEnum(args ...)
            // The constructor invocation in the initializer has no corresponding JavaParser node,
            // so this removes those invocations. This doesn't remove any trees that should be
            // matched to a JavaParser node, because it's illegal to explicitly construct an
            // instance of an enum.
            for (Tree member : tree.getMembers()) {
                member.accept(this, p);
                if (!(member instanceof VariableTree)) {
                    continue;
                }

                VariableTree variable = (VariableTree) member;
                ExpressionTree initializer = variable.getInitializer();
                if (!(initializer instanceof NewClassTree)) {
                    continue;
                }

                NewClassTree constructor = (NewClassTree) initializer;
                if (!(constructor.getIdentifier() instanceof IdentifierTree)) {
                    continue;
                }

                IdentifierTree name = (IdentifierTree) constructor.getIdentifier();
                if (name.getName().contentEquals(tree.getSimpleName())) {
                    trees.remove(variable.getType());
                    trees.remove(constructor);
                    trees.remove(constructor.getIdentifier());
                }
            }
        } else if (TreeUtils.isRecordTree(tree)) {
            // A record like:
            //   record MyRec(String myField) {}
            // will be expanded by javac to:
            //   class MyRec {
            //      MyRec(String myField) {
            //        super();
            //      }
            //      private final String myField;
            //   }
            // So the constructor and the field declarations have no matching trees in the
            // JavaParser AST node, and we must remove those trees (and their subtrees) from the
            // `trees` field.
            TreeScannerWithDefaults removeAllVisitor =
                    new TreeScannerWithDefaults() {
                        @Override
                        public void defaultAction(Tree tree) {
                            trees.remove(tree);
                        }
                    };
            for (Tree member : tree.getMembers()) {
                scan(member, p);
                if (TreeUtils.isAutoGeneratedRecordMember(member)) {
                    member.accept(removeAllVisitor, null);
                } else {
                    // If the user declares a compact canonical constructor, javac will
                    // automatically fill in the parameters.
                    // These trees also don't have a match:
                    if (member instanceof MethodTree) {
                        MethodTree methodTree = (MethodTree) member;
                        if (TreeUtils.isCompactCanonicalRecordConstructor(methodTree)) {
                            for (VariableTree canonicalParameter : methodTree.getParameters()) {
                                canonicalParameter.accept(removeAllVisitor, null);
                            }
                        }
                    }
                }
            }
        } else {
            scan(tree.getMembers(), p);
        }

        return null;
    }

    @Override
    public Void visitExpressionStatement(ExpressionStatementTree tree, Void p) {
        // Javac inserts calls to super() at the start of constructors with no this or super call.
        // These don't have matching JavaParser nodes.
        if (JointJavacJavaParserVisitor.isDefaultSuperConstructorCall(tree)) {
            return null;
        }

        // Whereas synthetic constructors should be skipped, regular super() and this() should still
        // be added. JavaParser has no expression statement surrounding these, so remove the
        // expression statement itself.
        Void result = super.visitExpressionStatement(tree, p);
        if (tree.getExpression() instanceof MethodInvocationTree) {
            MethodInvocationTree invocation = (MethodInvocationTree) tree.getExpression();
            if (invocation.getMethodSelect() instanceof IdentifierTree) {
                IdentifierTree identifier = (IdentifierTree) invocation.getMethodSelect();
                if (identifier.getName().contentEquals("this")
                        || identifier.getName().contentEquals("super")) {
                    trees.remove(tree);
                    trees.remove(identifier);
                }
            }
        }

        return result;
    }

    @Override
    public Void visitForLoop(ForLoopTree tree, Void p) {
        // Javac nests a for loop's updates in expression statements but JavaParser stores the
        // statements directly, so remove the expression statements.
        Void result = super.visitForLoop(tree, p);
        for (StatementTree initializer : tree.getInitializer()) {
            trees.remove(initializer);
        }

        for (ExpressionStatementTree update : tree.getUpdate()) {
            trees.remove(update);
        }

        return result;
    }

    @Override
    public Void visitSwitch(SwitchTree tree, Void p) {
        super.visitSwitch(tree, p);
        // javac surrounds switch expression in a ParenthesizedTree but JavaParser does not.
        trees.remove(tree.getExpression());
        return null;
    }

    @Override
    public Void visitSwitchExpression17(Tree tree, Void p) {
        super.visitSwitchExpression17(tree, p);
        // javac surrounds switch expression in a ParenthesizedTree but JavaParser does not.
        trees.remove(SwitchExpressionUtils.getExpression(tree));
        return null;
    }

    @Override
    public Void visitSynchronized(SynchronizedTree tree, Void p) {
        super.visitSynchronized(tree, p);
        // javac surrounds synchronized expressions in a ParenthesizedTree but JavaParser does not.
        trees.remove(tree.getExpression());
        return null;
    }

    @Override
    public Void visitIf(IfTree tree, Void p) {
        // In an if statement, javac stores the condition as a parenthesized expression, which has
        // no corresponding JavaParserNode, so remove the parenthesized expression, but not its
        // child.
        Void result = super.visitIf(tree, p);
        trees.remove(tree.getCondition());
        return result;
    }

    @Override
    public Void visitImport(ImportTree tree, Void p) {
        // Javac stores an import like a.* as a member select, but JavaParser just stores "a", so
        // don't add the member select in that case.
        if (tree.getQualifiedIdentifier() instanceof MemberSelectTree) {
            MemberSelectTree memberSelect = (MemberSelectTree) tree.getQualifiedIdentifier();
            if (memberSelect.getIdentifier().contentEquals("*")) {
                memberSelect.getExpression().accept(this, p);
                return null;
            }
        }

        return super.visitImport(tree, p);
    }

    @Override
    public Void visitMethod(MethodTree tree, Void p) {
        // Synthetic default constructors don't have matching JavaParser nodes. Conservatively skip
        // nullary (no-argument) constructor calls, even if they may not be synthetic.
        if (JointJavacJavaParserVisitor.isNoArgumentConstructor(tree)) {
            return null;
        }

        Void result = super.visitMethod(tree, p);
        // A varargs parameter like String... is converted to String[], where the array type doesn't
        // have a corresponding JavaParser AST node. Conservatively skip the array type (but not the
        // component type) if it's the last argument.
        if (!tree.getParameters().isEmpty()) {
            VariableTree last = tree.getParameters().get(tree.getParameters().size() - 1);
            if (last.getType() instanceof ArrayTypeTree) {
                trees.remove(last.getType());
            }

            if (last.getType() instanceof AnnotatedTypeTree) {
                AnnotatedTypeTree annotatedType = (AnnotatedTypeTree) last.getType();
                if (annotatedType.getUnderlyingType() instanceof ArrayTypeTree) {
                    trees.remove(annotatedType);
                    trees.remove(annotatedType.getUnderlyingType());
                }
            }
        }

        return result;
    }

    @Override
    public Void visitMethodInvocation(MethodInvocationTree tree, Void p) {
        Void result = super.visitMethodInvocation(tree, p);
        // In a method invocation like myObject.myMethod(), the method invocation stores
        // myObject.myMethod as its own MemberSelectTree which has no corresponding JavaParserNode.
        if (tree.getMethodSelect() instanceof MemberSelectTree) {
            trees.remove(tree.getMethodSelect());
        }

        return result;
    }

    @Override
    public Void visitModifiers(ModifiersTree tree, Void p) {
        // Don't add ModifierTrees or children because they have no corresponding JavaParser node.
        return null;
    }

    @Override
    public Void visitNewArray(NewArrayTree tree, Void p) {
        // Skip array initialization because it's not implemented yet.
        return null;
    }

    @Override
    public Void visitNewClass(NewClassTree tree, Void p) {
        defaultAction(tree);

        if (tree.getEnclosingExpression() != null) {
            tree.getEnclosingExpression().accept(this, p);
        }

        tree.getIdentifier().accept(this, p);
        for (Tree typeArgument : tree.getTypeArguments()) {
            typeArgument.accept(this, p);
        }

        for (Tree arg : tree.getTypeArguments()) {
            arg.accept(this, p);
        }

        if (tree.getClassBody() == null) {
            return null;
        }

        // Anonymous class bodies require special handling. There isn't a corresponding JavaParser
        // node, and synthetic constructors must be skipped.
        ClassTree body = tree.getClassBody();
        scan(body.getModifiers(), p);
        scan(body.getTypeParameters(), p);
        scan(body.getImplementsClause(), p);
        for (Tree member : body.getMembers()) {
            // Constructors cannot be declared in an anonymous class, so don't add them.
            if (member instanceof MethodTree) {
                MethodTree methodTree = (MethodTree) member;
                if (methodTree.getName().contentEquals("<init>")) {
                    continue;
                }
            }

            member.accept(this, p);
        }

        return null;
    }

    @Override
    public Void visitLambdaExpression(LambdaExpressionTree tree, Void p) {
        for (VariableTree parameter : tree.getParameters()) {
            // Programmers may omit parameter types for lambda expressions. When not specified,
            // javac infers them but JavaParser uses UnknownType. Conservatively, don't add
            // parameter types for lambda expressions.
            scan(parameter.getModifiers(), p);
            scan(parameter.getNameExpression(), p);
            assert parameter.getInitializer() == null;
        }

        scan(tree.getBody(), p);
        return null;
    }

    @Override
    public Void visitWhileLoop(WhileLoopTree tree, Void p) {
        super.visitWhileLoop(tree, p);
        // javac surrounds while loop conditions in a ParenthesizedTree but JavaParser does not.
        trees.remove(tree.getCondition());
        return null;
    }

    @Override
    public Void visitDoWhileLoop(DoWhileLoopTree tree, Void p) {
        super.visitDoWhileLoop(tree, p);
        // javac surrounds while loop conditions in a ParenthesizedTree but JavaParser does not.
        trees.remove(tree.getCondition());
        return null;
    }

    @Override
    public Void visitVariable(VariableTree tree, Void p) {
        // Javac expands the keyword "var" in a variable declaration to its inferred type.
        // JavaParser has a special "var" construct, so they won't match. If a javac type was
        // generated this way, then it won't have a position in source code so in that case we don't
        // add it.
        if (TreeUtils.isVariableTreeDeclaredUsingVar(tree)) {
            return null;
        }

        return super.visitVariable(tree, p);
    }

    @Override
    public Void visitYield17(Tree tree, Void p) {
        // JavaParser does not parse yields correctly:
        // https://github.com/javaparser/javaparser/issues/3364
        // So skip yields.
        return null;
    }

    @Override
    public Void visitEmptyStatement(EmptyStatementTree tree, Void p) {
        // JavaParser doesn't seem to include completely-empty classes.
        // See https://github.com/typetools/checker-framework/issues/6570
        // for a discussion of our motivation for excluding empty statements.
        // Also, empty statements don't have semantics, so it's okay if there
        // are arbitrarily-many extra empty statements in one of the inputs.
        return null;
    }
}
