package org.checkerframework.javacutil;

import com.sun.source.tree.AnnotatedTypeTree;
import com.sun.source.tree.AnnotationTree;
import com.sun.source.tree.ArrayAccessTree;
import com.sun.source.tree.AssignmentTree;
import com.sun.source.tree.BinaryTree;
import com.sun.source.tree.BlockTree;
import com.sun.source.tree.CaseTree;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.CompoundAssignmentTree;
import com.sun.source.tree.ExpressionStatementTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.InstanceOfTree;
import com.sun.source.tree.LiteralTree;
import com.sun.source.tree.MemberSelectTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.NewArrayTree;
import com.sun.source.tree.NewClassTree;
import com.sun.source.tree.ParameterizedTypeTree;
import com.sun.source.tree.ParenthesizedTree;
import com.sun.source.tree.PrimitiveTypeTree;
import com.sun.source.tree.StatementTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.TreeVisitor;
import com.sun.source.tree.TypeCastTree;
import com.sun.source.tree.TypeParameterTree;
import com.sun.source.tree.UnionTypeTree;
import com.sun.source.tree.VariableTree;
import com.sun.source.util.SimpleTreeVisitor;
import com.sun.tools.javac.code.Flags;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Symbol.MethodSymbol;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.code.TypeTag;
import com.sun.tools.javac.code.Types;
import com.sun.tools.javac.processing.JavacProcessingEnvironment;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.JCTree.JCAnnotatedType;
import com.sun.tools.javac.tree.JCTree.JCAnnotation;
import com.sun.tools.javac.tree.JCTree.JCBinary;
import com.sun.tools.javac.tree.JCTree.JCExpression;
import com.sun.tools.javac.tree.JCTree.JCExpressionStatement;
import com.sun.tools.javac.tree.JCTree.JCLambda;
import com.sun.tools.javac.tree.JCTree.JCLambda.ParameterKind;
import com.sun.tools.javac.tree.JCTree.JCLiteral;
import com.sun.tools.javac.tree.JCTree.JCMemberReference;
import com.sun.tools.javac.tree.JCTree.JCMethodDecl;
import com.sun.tools.javac.tree.JCTree.JCMethodInvocation;
import com.sun.tools.javac.tree.JCTree.JCNewArray;
import com.sun.tools.javac.tree.JCTree.JCNewClass;
import com.sun.tools.javac.tree.JCTree.JCTypeParameter;
import com.sun.tools.javac.tree.TreeInfo;
import com.sun.tools.javac.tree.TreeMaker;
import com.sun.tools.javac.util.Context;

import org.checkerframework.checker.interning.qual.PolyInterned;
import org.checkerframework.checker.nullness.qual.EnsuresNonNullIf;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.checker.signature.qual.FullyQualifiedName;
import org.checkerframework.dataflow.qual.Pure;
import org.plumelib.util.CollectionsPlume;
import org.plumelib.util.UniqueIdMap;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.StringJoiner;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Name;
import javax.lang.model.element.NestingKind;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.ExecutableType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.ElementFilter;

/**
 * Utility methods for analyzing a javac {@code Tree}.
 *
 * @see TreePathUtil
 */
// TODO: use JCP to add version-specific behavior
public final class TreeUtils {

  // Class cannot be instantiated.
  private TreeUtils() {
    throw new AssertionError("Class TreeUtils cannot be instantiated.");
  }

  /** Unique IDs for trees. */
  public static final UniqueIdMap<Tree> treeUids = new UniqueIdMap<>();

  /** Whether we are running on at least Java 12. */
  private static final boolean atLeastJava12;

  /** Whether we are running on at least Java 13. */
  private static final boolean atLeastJava13;

  /** Whether we are running on at least Java 16. */
  private static final boolean atLeastJava16;

  /** The CaseTree.getExpression method for Java up to 11; null otherwise. */
  private static final @Nullable Method CASETREE_GETEXPRESSION;

  /** The CaseTree.getExpressions method for Java 12 and higher; null otherwise. */
  private static final @Nullable Method CASETREE_GETEXPRESSIONS;

  /** The CaseTree.getBody method for Java 12 and higher; null otherwise. */
  private static final @Nullable Method CASETREE_GETBODY;

  /** The SwitchExpressionTree.getExpression method for Java 12 and higher; null otherwise. */
  private static final @Nullable Method SWITCHEXPRTREE_GETEXPRESSION;

  /** The SwitchExpressionTree.getExpression method for Java 12 and higher; null otherwise. */
  private static final @Nullable Method SWITCHEXPRTREE_GETCASES;

  /** The YieldTree.getValue method for Java 13 and higher; null otherwise. */
  private static final @Nullable Method YIELDTREE_GETVALUE;

  /** The InstanceOfTree.getPattern method for Java 16 and higher; null otherwise. */
  private static final @Nullable Method INSTANCEOFTREE_GETPATTERN;

  /** The BindingPatternTree.getVariable method for Java 16 and higher; null otherwise. */
  private static final @Nullable Method BINDINGPATTERNTREE_GETVARIABLE;

  /** The value of Flags.RECORD which does not exist in Java 9 or 11. */
  private static final long Flags_RECORD = 2305843009213693952L;

  /** The set of tree kinds that can be categorized as binary comparison. */
  private static final Set<Tree.Kind> BINARY_COMPARISON_TREE_KINDS;

  static {
    final SourceVersion latestSource = SourceVersion.latest();
    SourceVersion java12;
    try {
      java12 = SourceVersion.valueOf("RELEASE_12");
    } catch (IllegalArgumentException e) {
      java12 = null;
    }
    atLeastJava12 = java12 != null && latestSource.ordinal() >= java12.ordinal();

    SourceVersion java13;
    try {
      java13 = SourceVersion.valueOf("RELEASE_13");
    } catch (IllegalArgumentException e) {
      java13 = null;
    }
    atLeastJava13 = java13 != null && latestSource.ordinal() >= java13.ordinal();

    SourceVersion java16;
    try {
      java16 = SourceVersion.valueOf("RELEASE_16");
    } catch (IllegalArgumentException e) {
      java16 = null;
    }
    atLeastJava16 = java16 != null && latestSource.ordinal() >= java16.ordinal();

    try {
      // TODO: profile and see whether doing all these here has a performance impact.
      // If so, move to lazily setting the fields.
      if (atLeastJava12) {
        CASETREE_GETEXPRESSIONS = CaseTree.class.getDeclaredMethod("getExpressions");
        CASETREE_GETEXPRESSION = null;
        CASETREE_GETBODY = CaseTree.class.getDeclaredMethod("getBody");

        Class<?> switchExpressionClass = Class.forName("com.sun.source.tree.SwitchExpressionTree");
        SWITCHEXPRTREE_GETEXPRESSION = switchExpressionClass.getMethod("getExpression");
        SWITCHEXPRTREE_GETCASES = switchExpressionClass.getMethod("getCases");
      } else {
        CASETREE_GETEXPRESSION = CaseTree.class.getDeclaredMethod("getExpression");
        CASETREE_GETEXPRESSIONS = null;
        CASETREE_GETBODY = null;

        SWITCHEXPRTREE_GETEXPRESSION = null;
        SWITCHEXPRTREE_GETCASES = null;
      }
      if (atLeastJava13) {
        Class<?> yieldTreeClass = Class.forName("com.sun.source.tree.YieldTree");
        YIELDTREE_GETVALUE = yieldTreeClass.getMethod("getValue");
      } else {
        YIELDTREE_GETVALUE = null;
      }
      if (atLeastJava16) {
        INSTANCEOFTREE_GETPATTERN = InstanceOfTree.class.getMethod("getPattern");
        Class<?> bindingPatternClass = Class.forName("com.sun.source.tree.BindingPatternTree");
        BINDINGPATTERNTREE_GETVARIABLE = bindingPatternClass.getMethod("getVariable");
      } else {
        INSTANCEOFTREE_GETPATTERN = null;
        BINDINGPATTERNTREE_GETVARIABLE = null;
      }
    } catch (ClassNotFoundException | NoSuchMethodException e) {
      Error err = new AssertionError("Unexpected error in TreeUtils static initializer");
      err.initCause(e);
      throw err;
    }

    BINARY_COMPARISON_TREE_KINDS =
        EnumSet.of(
            Tree.Kind.EQUAL_TO,
            Tree.Kind.NOT_EQUAL_TO,
            Tree.Kind.LESS_THAN,
            Tree.Kind.GREATER_THAN,
            Tree.Kind.LESS_THAN_EQUAL,
            Tree.Kind.GREATER_THAN_EQUAL);
  }

  /**
   * Checks if the provided method is a constructor method or no.
   *
   * @param tree a tree defining the method
   * @return true iff tree describes a constructor
   */
  public static boolean isConstructor(final MethodTree tree) {
    return tree.getName().contentEquals("<init>");
  }

  /**
   * Checks if the method invocation is a call to super.
   *
   * @param tree a tree defining a method invocation
   * @return true iff tree describes a call to super
   */
  public static boolean isSuperConstructorCall(MethodInvocationTree tree) {
    return isNamedMethodCall("super", tree);
  }

  /**
   * Checks if the method invocation is a call to "this".
   *
   * @param tree a tree defining a method invocation
   * @return true iff tree describes a call to this
   */
  public static boolean isThisConstructorCall(MethodInvocationTree tree) {
    return isNamedMethodCall("this", tree);
  }

  /**
   * Checks if the method call is a call to the given method name.
   *
   * @param name a method name
   * @param tree a tree defining a method invocation
   * @return true iff tree is a call to the given method
   */
  private static boolean isNamedMethodCall(String name, MethodInvocationTree tree) {
    return getMethodName(tree.getMethodSelect()).contentEquals(name);
  }

  /**
   * Returns true if the tree is a tree that 'looks like' either an access of a field or an
   * invocation of a method that are owned by the same accessing instance.
   *
   * <p>It would only return true if the access tree is of the form:
   *
   * <pre>
   *   field
   *   this.field
   *
   *   method()
   *   this.method()
   * </pre>
   *
   * It does not perform any semantical check to differentiate between fields and local variables;
   * local methods or imported static methods.
   *
   * @param tree expression tree representing an access to object member
   * @return {@code true} iff the member is a member of {@code this} instance
   */
  public static boolean isSelfAccess(final ExpressionTree tree) {
    ExpressionTree tr = TreeUtils.withoutParens(tree);
    // If method invocation check the method select
    if (tr.getKind() == Tree.Kind.ARRAY_ACCESS) {
      return false;
    }

    if (tree.getKind() == Tree.Kind.METHOD_INVOCATION) {
      tr = ((MethodInvocationTree) tree).getMethodSelect();
    }
    tr = TreeUtils.withoutParens(tr);
    if (tr.getKind() == Tree.Kind.TYPE_CAST) {
      tr = ((TypeCastTree) tr).getExpression();
    }
    tr = TreeUtils.withoutParens(tr);

    if (tr.getKind() == Tree.Kind.IDENTIFIER) {
      return true;
    }

    if (tr.getKind() == Tree.Kind.MEMBER_SELECT) {
      tr = ((MemberSelectTree) tr).getExpression();
      if (tr.getKind() == Tree.Kind.IDENTIFIER) {
        Name ident = ((IdentifierTree) tr).getName();
        return ident.contentEquals("this") || ident.contentEquals("super");
      }
    }

    return false;
  }

  /**
   * If the given tree is a parenthesized tree, return the enclosed non-parenthesized tree.
   * Otherwise, return the same tree.
   *
   * @param tree an expression tree
   * @return the outermost non-parenthesized tree enclosed by the given tree
   */
  @SuppressWarnings("interning:return.type.incompatible") // polymorphism implementation
  public static @PolyInterned ExpressionTree withoutParens(
      final @PolyInterned ExpressionTree tree) {
    ExpressionTree t = tree;
    while (t.getKind() == Tree.Kind.PARENTHESIZED) {
      t = ((ParenthesizedTree) t).getExpression();
    }
    return t;
  }

  /**
   * If the given tree is a parenthesized tree or cast tree, return the enclosed non-parenthesized,
   * non-cast tree. Otherwise, return the same tree.
   *
   * @param tree an expression tree
   * @return the outermost non-parenthesized non-cast tree enclosed by the given tree
   */
  @SuppressWarnings("interning:return") // polymorphism implementation
  public static @PolyInterned ExpressionTree withoutParensOrCasts(
      final @PolyInterned ExpressionTree tree) {
    ExpressionTree t = withoutParens(tree);
    while (t.getKind() == Tree.Kind.TYPE_CAST) {
      t = withoutParens(((TypeCastTree) t).getExpression());
    }
    return t;
  }

  // Obtaining Elements from Trees.
  // There are three sets of methods:
  //  * use elementFromDeclaration whenever the tree is a declaration
  //  * use elementFromUse when the tree is a use
  //  * use elementFromTree in other cases; note that it may return null
  // This section of the file groups methods by their receiver type; that is, it puts all
  // `elementFrom*(FooTree)` methods together.

  /**
   * Returns the type element corresponding to the given class declaration.
   *
   * @param tree class declaration
   * @return the element for the given class
   */
  public static TypeElement elementFromDeclaration(ClassTree tree) {
    TypeElement result = (TypeElement) TreeInfo.symbolFor((JCTree) tree);
    if (result == null) {
      throw new BugInCF("null element for class tree %s", tree);
    }
    return result;
  }

  /**
   * Returns the type element corresponding to the given class declaration.
   *
   * <p>The TypeElement may be null for an anonymous class.
   *
   * @param tree the {@link Tree} node to get the symbol for
   * @return the {@link Symbol} for the given tree, or null if one could not be found
   * @deprecated use elementFromDeclaration
   */
  @Deprecated // not for removal; retain to prevent calls to this overload
  @Pure
  public static @Nullable TypeElement elementFromTree(ClassTree tree) {
    return elementFromDeclaration(tree);
  }

  /**
   * Returns the type element corresponding to the given class declaration.
   *
   * @param tree the {@link Tree} node to get the symbol for
   * @return the {@link Symbol} for the given tree, or null if one could not be found
   * @deprecated use elementFromDeclaration
   */
  @Deprecated // not for removal; retain to prevent calls to this overload
  @Pure
  public static @Nullable TypeElement elementFromUse(ClassTree tree) {
    return elementFromDeclaration(tree);
  }

  /**
   * Returns the element corresponding to the given tree.
   *
   * @param tree the tree corresponding to a use of an element
   * @return the element for the corresponding declaration, {@code null} otherwise
   * @deprecated use elementFromUse or elementFromTree
   */
  @Pure
  @Deprecated // not for removal; retain to prevent calls to this overload
  public static @Nullable Element elementFromDeclaration(ExpressionTree tree) {
    return TreeUtils.elementFromUse(tree);
  }

  /**
   * Returns the element corresponding to the given tree.
   *
   * @param tree the tree corresponding to a use of an element
   * @return the element for the corresponding declaration, {@code null} otherwise
   */
  @Pure
  public static @Nullable Element elementFromTree(ExpressionTree tree) {
    return TreeUtils.elementFromTree((Tree) tree);
  }

  /**
   * Returns the element corresponding to the given use. The given tree must be a use of an element;
   * for example, it cannot be a binary expression.
   *
   * @param tree the tree, which must be a use of an element
   * @return the element for the given use
   */
  @Pure
  public static Element elementFromUse(ExpressionTree tree) {
    Element result = TreeUtils.elementFromTree(tree);
    if (result == null) {
      throw new BugInCF(
          "argument to elementFromUse() has no element: %s [%s]", tree, tree.getClass());
    }
    return result;
  }

  /**
   * Returns the VariableElement corresponding to the given use.
   *
   * @param tree the tree corresponding to a use of a VariableElement
   * @return the element for the corresponding declaration, {@code null} otherwise
   */
  @Pure
  public static VariableElement variableElementFromUse(ExpressionTree tree) {
    VariableElement result = TreeUtils.variableElementFromTree(tree);
    if (result == null) {
      throw new BugInCF("null element for %s [%s]", tree, tree.getClass());
    }
    return result;
  }

  /**
   * Returns the element for the given expression.
   *
   * @param tree the {@link Tree} node to get the symbol for
   * @return the element for the given tree, or null if one could not be found
   * @deprecated use elementFromUse
   */
  @Deprecated // not for removal; retain to prevent calls to this overload
  @Pure
  public static Element elementFromDeclaration(MemberSelectTree tree) {
    return TreeUtils.elementFromUse(tree);
  }

  /**
   * Returns the element for the given expression.
   *
   * @param tree the {@link Tree} node to get the symbol for
   * @return the element for the given tree, or null if one could not be found
   * @deprecated use elementFromUse
   */
  @Deprecated // not for removal; retain to prevent calls to this overload
  @Pure
  public static Element elementFromTree(MemberSelectTree tree) {
    return TreeUtils.elementFromUse(tree);
  }

  /**
   * Returns the element for the given expression.
   *
   * @param tree a method call
   * @return the element for the called method
   */
  @Pure
  public static Element elementFromUse(MemberSelectTree tree) {
    Element result = TreeInfo.symbolFor((JCTree) tree);
    if (result == null) {
      throw new BugInCF("tree = " + tree);
    }
    return result;
  }

  /**
   * Returns the ExecutableElement for the called method.
   *
   * @param tree the {@link Tree} node to get the symbol for
   * @return the Element for the given tree, or null if one could not be found
   * @deprecated use elementFromUse
   */
  @Deprecated // not for removal; retain to prevent calls to this overload
  @Pure
  public static ExecutableElement elementFromDeclaration(MethodInvocationTree tree) {
    return TreeUtils.elementFromUse(tree);
  }

  /**
   * Returns the ExecutableElement for the called method.
   *
   * @param tree the {@link Tree} node to get the symbol for
   * @return the Element for the given tree, or null if one could not be found
   * @deprecated use elementFromUse
   */
  @Deprecated // not for removal; retain to prevent calls to this overload
  @Pure
  public static ExecutableElement elementFromTree(MethodInvocationTree tree) {
    return TreeUtils.elementFromUse(tree);
  }

  /**
   * Returns the ExecutableElement for the called method.
   *
   * @param tree a method call
   * @return the ExecutableElement for the called method
   */
  @Pure
  public static ExecutableElement elementFromUse(MethodInvocationTree tree) {
    ExecutableElement result = (ExecutableElement) TreeInfo.symbolFor((JCTree) tree);
    if (result == null) {
      throw new BugInCF("tree = %s [%s]", tree, tree.getClass());
    }
    return result;
  }

  /**
   * Returns the ExecutableElement for the given method declaration.
   *
   * @param tree a method declaration
   * @return the element for the given method
   */
  public static ExecutableElement elementFromDeclaration(MethodTree tree) {
    ExecutableElement result = (ExecutableElement) TreeInfo.symbolFor((JCTree) tree);
    if (result == null) {
      throw new BugInCF("null element for method tree %s", tree);
    }
    return result;
  }

  /**
   * Returns the ExecutableElement for the given method declaration.
   *
   * @param tree the {@link Tree} node to get the symbol for
   * @return the Element for the given tree, or null if one could not be found
   * @deprecated use elementFromDeclaration
   */
  @Deprecated // not for removal; retain to prevent calls to this overload
  @Pure
  public static @Nullable ExecutableElement elementFromTree(MethodTree tree) {
    return elementFromDeclaration(tree);
  }

  /**
   * Returns the ExecutableElement for the given method declaration.
   *
   * @param tree the {@link Tree} node to get the symbol for
   * @return the Element for the given tree, or null if one could not be found
   * @deprecated use elementFromDeclaration
   */
  @Deprecated // not for removal; retain to prevent calls to this overload
  @Pure
  public static @Nullable ExecutableElement elementFromUse(MethodTree tree) {
    return elementFromDeclaration(tree);
  }

  /**
   * Returns the ExecutableElement for the given constructor invocation.
   *
   * @param tree the {@link Tree} node to get the symbol for
   * @throws IllegalArgumentException if {@code tree} is null or is not a valid javac-internal tree
   *     (JCTree)
   * @return the {@link Symbol} for the given tree, or null if one could not be found
   * @deprecated use elementFromUse
   */
  @Deprecated // not for removal; retain to prevent calls to this overload
  @Pure
  public static ExecutableElement elementFromDeclaration(NewClassTree tree) {
    return TreeUtils.elementFromUse(tree);
  }

  /**
   * Returns the ExecutableElement for the given constructor invocation.
   *
   * @param tree the {@link Tree} node to get the symbol for
   * @throws IllegalArgumentException if {@code tree} is null or is not a valid javac-internal tree
   *     (JCTree)
   * @return the {@link Symbol} for the given tree, or null if one could not be found
   * @deprecated use elementFromUse
   */
  @Deprecated // not for removal; retain to prevent calls to this overload
  @Pure
  public static ExecutableElement elementFromTree(NewClassTree tree) {
    return TreeUtils.elementFromUse(tree);
  }

  /**
   * Returns the ExecutableElement for the given constructor invocation.
   *
   * @param tree a constructor invocation
   * @return the ExecutableElement for the called constructor
   * @see #constructor(NewClassTree)
   */
  @Pure
  public static ExecutableElement elementFromUse(NewClassTree tree) {
    ExecutableElement result = (ExecutableElement) TreeInfo.symbolFor((JCTree) tree);
    if (result == null) {
      throw new BugInCF("null element for %s", tree);
    }
    return result;
  }

  /**
   * Returns the VariableElement corresponding to the given variable declaration.
   *
   * @param tree the variable
   * @return the element for the given variable
   */
  public static VariableElement elementFromDeclaration(VariableTree tree) {
    VariableElement result = (VariableElement) TreeInfo.symbolFor((JCTree) tree);
    if (result == null) {
      throw new BugInCF("null element for variable tree %s", tree);
    }
    return result;
  }

  /**
   * Returns the VariableElement corresponding to the given variable declaration.
   *
   * @param tree the {@link Tree} node to get the symbol for
   * @return the Element for the given tree, or null if one could not be found
   * @deprecated use elementFromDeclaration
   */
  @Deprecated // not for removal; retain to prevent calls to this overload
  @Pure
  public static @Nullable VariableElement elementFromTree(VariableTree tree) {
    return elementFromDeclaration(tree);
  }

  /**
   * Returns the VariableElement corresponding to the given variable declaration.
   *
   * @param tree the {@link Tree} node to get the symbol for
   * @return the Element for the given tree, or null if one could not be found
   * @deprecated use elementFromDeclaration
   */
  @Deprecated // not for removal; retain to prevent calls to this overload
  @Pure
  public static @Nullable VariableElement elementFromUse(VariableTree tree) {
    return elementFromDeclaration(tree);
  }

  /**
   * Returns the {@link VariableElement} for the given Tree API node.
   *
   * @param tree the {@link Tree} node to get the symbol for
   * @throws IllegalArgumentException if {@code tree} is null or is not a valid javac-internal tree
   *     (JCTree)
   * @return the {@link Symbol} for the given tree
   */
  @Pure
  public static VariableElement variableElementFromTree(Tree tree) {
    VariableElement result = (VariableElement) TreeInfo.symbolFor((JCTree) tree);
    if (result == null) {
      throw new BugInCF("null element for %s [%s]", tree, tree.getClass());
    }
    return result;
  }

  /**
   * Returns the {@link Element} for the given Tree API node. For an object instantiation returns
   * the value of the {@link JCNewClass#constructor} field.
   *
   * <p>Use this only when you do not statically know whether the tree is a declaration or a use of
   * an element.
   *
   * @param tree the {@link Tree} node to get the symbol for
   * @throws BugInCF if {@code tree} is null or is not a valid javac-internal tree (JCTree)
   * @return the {@link Symbol} for the given tree, or null if one could not be found
   */
  @Pure
  public static @Nullable Element elementFromTree(Tree tree) {
    if (tree == null) {
      throw new BugInCF("TreeUtils.elementFromTree: tree is null");
    }

    if (!(tree instanceof JCTree)) {
      throw new BugInCF(
          "TreeUtils.elementFromTree: tree is not a valid Javac tree but a " + tree.getClass());
    }

    if (isExpressionTree(tree)) {
      tree = withoutParensOrCasts((ExpressionTree) tree);
    }

    switch (tree.getKind()) {
        // symbol() only works on MethodSelects, so we need to get it manually
        // for method invocations.
      case METHOD_INVOCATION:
        return TreeInfo.symbol(((JCMethodInvocation) tree).getMethodSelect());

      case ASSIGNMENT:
        return TreeInfo.symbol((JCTree) ((AssignmentTree) tree).getVariable());

      case ARRAY_ACCESS:
        return elementFromTree(((ArrayAccessTree) tree).getExpression());

      case NEW_CLASS:
        return ((JCNewClass) tree).constructor;

      case MEMBER_REFERENCE:
        // TreeInfo.symbol, which is used in the default case, didn't handle
        // member references until JDK8u20. So handle it here.
        ExecutableElement memberResult = (ExecutableElement) ((JCMemberReference) tree).sym;
        return memberResult;

      default:
        Element defaultResult;
        if (isTypeDeclaration(tree)
            || tree.getKind() == Tree.Kind.VARIABLE
            || tree.getKind() == Tree.Kind.METHOD) {
          defaultResult = TreeInfo.symbolFor((JCTree) tree);
        } else {
          defaultResult = TreeInfo.symbol((JCTree) tree);
        }
        return defaultResult;
    }
  }

  /**
   * Returns the constructor invoked by {@code newClassTree} unless {@code newClassTree} is creating
   * an anonymous class. In which case, the super constructor is returned.
   *
   * @param newClassTree the constructor invocation
   * @return the super constructor invoked in the body of the anonymous constructor; or {@link
   *     #constructor(NewClassTree)} if {@code newClassTree} is not creating an anonymous class
   */
  public static ExecutableElement getSuperConstructor(NewClassTree newClassTree) {
    if (newClassTree.getClassBody() == null) {
      return elementFromUse(newClassTree);
    }
    JCNewClass jcNewClass = (JCNewClass) newClassTree;
    // Anonymous constructor bodies, which are always synthetic, contain exactly one statement
    // in the form:
    //    super(arg1, ...)
    // or
    //    o.super(arg1, ...)
    //
    // which is a method invocation of the super constructor.

    // The method call is guaranteed to return nonnull.
    JCMethodDecl anonConstructor =
        (JCMethodDecl) TreeInfo.declarationFor(jcNewClass.constructor, jcNewClass);
    assert anonConstructor != null;
    assert anonConstructor.body.stats.size() == 1;
    JCExpressionStatement stmt = (JCExpressionStatement) anonConstructor.body.stats.head;
    JCMethodInvocation superInvok = (JCMethodInvocation) stmt.expr;
    return (ExecutableElement) TreeInfo.symbol(superInvok.meth);
  }

  /**
   * Determines the symbol for a constructor given an invocation via {@code new}.
   *
   * @see #elementFromUse(NewClassTree)
   * @param tree the constructor invocation
   * @return the {@link ExecutableElement} corresponding to the constructor call in {@code tree}
   * @deprecated use elementFromUse instead
   */
  @Deprecated // 2022-09-12
  public static ExecutableElement constructor(NewClassTree tree) {
    return (ExecutableElement) ((JCNewClass) tree).constructor;
  }

  /**
   * Determine whether the given ExpressionTree has an underlying element.
   *
   * @param tree the ExpressionTree to test
   * @return whether the tree refers to an identifier, member select, or method invocation
   */
  @EnsuresNonNullIf(result = true, expression = "elementFromTree(#1)")
  @EnsuresNonNullIf(result = true, expression = "elementFromUse(#1)")
  @Pure
  public static boolean isUseOfElement(ExpressionTree tree) {
    ExpressionTree realnode = TreeUtils.withoutParens(tree);
    switch (realnode.getKind()) {
      case IDENTIFIER:
      case MEMBER_SELECT:
      case METHOD_INVOCATION:
      case NEW_CLASS:
        assert elementFromTree(tree) != null : "@AssumeAssertion(nullness): inspection";
        assert elementFromUse(tree) != null : "@AssumeAssertion(nullness): inspection";
        return true;
      default:
        return false;
    }
  }

  /**
   * Returns true if {@code tree} has a synthetic argument.
   *
   * <p>For some anonymous classes with an explicit enclosing expression, javac creates a synthetic
   * argument to the constructor that is the enclosing expression of the NewClassTree. Suppose a
   * programmer writes:
   *
   * <pre>{@code class Outer {
   *   class Inner { }
   *     void method() {
   *       this.new Inner(){};
   *     }
   * }}</pre>
   *
   * Java 9 javac creates the following synthetic tree for {@code this.new Inner(){}}:
   *
   * <pre>{@code new Inner(this) {
   *   (.Outer x0) {
   *     x0.super();
   *   }
   * }}</pre>
   *
   * Java 11 javac creates a different tree without the synthetic argument for {@code this.new
   * Inner(){}}; the first line in the below code differs:
   *
   * <pre>{@code this.new Inner() {
   *   (.Outer x0) {
   *     x0.super();
   *   }
   * }}</pre>
   *
   * @param tree a new class tree
   * @return true if {@code tree} has a synthetic argument
   */
  public static boolean hasSyntheticArgument(NewClassTree tree) {
    if (tree.getClassBody() == null || tree.getEnclosingExpression() != null) {
      return false;
    }
    for (Tree member : tree.getClassBody().getMembers()) {
      if (member.getKind() == Tree.Kind.METHOD && isConstructor((MethodTree) member)) {
        MethodTree methodTree = (MethodTree) member;
        StatementTree f = methodTree.getBody().getStatements().get(0);
        return TreeUtils.getReceiverTree(((ExpressionStatementTree) f).getExpression()) != null;
      }
    }
    return false;
  }
  /**
   * Returns the name of the invoked method.
   *
   * @param tree the method invocation
   * @return the name of the invoked method
   */
  public static Name methodName(MethodInvocationTree tree) {
    ExpressionTree expr = tree.getMethodSelect();
    if (expr.getKind() == Tree.Kind.IDENTIFIER) {
      return ((IdentifierTree) expr).getName();
    } else if (expr.getKind() == Tree.Kind.MEMBER_SELECT) {
      return ((MemberSelectTree) expr).getIdentifier();
    }
    throw new BugInCF("TreeUtils.methodName: cannot be here: " + tree);
  }

  /**
   * Returns true if the first statement in the body is a self constructor invocation within a
   * constructor.
   *
   * @param tree the method declaration
   * @return true if the first statement in the body is a self constructor invocation within a
   *     constructor
   */
  public static boolean containsThisConstructorInvocation(MethodTree tree) {
    if (!TreeUtils.isConstructor(tree) || tree.getBody().getStatements().isEmpty()) {
      return false;
    }

    StatementTree st = tree.getBody().getStatements().get(0);
    if (!(st instanceof ExpressionStatementTree)
        || !(((ExpressionStatementTree) st).getExpression() instanceof MethodInvocationTree)) {
      return false;
    }

    MethodInvocationTree invocation =
        (MethodInvocationTree) ((ExpressionStatementTree) st).getExpression();

    return "this".contentEquals(TreeUtils.methodName(invocation));
  }

  /**
   * Returns the first statement of the tree if it is a block. If it is not a block or an empty
   * block, tree is returned.
   *
   * @param tree any kind of tree
   * @return the first statement of the tree if it is a block. If it is not a block or an empty
   *     block, tree is returned.
   */
  public static Tree firstStatement(Tree tree) {
    Tree first;
    if (tree.getKind() == Tree.Kind.BLOCK) {
      BlockTree block = (BlockTree) tree;
      if (block.getStatements().isEmpty()) {
        first = block;
      } else {
        first = block.getStatements().iterator().next();
      }
    } else {
      first = tree;
    }
    return first;
  }

  /**
   * Determine whether the given class contains an explicit constructor.
   *
   * @param tree a class tree
   * @return true iff there is an explicit constructor
   */
  public static boolean hasExplicitConstructor(ClassTree tree) {
    TypeElement elem = TreeUtils.elementFromDeclaration(tree);
    for (ExecutableElement constructorElt :
        ElementFilter.constructorsIn(elem.getEnclosedElements())) {
      if (!isSynthetic(constructorElt)) {
        return true;
      }
    }
    return false;
  }

  /**
   * Returns true if the given method is synthetic. Also returns true if the method is a generated
   * default constructor, which does not appear in source code but is not considered synthetic.
   *
   * @param ee a method or constructor element
   * @return true iff the given method is synthetic
   */
  public static boolean isSynthetic(ExecutableElement ee) {
    MethodSymbol ms = (MethodSymbol) ee;
    long mod = ms.flags();
    // GENERATEDCONSTR is for generated constructors, which do not have SYNTHETIC set.
    return (mod & (Flags.SYNTHETIC | Flags.GENERATEDCONSTR)) != 0;
  }

  /**
   * Returns true if the given method is synthetic.
   *
   * @param tree a method declaration tree
   * @return true iff the given method is synthetic
   */
  public static boolean isSynthetic(MethodTree tree) {
    ExecutableElement ee = TreeUtils.elementFromDeclaration(tree);
    return isSynthetic(ee);
  }

  /**
   * Returns true if the tree is of a diamond type. In contrast to the implementation in TreeInfo,
   * this version works on Trees.
   *
   * @see com.sun.tools.javac.tree.TreeInfo#isDiamond(JCTree)
   */
  public static boolean isDiamondTree(Tree tree) {
    switch (tree.getKind()) {
      case ANNOTATED_TYPE:
        return isDiamondTree(((AnnotatedTypeTree) tree).getUnderlyingType());
      case PARAMETERIZED_TYPE:
        return ((ParameterizedTypeTree) tree).getTypeArguments().isEmpty();
      case NEW_CLASS:
        return isDiamondTree(((NewClassTree) tree).getIdentifier());
      default:
        return false;
    }
  }

  /** Returns true if the tree represents a {@code String} concatenation operation. */
  public static boolean isStringConcatenation(Tree tree) {
    return (tree.getKind() == Tree.Kind.PLUS && TypesUtils.isString(TreeUtils.typeOf(tree)));
  }

  /** Returns true if the compound assignment tree is a string concatenation. */
  public static boolean isStringCompoundConcatenation(CompoundAssignmentTree tree) {
    return (tree.getKind() == Tree.Kind.PLUS_ASSIGNMENT
        && TypesUtils.isString(TreeUtils.typeOf(tree)));
  }

  /**
   * Returns true if the tree is a constant-time expression.
   *
   * <p>A tree is a constant-time expression if it is:
   *
   * <ol>
   *   <li>a literal tree
   *   <li>a reference to a final variable initialized with a compile time constant
   *   <li>a String concatenation of two compile time constants
   * </ol>
   *
   * @param tree the tree to check
   * @return true if the tree is a constant-time expression.
   */
  public static boolean isCompileTimeString(ExpressionTree tree) {
    tree = TreeUtils.withoutParens(tree);
    if (tree instanceof LiteralTree) {
      return true;
    }

    if (TreeUtils.isUseOfElement(tree)) {
      Element elt = TreeUtils.elementFromUse(tree);
      return ElementUtils.isCompileTimeConstant(elt);
    } else if (TreeUtils.isStringConcatenation(tree)) {
      BinaryTree binOp = (BinaryTree) tree;
      return isCompileTimeString(binOp.getLeftOperand())
          && isCompileTimeString(binOp.getRightOperand());
    } else {
      return false;
    }
  }

  /**
   * Returns the receiver tree of a field access or a method invocation.
   *
   * @param expression a field access or a method invocation
   * @return the expression's receiver tree, or null if it does not have an explicit receiver
   */
  public static @Nullable ExpressionTree getReceiverTree(ExpressionTree expression) {
    ExpressionTree receiver;
    switch (expression.getKind()) {
      case METHOD_INVOCATION:
        // Trying to handle receiver calls to trees of the form
        //     ((m).getArray())
        // returns the type of 'm' in this case
        receiver = ((MethodInvocationTree) expression).getMethodSelect();

        if (receiver.getKind() == Tree.Kind.MEMBER_SELECT) {
          receiver = ((MemberSelectTree) receiver).getExpression();
        } else {
          // It's a method call "m(foo)" without an explicit receiver
          return null;
        }
        break;
      case NEW_CLASS:
        receiver = ((NewClassTree) expression).getEnclosingExpression();
        break;
      case ARRAY_ACCESS:
        receiver = ((ArrayAccessTree) expression).getExpression();
        break;
      case MEMBER_SELECT:
        receiver = ((MemberSelectTree) expression).getExpression();
        // Avoid int.class
        if (receiver instanceof PrimitiveTypeTree) {
          return null;
        }
        break;
      case IDENTIFIER:
        // It's a field access on implicit this or a local variable/parameter.
        return null;
      default:
        return null;
    }
    if (receiver == null) {
      return null;
    }

    return TreeUtils.withoutParens(receiver);
  }

  // TODO: What about anonymous classes?
  // Adding Tree.Kind.NEW_CLASS here doesn't work, because then a
  // tree gets cast to ClassTree when it is actually a NewClassTree,
  // for example in enclosingClass above.
  /** The kinds that represent classes. */
  private static final Set<Tree.Kind> classTreeKinds;

  static {
    classTreeKinds = EnumSet.noneOf(Tree.Kind.class);
    for (Tree.Kind kind : Tree.Kind.values()) {
      if (kind.asInterface() == ClassTree.class) {
        classTreeKinds.add(kind);
      }
    }
  }

  /**
   * Return the set of kinds that represent classes.
   *
   * @return the set of kinds that represent classes
   */
  public static Set<Tree.Kind> classTreeKinds() {
    return classTreeKinds;
  }

  /**
   * Is the given tree kind a class, i.e. a class, enum, interface, or annotation type.
   *
   * @param tree the tree to test
   * @return true, iff the given kind is a class kind
   */
  public static boolean isClassTree(Tree tree) {
    return classTreeKinds().contains(tree.getKind());
  }

  /**
   * The kinds that represent declarations that might have {@code @SuppressWarnings} written on
   * them: classes, methods, and variables.
   */
  private static final Set<Tree.Kind> declarationTreeKinds;

  static {
    declarationTreeKinds = EnumSet.noneOf(Tree.Kind.class);
    declarationTreeKinds.addAll(classTreeKinds);
    declarationTreeKinds.add(Tree.Kind.METHOD);
    declarationTreeKinds.add(Tree.Kind.VARIABLE);
  }

  /**
   * Return the set of kinds that represent declarations: classes, methods, and variables.
   *
   * @return the set of kinds that represent declarations
   */
  public static Set<Tree.Kind> declarationTreeKinds() {
    return declarationTreeKinds;
  }

  /**
   * Returns true if the given tree is a declaration.
   *
   * @param tree the tree to test
   * @return true if the given tree is a declaration
   */
  public static boolean isDeclarationTree(Tree tree) {
    return declarationTreeKinds.contains(tree.getKind());
  }

  /** The kinds that represent types. */
  private static final Set<Tree.Kind> typeTreeKinds =
      EnumSet.of(
          Tree.Kind.PRIMITIVE_TYPE,
          Tree.Kind.PARAMETERIZED_TYPE,
          Tree.Kind.TYPE_PARAMETER,
          Tree.Kind.ARRAY_TYPE,
          Tree.Kind.UNBOUNDED_WILDCARD,
          Tree.Kind.EXTENDS_WILDCARD,
          Tree.Kind.SUPER_WILDCARD,
          Tree.Kind.ANNOTATED_TYPE);

  /**
   * Return the set of kinds that represent types.
   *
   * @return the set of kinds that represent types
   */
  public static Set<Tree.Kind> typeTreeKinds() {
    return typeTreeKinds;
  }

  /**
   * Is the given tree a type instantiation?
   *
   * <p>TODO: this is an under-approximation: e.g. an identifier could be either a type use or an
   * expression. How can we distinguish.
   *
   * @param tree the tree to test
   * @return true, iff the given tree is a type
   */
  public static boolean isTypeTree(Tree tree) {
    return typeTreeKinds().contains(tree.getKind());
  }

  /**
   * Returns true if the given element is an invocation of the method, or of any method that
   * overrides that one.
   */
  public static boolean isMethodInvocation(
      Tree tree, ExecutableElement method, ProcessingEnvironment env) {
    if (!(tree instanceof MethodInvocationTree)) {
      return false;
    }
    MethodInvocationTree methInvok = (MethodInvocationTree) tree;
    ExecutableElement invoked = TreeUtils.elementFromUse(methInvok);
    if (invoked == null) {
      return false;
    }
    return ElementUtils.isMethod(invoked, method, env);
  }

  /**
   * Returns true if the argument is an invocation of one of the given methods, or of any method
   * that overrides them.
   */
  public static boolean isMethodInvocation(
      Tree methodTree, List<ExecutableElement> methods, ProcessingEnvironment processingEnv) {
    if (!(methodTree instanceof MethodInvocationTree)) {
      return false;
    }
    for (ExecutableElement Method : methods) {
      if (isMethodInvocation(methodTree, Method, processingEnv)) {
        return true;
      }
    }
    return false;
  }

  /**
   * Returns the ExecutableElement for a method declaration. Errs if there is not exactly one
   * matching method. If more than one method takes the same number of formal parameters, then use
   * {@link #getMethod(String, String, ProcessingEnvironment, String...)}.
   *
   * @param type the class that contains the method
   * @param methodName the name of the method
   * @param params the number of formal parameters
   * @param env the processing environment
   * @return the ExecutableElement for the specified method
   */
  public static ExecutableElement getMethod(
      Class<?> type, String methodName, int params, ProcessingEnvironment env) {
    String typeName = type.getCanonicalName();
    if (typeName == null) {
      throw new BugInCF("TreeUtils.getMethod: class %s has no canonical name", type);
    }
    return getMethod(typeName, methodName, params, env);
  }

  /**
   * Returns the ExecutableElement for a method declaration. Errs if there is not exactly one
   * matching method. If more than one method takes the same number of formal parameters, then use
   * {@link #getMethod(String, String, ProcessingEnvironment, String...)}.
   *
   * @param typeName the class that contains the method
   * @param methodName the name of the method
   * @param params the number of formal parameters
   * @param env the processing environment
   * @return the ExecutableElement for the specified method
   */
  public static ExecutableElement getMethod(
      @FullyQualifiedName String typeName,
      String methodName,
      int params,
      ProcessingEnvironment env) {
    List<ExecutableElement> methods = getMethods(typeName, methodName, params, env);
    if (methods.size() == 1) {
      return methods.get(0);
    }
    throw new BugInCF(
        "TreeUtils.getMethod(%s, %s, %d): expected 1 match, found %d",
        typeName, methodName, params, methods.size());
  }

  /**
   * Returns the ExecutableElement for a method declaration. Returns null there is no matching
   * method. Errs if there is more than one matching method. If more than one method takes the same
   * number of formal parameters, then use {@link #getMethod(String, String, ProcessingEnvironment,
   * String...)}.
   *
   * @param typeName the class that contains the method
   * @param methodName the name of the method
   * @param params the number of formal parameters
   * @param env the processing environment
   * @return the ExecutableElement for the specified method, or null
   */
  public static @Nullable ExecutableElement getMethodOrNull(
      @FullyQualifiedName String typeName,
      String methodName,
      int params,
      ProcessingEnvironment env) {
    List<ExecutableElement> methods = getMethods(typeName, methodName, params, env);
    if (methods.size() == 0) {
      return null;
    } else if (methods.size() == 1) {
      return methods.get(0);
    } else {
      throw new BugInCF(
          "TreeUtils.getMethod(%s, %s, %d): expected 0 or 1 match, found %d",
          typeName, methodName, params, methods.size());
    }
  }

  /**
   * Returns all ExecutableElements for method declarations of methodName, in class typeName, with
   * params formal parameters.
   *
   * @param typeName the class that contains the method
   * @param methodName the name of the method
   * @param params the number of formal parameters
   * @param env the processing environment
   * @return the ExecutableElements for all matching methods
   */
  public static List<ExecutableElement> getMethods(
      @FullyQualifiedName String typeName,
      String methodName,
      int params,
      ProcessingEnvironment env) {
    List<ExecutableElement> methods = new ArrayList<>(1);
    TypeElement typeElt = env.getElementUtils().getTypeElement(typeName);
    if (typeElt == null) {
      throw new UserError("Configuration problem! Could not load type: " + typeName);
    }
    for (ExecutableElement exec : ElementFilter.methodsIn(typeElt.getEnclosedElements())) {
      if (exec.getSimpleName().contentEquals(methodName) && exec.getParameters().size() == params) {
        methods.add(exec);
      }
    }
    return methods;
  }

  /**
   * Returns the ExecutableElement for a method declaration. Errs if there is no matching method.
   *
   * @param type the class that contains the method
   * @param methodName the name of the method
   * @param env the processing environment
   * @param paramTypes the method's formal parameter types
   * @return the ExecutableElement for the specified method
   */
  public static ExecutableElement getMethod(
      Class<?> type, String methodName, ProcessingEnvironment env, String... paramTypes) {
    String typeName = type.getCanonicalName();
    if (typeName == null) {
      throw new BugInCF("TreeUtils.getMethod: class %s has no canonical name", type);
    }
    return getMethod(typeName, methodName, env, paramTypes);
  }

  /**
   * Returns the ExecutableElement for a method declaration. Errs if there is no matching method.
   *
   * @param typeName the class that contains the method
   * @param methodName the name of the method
   * @param env the processing environment
   * @param paramTypes the method's formal parameter types
   * @return the ExecutableElement for the specified method
   */
  public static ExecutableElement getMethod(
      @FullyQualifiedName String typeName,
      String methodName,
      ProcessingEnvironment env,
      String... paramTypes) {
    TypeElement typeElt = env.getElementUtils().getTypeElement(typeName);
    for (ExecutableElement exec : ElementFilter.methodsIn(typeElt.getEnclosedElements())) {
      if (exec.getSimpleName().contentEquals(methodName)
          && exec.getParameters().size() == paramTypes.length) {
        boolean typesMatch = true;
        List<? extends VariableElement> params = exec.getParameters();
        for (int i = 0; i < paramTypes.length; i++) {
          VariableElement ve = params.get(i);
          TypeMirror tm = TypeAnnotationUtils.unannotatedType(ve.asType());
          if (!tm.toString().equals(paramTypes[i])) {
            typesMatch = false;
            break;
          }
        }
        if (typesMatch) {
          return exec;
        }
      }
    }
    List<String> candidates = new ArrayList<>();
    for (ExecutableElement exec : ElementFilter.methodsIn(typeElt.getEnclosedElements())) {
      if (exec.getSimpleName().contentEquals(methodName)) {
        candidates.add(executableElementToString(exec));
      }
    }
    if (candidates.isEmpty()) {
      for (ExecutableElement exec : ElementFilter.methodsIn(typeElt.getEnclosedElements())) {
        candidates.add(executableElementToString(exec));
      }
    }
    throw new BugInCF(
        "TreeUtils.getMethod: found no match for %s.%s(%s); candidates: %s",
        typeName, methodName, Arrays.toString(paramTypes), candidates);
  }

  /**
   * Formats the ExecutableElement in the way that getMethod() expects it.
   *
   * @param exec an executable element
   * @return the ExecutableElement, formatted in the way that getMethod() expects it
   */
  private static String executableElementToString(ExecutableElement exec) {
    StringJoiner result = new StringJoiner(", ", exec.getSimpleName() + "(", ")");
    for (VariableElement param : exec.getParameters()) {
      result.add(TypeAnnotationUtils.unannotatedType(param.asType()).toString());
    }
    return result.toString();
  }

  /**
   * Determine whether the given expression is either "this" or an outer "C.this".
   *
   * <p>TODO: Should this also handle "super"?
   */
  public static boolean isExplicitThisDereference(ExpressionTree tree) {
    if (tree.getKind() == Tree.Kind.IDENTIFIER
        && ((IdentifierTree) tree).getName().contentEquals("this")) {
      // Explicit this reference "this"
      return true;
    }

    if (tree.getKind() != Tree.Kind.MEMBER_SELECT) {
      return false;
    }

    MemberSelectTree memSelTree = (MemberSelectTree) tree;
    if (memSelTree.getIdentifier().contentEquals("this")) {
      // Outer this reference "C.this"
      return true;
    }
    return false;
  }

  /**
   * Determine whether {@code tree} is a class literal, such as
   *
   * <pre>
   *   <em>Object</em> . <em>class</em>
   * </pre>
   *
   * @return true iff if tree is a class literal
   */
  public static boolean isClassLiteral(Tree tree) {
    if (tree.getKind() != Tree.Kind.MEMBER_SELECT) {
      return false;
    }
    return "class".equals(((MemberSelectTree) tree).getIdentifier().toString());
  }

  /**
   * Determine whether {@code tree} is a field access expression, such as
   *
   * <pre>
   *   <em>f</em>
   *   <em>obj</em> . <em>f</em>
   * </pre>
   *
   * This method currently also returns true for class literals and qualified this.
   *
   * @param tree a tree that might be a field access
   * @return true iff if tree is a field access expression (implicit or explicit)
   */
  public static boolean isFieldAccess(Tree tree) {
    if (tree.getKind() == Tree.Kind.MEMBER_SELECT) {
      // explicit member access (or a class literal or a qualified this)
      MemberSelectTree memberSelect = (MemberSelectTree) tree;
      assert isUseOfElement(memberSelect) : "@AssumeAssertion(nullness): tree kind";
      Element el = TreeUtils.elementFromUse(memberSelect);
      return el.getKind().isField();
    } else if (tree.getKind() == Tree.Kind.IDENTIFIER) {
      // implicit field access
      IdentifierTree ident = (IdentifierTree) tree;
      assert isUseOfElement(ident) : "@AssumeAssertion(nullness): tree kind";
      Element el = TreeUtils.elementFromUse(ident);
      return el.getKind().isField()
          && !ident.getName().contentEquals("this")
          && !ident.getName().contentEquals("super");
    }
    return false;
  }

  /**
   * Compute the name of the field that the field access {@code tree} accesses. Requires {@code
   * tree} to be a field access, as determined by {@code isFieldAccess} (which currently also
   * returns true for class literals and qualified this).
   *
   * @param tree a field access tree
   * @return the name of the field accessed by {@code tree}
   */
  public static String getFieldName(Tree tree) {
    assert isFieldAccess(tree);
    if (tree.getKind() == Tree.Kind.MEMBER_SELECT) {
      MemberSelectTree mtree = (MemberSelectTree) tree;
      return mtree.getIdentifier().toString();
    } else {
      IdentifierTree itree = (IdentifierTree) tree;
      return itree.getName().toString();
    }
  }

  /**
   * Determine whether {@code tree} refers to a method element, such as.
   *
   * <pre>
   *   <em>m</em>(...)
   *   <em>obj</em> . <em>m</em>(...)
   * </pre>
   *
   * @return true iff if tree is a method access expression (implicit or explicit)
   */
  public static boolean isMethodAccess(Tree tree) {
    if (tree.getKind() == Tree.Kind.MEMBER_SELECT) {
      // explicit method access
      MemberSelectTree memberSelect = (MemberSelectTree) tree;
      assert isUseOfElement(memberSelect) : "@AssumeAssertion(nullness): tree kind";
      Element el = TreeUtils.elementFromUse(memberSelect);
      return el.getKind() == ElementKind.METHOD || el.getKind() == ElementKind.CONSTRUCTOR;
    } else if (tree.getKind() == Tree.Kind.IDENTIFIER) {
      // implicit method access
      IdentifierTree ident = (IdentifierTree) tree;
      // The field "super" and "this" are also legal methods
      if (ident.getName().contentEquals("super") || ident.getName().contentEquals("this")) {
        return true;
      }
      assert isUseOfElement(ident) : "@AssumeAssertion(nullness): tree kind";
      Element el = TreeUtils.elementFromUse(ident);
      return el.getKind() == ElementKind.METHOD || el.getKind() == ElementKind.CONSTRUCTOR;
    }
    return false;
  }

  /**
   * Compute the name of the method that the method access {@code tree} accesses. Requires {@code
   * tree} to be a method access, as determined by {@code isMethodAccess}.
   *
   * @param tree a method access tree
   * @return the name of the method accessed by {@code tree}
   */
  public static String getMethodName(Tree tree) {
    assert isMethodAccess(tree);
    if (tree.getKind() == Tree.Kind.MEMBER_SELECT) {
      MemberSelectTree mtree = (MemberSelectTree) tree;
      return mtree.getIdentifier().toString();
    } else {
      IdentifierTree itree = (IdentifierTree) tree;
      return itree.getName().toString();
    }
  }

  /**
   * Return {@code true} if and only if {@code tree} can have a type annotation.
   *
   * @return {@code true} if and only if {@code tree} can have a type annotation
   */
  // TODO: is this implementation precise enough? E.g. does a .class literal work correctly?
  public static boolean canHaveTypeAnnotation(Tree tree) {
    return ((JCTree) tree).type != null;
  }

  /**
   * Returns true if and only if the given {@code tree} represents a field access of the given
   * {@link VariableElement}.
   */
  public static boolean isSpecificFieldAccess(Tree tree, VariableElement var) {
    if (tree instanceof MemberSelectTree) {
      MemberSelectTree memSel = (MemberSelectTree) tree;
      assert isUseOfElement(memSel) : "@AssumeAssertion(nullness): tree kind";
      Element field = TreeUtils.elementFromUse(memSel);
      return field.equals(var);
    } else if (tree instanceof IdentifierTree) {
      IdentifierTree idTree = (IdentifierTree) tree;
      assert isUseOfElement(idTree) : "@AssumeAssertion(nullness): tree kind";
      Element field = TreeUtils.elementFromUse(idTree);
      return field.equals(var);
    } else {
      return false;
    }
  }

  /**
   * Returns the VariableElement for a field declaration.
   *
   * @param typeName the class where the field is declared
   * @param fieldName the name of the field
   * @param env the processing environment
   * @return the VariableElement for typeName.fieldName
   */
  public static VariableElement getField(
      @FullyQualifiedName String typeName, String fieldName, ProcessingEnvironment env) {
    TypeElement mapElt = env.getElementUtils().getTypeElement(typeName);
    for (VariableElement var : ElementFilter.fieldsIn(mapElt.getEnclosedElements())) {
      if (var.getSimpleName().contentEquals(fieldName)) {
        return var;
      }
    }
    throw new BugInCF("TreeUtils.getField: shouldn't be here");
  }

  /**
   * Determine whether the given tree represents an ExpressionTree.
   *
   * @param tree the Tree to test
   * @return whether the tree is an ExpressionTree
   */
  public static boolean isExpressionTree(Tree tree) {
    return tree instanceof ExpressionTree;
  }

  /**
   * Returns true if this is a super call to the {@link Enum} constructor.
   *
   * @param tree the method invocation to check
   * @return true if this is a super call to the {@link Enum} constructor
   */
  public static boolean isEnumSuperCall(MethodInvocationTree tree) {
    ExecutableElement ex = TreeUtils.elementFromUse(tree);
    assert ex != null : "@AssumeAssertion(nullness): tree kind";
    Name name = ElementUtils.getQualifiedClassName(ex);
    assert name != null : "@AssumeAssertion(nullness): assumption";
    boolean correctClass = "java.lang.Enum".contentEquals(name);
    boolean correctMethod = "<init>".contentEquals(ex.getSimpleName());
    return correctClass && correctMethod;
  }

  /**
   * Determine whether the given tree represents a declaration of a type (including type
   * parameters).
   *
   * @param tree the Tree to test
   * @return true if the tree is a type declaration
   */
  public static boolean isTypeDeclaration(Tree tree) {
    return isClassTree(tree) || tree.getKind() == Tree.Kind.TYPE_PARAMETER;
  }

  /**
   * Returns whether or not tree is an access of array length.
   *
   * @param tree tree to check
   * @return true if tree is an access of array length
   */
  public static boolean isArrayLengthAccess(Tree tree) {
    if (tree.getKind() == Tree.Kind.MEMBER_SELECT
        && isFieldAccess(tree)
        && getFieldName(tree).equals("length")) {
      ExpressionTree expressionTree = ((MemberSelectTree) tree).getExpression();
      if (TreeUtils.typeOf(expressionTree).getKind() == TypeKind.ARRAY) {
        return true;
      }
    }
    return false;
  }

  /**
   * Determines whether or not the given {@link MethodTree} is an anonymous constructor (the
   * constructor for an anonymous class).
   *
   * @param method a method tree that may be an anonymous constructor
   * @return true if the given path points to an anonymous constructor, false if it does not
   */
  public static boolean isAnonymousConstructor(final MethodTree method) {
    Element e = elementFromTree(method);
    if (e == null || e.getKind() != ElementKind.CONSTRUCTOR) {
      return false;
    }
    TypeElement typeElement = (TypeElement) e.getEnclosingElement();
    return typeElement.getNestingKind() == NestingKind.ANONYMOUS;
  }

  /**
   * Returns true if the given {@link MethodTree} is a compact canonical constructor (the
   * constructor for a record where the parameters are implicitly declared and implicitly assigned
   * to the record's fields). This may be an explicitly declared compact canonical constructor or an
   * implicitly generated one.
   *
   * @param method a method tree that may be a compact canonical constructor
   * @return true if the given method is a compact canonical constructor
   */
  public static boolean isCompactCanonicalRecordConstructor(final MethodTree method) {
    Symbol s = (Symbol) elementFromTree(method);
    if (s == null) {
      throw new BugInCF(
          "TreeUtils.isCompactCanonicalRecordConstructor: null symbol for method tree: " + method);
    }
    return (s.flags() & Flags_RECORD) != 0;
  }

  /**
   * Returns true if the given {@link Tree} is part of a record that has been automatically
   * generated by the compiler. This can be a field that is derived from the record's header field
   * list, or an automatically generated canonical constructor.
   *
   * @param member the {@link Tree} for a member of a record
   * @return true if the given path is generated by the compiler
   */
  public static boolean isAutoGeneratedRecordMember(final Tree member) {
    Element e = elementFromTree(member);
    if (e == null) {
      throw new BugInCF("TreeUtils.isAutoGeneratedRecordMember: null symbol for tree: " + member);
    }
    return ElementUtils.isAutoGeneratedRecordMember(e);
  }

  /**
   * Converts the given AnnotationTrees to AnnotationMirrors.
   *
   * @param annoTrees list of annotation trees to convert to annotation mirrors
   * @return list of annotation mirrors that represent the given annotation trees
   */
  public static List<AnnotationMirror> annotationsFromTypeAnnotationTrees(
      List<? extends AnnotationTree> annoTrees) {
    return CollectionsPlume.mapList(TreeUtils::annotationFromAnnotationTree, annoTrees);
  }

  /**
   * Converts the given AnnotationTree to an AnnotationMirror.
   *
   * @param tree annotation tree to convert to an annotation mirror
   * @return annotation mirror that represent the given annotation tree
   */
  public static AnnotationMirror annotationFromAnnotationTree(AnnotationTree tree) {
    return ((JCAnnotation) tree).attribute;
  }

  /**
   * Converts the given AnnotatedTypeTree to a list of AnnotationMirrors.
   *
   * @param tree annotated type tree to convert
   * @return list of AnnotationMirrors from the tree
   */
  public static List<? extends AnnotationMirror> annotationsFromTree(AnnotatedTypeTree tree) {
    return annotationsFromTypeAnnotationTrees(((JCAnnotatedType) tree).annotations);
  }

  /**
   * Converts the given TypeParameterTree to a list of AnnotationMirrors.
   *
   * @param tree type parameter tree to convert
   * @return list of AnnotationMirrors from the tree
   */
  public static List<? extends AnnotationMirror> annotationsFromTree(TypeParameterTree tree) {
    return annotationsFromTypeAnnotationTrees(((JCTypeParameter) tree).annotations);
  }

  /**
   * Converts the given NewArrayTree to a list of AnnotationMirrors.
   *
   * @param tree new array tree
   * @return list of AnnotationMirrors from the tree
   */
  public static List<? extends AnnotationMirror> annotationsFromArrayCreation(
      NewArrayTree tree, int level) {

    assert tree instanceof JCNewArray;
    final JCNewArray newArray = ((JCNewArray) tree);

    if (level == -1) {
      return annotationsFromTypeAnnotationTrees(newArray.annotations);
    }

    if (newArray.dimAnnotations.length() > 0
        && (level >= 0)
        && (level < newArray.dimAnnotations.size())) {
      return annotationsFromTypeAnnotationTrees(newArray.dimAnnotations.get(level));
    }

    return Collections.emptyList();
  }

  /**
   * Returns true if the tree is the declaration or use of a local variable.
   *
   * @param tree the tree to check
   * @return true if the tree is the declaration or use of a local variable
   */
  public static boolean isLocalVariable(Tree tree) {
    if (tree.getKind() == Tree.Kind.VARIABLE) {
      VariableElement varElt = elementFromDeclaration((VariableTree) tree);
      return ElementUtils.isLocalVariable(varElt);
    } else if (tree.getKind() == Tree.Kind.IDENTIFIER) {
      ExpressionTree etree = (ExpressionTree) tree;
      assert isUseOfElement(etree) : "@AssumeAssertion(nullness): tree kind";
      return ElementUtils.isLocalVariable(elementFromUse(etree));
    }
    return false;
  }

  /**
   * Returns the type as a TypeMirror of {@code tree}. To obtain {@code tree}'s AnnotatedTypeMirror,
   * call {@code AnnotatedTypeFactory.getAnnotatedType()}.
   *
   * @return the type as a TypeMirror of {@code tree}
   */
  public static TypeMirror typeOf(Tree tree) {
    return ((JCTree) tree).type;
  }

  /**
   * Determines the type for a method invocation at its call site, which has all type variables
   * substituted with the type arguments at the call site.
   *
   * @param tree the method invocation
   * @return the {@link ExecutableType} corresponding to the method invocation at its call site
   */
  @Pure
  public static ExecutableType typeFromUse(MethodInvocationTree tree) {
    TypeMirror type = TreeUtils.typeOf(tree.getMethodSelect());
    if (!(type instanceof ExecutableType)) {
      throw new BugInCF(
          "TreeUtils.typeFromUse(MethodInvocationTree): type of method select in method"
              + " invocation should be ExecutableType. Found: %s",
          type);
    }
    return (ExecutableType) type;
  }

  /**
   * Determines the type for a constructor at its call site given an invocation via {@code new},
   * which has all type variables substituted with the type arguments at the call site.
   *
   * @param tree the constructor invocation
   * @return the {@link ExecutableType} corresponding to the constructor call (i.e., the given
   *     {@code tree}) at its call site
   */
  @Pure
  public static ExecutableType typeFromUse(NewClassTree tree) {
    if (!(tree instanceof JCTree.JCNewClass)) {
      throw new BugInCF("TreeUtils.typeFromUse(NewClassTree): not a javac internal tree");
    }

    JCNewClass newClassTree = (JCNewClass) tree;
    TypeMirror type = newClassTree.constructorType;

    if (!(type instanceof ExecutableType)) {
      throw new BugInCF(
          "TreeUtils.typeFromUse(NewClassTree): type of constructor in new class tree"
              + " should be ExecutableType. Found: %s",
          type);
    }
    return (ExecutableType) type;
  }

  /**
   * The type of the lambda or method reference tree is a functional interface type. This method
   * returns the single abstract method declared by that functional interface. (The type of this
   * method is referred to as the function type.)
   *
   * @param tree lambda or member reference tree
   * @param env ProcessingEnvironment
   * @return the single abstract method declared by the type of the tree
   */
  public static ExecutableElement findFunction(Tree tree, ProcessingEnvironment env) {
    Context ctx = ((JavacProcessingEnvironment) env).getContext();
    Types javacTypes = Types.instance(ctx);
    return (ExecutableElement) javacTypes.findDescriptorSymbol(((Type) typeOf(tree)).asElement());
  }

  /**
   * Returns true if {@code tree} is an implicitly typed lambda.
   *
   * <p>A lambda expression whose formal type parameters have inferred types is an implicitly typed
   * lambda. (See JLS 15.27.1)
   *
   * @param tree any kind of tree
   * @return true iff {@code tree} is an implicitly typed lambda
   */
  public static boolean isImplicitlyTypedLambda(Tree tree) {
    return tree.getKind() == Tree.Kind.LAMBDA_EXPRESSION
        && ((JCLambda) tree).paramKind == ParameterKind.IMPLICIT;
  }

  /**
   * Determine whether an expression {@link ExpressionTree} has the constant value true, according
   * to the compiler logic.
   *
   * @param tree the expression to be checked
   * @return true if {@code tree} has the constant value true
   */
  public static boolean isExprConstTrue(ExpressionTree tree) {
    assert tree instanceof JCExpression;
    if (((JCExpression) tree).type.isTrue()) {
      return true;
    }
    tree = TreeUtils.withoutParens(tree);
    if (tree instanceof JCTree.JCBinary) {
      JCBinary binTree = (JCBinary) tree;
      JCExpression ltree = binTree.lhs;
      JCExpression rtree = binTree.rhs;
      switch (binTree.getTag()) {
        case AND:
          return isExprConstTrue(ltree) && isExprConstTrue(rtree);
        case OR:
          return isExprConstTrue(ltree) || isExprConstTrue(rtree);
        default:
          break;
      }
    }
    return false;
  }

  /**
   * Return toString(), but without line separators.
   *
   * @param tree a tree
   * @return a one-line string representation of the tree
   */
  public static String toStringOneLine(Tree tree) {
    return tree.toString().trim().replaceAll("\\s+", " ");
  }

  /**
   * Return either {@link #toStringOneLine} if it is no more than {@code length} characters, or
   * {@link #toStringOneLine} quoted and truncated.
   *
   * @param tree a tree
   * @param length the maximum length for the result; must be at least 6
   * @return a one-line string representation of the tree that is no longer than {@code length}
   *     characters long
   */
  public static String toStringTruncated(Tree tree, int length) {
    if (length < 6) {
      throw new BugInCF("TreeUtils.toStringTruncated: bad length " + length);
    }
    String result = toStringOneLine(tree);
    if (result.length() > length) {
      // The quoting increases the likelihood that all delimiters are balanced in the result.
      // That makes it easier to manipulate the result (such as skipping over it) in an
      // editor.  The quoting also makes clear that the value is truncated.
      result = "\"" + result.substring(0, length - 5) + "...\"";
    }
    return result;
  }

  /**
   * Given a javac ExpressionTree representing a fully qualified name such as "java.lang.Object",
   * creates a String containing the name.
   *
   * @param nameExpr an ExpressionTree representing a fully qualified name
   * @return a String representation of the fully qualified name
   */
  public static String nameExpressionToString(ExpressionTree nameExpr) {
    TreeVisitor<String, Void> visitor =
        new SimpleTreeVisitor<String, Void>() {
          @Override
          public String visitIdentifier(IdentifierTree tree, Void p) {
            return tree.toString();
          }

          @Override
          public String visitMemberSelect(MemberSelectTree tree, Void p) {
            return tree.getExpression().accept(this, null) + "." + tree.getIdentifier().toString();
          }
        };
    return nameExpr.accept(visitor, null);
  }

  /**
   * Returns true if the binary operator may do a widening primitive conversion. See <a
   * href="https://docs.oracle.com/javase/specs/jls/se17/html/jls-5.html">JLS chapter 5</a>.
   *
   * @param tree a binary tree
   * @return true if the tree's operator does numeric promotion on its arguments
   */
  public static boolean isWideningBinary(BinaryTree tree) {
    switch (tree.getKind()) {
      case LEFT_SHIFT:
      case LEFT_SHIFT_ASSIGNMENT:
      case RIGHT_SHIFT:
      case RIGHT_SHIFT_ASSIGNMENT:
      case UNSIGNED_RIGHT_SHIFT:
      case UNSIGNED_RIGHT_SHIFT_ASSIGNMENT:
        // Strictly speaking, these operators do unary promotion on each argument
        // separately.
        return true;

      case MULTIPLY:
      case MULTIPLY_ASSIGNMENT:
      case DIVIDE:
      case DIVIDE_ASSIGNMENT:
      case REMAINDER:
      case REMAINDER_ASSIGNMENT:
      case PLUS:
      case PLUS_ASSIGNMENT:
      case MINUS:
      case MINUS_ASSIGNMENT:

      case LESS_THAN:
      case LESS_THAN_EQUAL:
      case GREATER_THAN:
      case GREATER_THAN_EQUAL:
      case EQUAL_TO:
      case NOT_EQUAL_TO:

      case AND:
      case XOR:
      case OR:
        // These operators do binary promotion on the two arguments together.
        return true;

        // TODO: CONDITIONAL_EXPRESSION (?:) sometimes does numeric promotion.

      default:
        return false;
    }
  }

  /**
   * Returns the annotations explicitly written on the given type.
   *
   * @param annoTrees annotations written before a variable/method declaration; null if this type is
   *     not from such a location. This might contain type annotations that the Java parser attached
   *     to the declaration rather than to the type.
   * @param typeTree the type whose annotations to return
   * @return the annotations explicitly written on the given type
   */
  public static List<? extends AnnotationTree> getExplicitAnnotationTrees(
      @Nullable List<? extends AnnotationTree> annoTrees, Tree typeTree) {
    while (true) {
      switch (typeTree.getKind()) {
        case IDENTIFIER:
        case PRIMITIVE_TYPE:
          if (annoTrees == null) {
            return Collections.emptyList();
          }
          return annoTrees;
        case ANNOTATED_TYPE:
          return ((AnnotatedTypeTree) typeTree).getAnnotations();
        case ARRAY_TYPE:
        case TYPE_PARAMETER:
        case UNBOUNDED_WILDCARD:
        case EXTENDS_WILDCARD:
        case SUPER_WILDCARD:
          return Collections.emptyList();
        case MEMBER_SELECT:
          if (annoTrees == null) {
            return Collections.emptyList();
          }
          typeTree = ((MemberSelectTree) typeTree).getExpression();
          break;
        case PARAMETERIZED_TYPE:
          typeTree = ((ParameterizedTypeTree) typeTree).getType();
          break;
        case UNION_TYPE:
          List<AnnotationTree> result = new ArrayList<>();
          for (Tree alternative : ((UnionTypeTree) typeTree).getTypeAlternatives()) {
            result.addAll(getExplicitAnnotationTrees(null, alternative));
          }
          return result;
        default:
          throw new BugInCF(
              "TreeUtils.getExplicitAnnotationTrees: what typeTree? %s %s %s",
              typeTree.getKind(), typeTree.getClass(), typeTree);
      }
    }
  }

  /**
   * Return a tree for the default value of the given type. The default value is 0, false, or null.
   *
   * @param typeMirror a type
   * @param processingEnv the processing environment
   * @return a tree for {@code type}'s default value
   */
  public static LiteralTree getDefaultValueTree(
      TypeMirror typeMirror, ProcessingEnvironment processingEnv) {
    typeMirror = TypeAnnotationUtils.unannotatedType(typeMirror);
    switch (typeMirror.getKind()) {
      case BYTE:
      case SHORT:
      case INT:
        // Byte should be (byte) 0, but this probably doesn't matter so just use int 0;
        // Short should be (short) 0, but this probably doesn't matter so just use int 0;
        return TreeUtils.createLiteral(TypeTag.INT, 0, typeMirror, processingEnv);
      case CHAR:
        // Value of a char literal needs to be stored as an integer because
        // LiteralTree#getValue
        // converts it from an integer to a char before being returned.
        return TreeUtils.createLiteral(TypeTag.CHAR, (int) '\u0000', typeMirror, processingEnv);
      case LONG:
        return TreeUtils.createLiteral(TypeTag.LONG, 0L, typeMirror, processingEnv);
      case FLOAT:
        return TreeUtils.createLiteral(TypeTag.FLOAT, 0.0f, typeMirror, processingEnv);
      case DOUBLE:
        return TreeUtils.createLiteral(TypeTag.DOUBLE, 0.0d, typeMirror, processingEnv);
      case BOOLEAN:
        // Value of a boolean literal needs to be stored as an integer because
        // LiteralTree#getValue
        // converts it from an integer to a boolean before being returned.
        return TreeUtils.createLiteral(TypeTag.BOOLEAN, 0, typeMirror, processingEnv);
      default:
        return TreeUtils.createLiteral(
            TypeTag.BOT, null, processingEnv.getTypeUtils().getNullType(), processingEnv);
    }
  }

  /**
   * Creates a LiteralTree for the given value.
   *
   * @param typeTag the literal's type tag
   * @param value a wrapped primitive, null, or a String
   * @param typeMirror the typeMirror for the literal
   * @param processingEnv the processing environment
   * @return a LiteralTree for the given type tag and value
   */
  public static LiteralTree createLiteral(
      TypeTag typeTag,
      @Nullable Object value,
      TypeMirror typeMirror,
      ProcessingEnvironment processingEnv) {
    Context context = ((JavacProcessingEnvironment) processingEnv).getContext();
    TreeMaker maker = TreeMaker.instance(context);
    LiteralTree result = maker.Literal(typeTag, value);
    ((JCLiteral) result).type = (Type) typeMirror;
    return result;
  }

  /**
   * Returns true if the given tree evaluates to {@code null}.
   *
   * @param t a tree
   * @return true if the given tree evaluates to {@code null}
   */
  public static boolean isNullExpression(Tree t) {
    while (true) {
      switch (t.getKind()) {
        case PARENTHESIZED:
          t = ((ParenthesizedTree) t).getExpression();
          break;
        case TYPE_CAST:
          t = ((TypeCastTree) t).getExpression();
          break;
        case NULL_LITERAL:
          return true;
        default:
          return false;
      }
    }
  }

  /**
   * Returns true if two expressions originating from the same scope are identical, i.e. they are
   * syntactically represented in the same way (modulo parentheses) and represent the same value.
   *
   * <p>If the expression includes one or more method calls, assumes the method calls are
   * deterministic.
   *
   * @param expr1 the first expression to compare
   * @param expr2 the second expression to compare; expr2 must originate from the same scope as
   *     expr1
   * @return true if the expressions expr1 and expr2 are syntactically identical
   */
  public static boolean sameTree(ExpressionTree expr1, ExpressionTree expr2) {
    expr1 = TreeUtils.withoutParens(expr1);
    expr2 = TreeUtils.withoutParens(expr2);
    // Converting to a string in order to compare is somewhat inefficient, and it doesn't handle
    // internal parentheses.  We could create a visitor instead.
    return expr1.getKind() == expr2.getKind() && expr1.toString().equals(expr2.toString());
  }

  /**
   * Returns true if this is the default case for a switch statement or expression.
   *
   * @param caseTree a case tree
   * @return true if {@code caseTree} is the default case for a switch statement or expression
   */
  public static boolean isDefaultCaseTree(CaseTree caseTree) {
    return caseTreeGetExpressions(caseTree).isEmpty();
  }

  /**
   * Get the list of expressions from a case expression. For the default case, this is empty.
   * Otherwise, in JDK 11 and earlier, this is a singleton list. In JDK 12 onwards, there can be
   * multiple expressions per case.
   *
   * @param caseTree the case expression to get the expressions from
   * @return the list of expressions in the case
   */
  public static List<? extends ExpressionTree> caseTreeGetExpressions(CaseTree caseTree) {
    try {
      if (atLeastJava12) {
        @SuppressWarnings({"unchecked", "nullness"})
        @NonNull List<? extends ExpressionTree> result =
            (List<? extends ExpressionTree>) CASETREE_GETEXPRESSIONS.invoke(caseTree);
        return result;
      } else {
        @SuppressWarnings("nullness")
        ExpressionTree expression = (ExpressionTree) CASETREE_GETEXPRESSION.invoke(caseTree);
        if (expression == null) {
          return Collections.emptyList();
        } else {
          return Collections.singletonList(expression);
        }
      }
    } catch (IllegalAccessException | InvocationTargetException e) {
      throw new BugInCF(
          "TreeUtils.caseTreeGetExpressions: reflection failed for tree: %s", caseTree, e);
    }
  }

  /**
   * Returns the body of the case statement if it is of the form {@code case <expression> ->
   * <expression>}. This method should only be called if {@link CaseTree#getStatements()} returns
   * null.
   *
   * @param caseTree the case expression to get the body from
   * @return the body of the case tree
   */
  public static @Nullable Tree caseTreeGetBody(CaseTree caseTree) {
    if (atLeastJava12) {
      try {
        @SuppressWarnings("nullness")
        Tree ret = (Tree) CASETREE_GETBODY.invoke(caseTree);
        return ret;
      } catch (IllegalAccessException | InvocationTargetException e) {
        throw new BugInCF("TreeUtils.caseTreeGetBody: reflection failed for tree: %s", caseTree, e);
      }
    } else {
      throw new BugInCF("TreeUtils.caseTreeGetBody: requires at least Java 12");
    }
  }

  /**
   * Returns the binding variable of {@code bindingPatternTree}.
   *
   * @param bindingPatternTree the BindingPatternTree whose binding variable is returned
   * @return the binding variable of {@code bindingPatternTree}
   */
  public static VariableTree bindingPatternTreeGetVariable(Tree bindingPatternTree) {
    if (atLeastJava16) {
      VariableTree variableTree;
      try {
        @SuppressWarnings("nullness")
        VariableTree localVT =
            (VariableTree) BINDINGPATTERNTREE_GETVARIABLE.invoke(bindingPatternTree);
        variableTree = localVT;
      } catch (IllegalAccessException | InvocationTargetException e) {
        throw new BugInCF(
            "TreeUtils.bindingPatternTreeGetVariable: reflection failed for tree: %s",
            bindingPatternTree, e);
      }
      if (variableTree != null) {
        return variableTree;
      }
      throw new BugInCF(
          "TreeUtils.bindingPatternTreeGetVariable: variable is null for tree: %s",
          bindingPatternTree);
    } else {
      throw new BugInCF("TreeUtils.bindingPatternTreeGetVariable: requires at least Java 16");
    }
  }

  /**
   * Returns the pattern of {@code instanceOfTree} tree. Returns null if the instanceof does not
   * have a pattern, including if the JDK version does not support instance-of patterns.
   *
   * @param instanceOfTree the {@link InstanceOfTree} whose pattern is returned
   * @return the {@code PatternTree} of {@code instanceOfTree} or null if it doesn't exist
   */
  public static @Nullable Tree instanceOfTreeGetPattern(InstanceOfTree instanceOfTree) {
    if (atLeastJava16) {
      try {
        @SuppressWarnings("nullness")
        Tree ret = (Tree) INSTANCEOFTREE_GETPATTERN.invoke(instanceOfTree);
        return ret;
      } catch (IllegalAccessException | InvocationTargetException e) {
        throw new BugInCF(
            "TreeUtils.instanceOfGetPattern: reflection failed for tree: %s", instanceOfTree, e);
      }
    } else {
      return null;
    }
  }

  /**
   * Returns the selector expression of {@code switchExpressionTree}. For example
   *
   * <pre>
   *   switch ( <em>expression</em> ) { ... }
   * </pre>
   *
   * @param switchExpressionTree the switch expression whose selector expression is returned
   * @return the selector expression of {@code switchExpressionTree}
   */
  public static ExpressionTree switchExpressionTreeGetExpression(Tree switchExpressionTree) {
    if (atLeastJava12) {
      ExpressionTree expressionTree;
      try {
        @SuppressWarnings("nullness")
        ExpressionTree localET =
            (ExpressionTree) SWITCHEXPRTREE_GETEXPRESSION.invoke(switchExpressionTree);
        expressionTree = localET;
      } catch (InvocationTargetException | IllegalAccessException e) {
        throw new BugInCF(
            "TreeUtils.switchExpressionTreeGetExpression: reflection failed for tree:" + " %s",
            switchExpressionTree, e);
      }
      if (expressionTree != null) {
        return expressionTree;
      }
      throw new BugInCF(
          "TreeUtils.switchExpressionTreeGetExpression: expression is null for tree:" + " %s",
          switchExpressionTree);
    } else {
      throw new BugInCF("TreeUtils.switchExpressionTreeGetExpression: requires at least Java 12");
    }
  }

  /**
   * Returns the cases of {@code switchExpressionTree}. For example
   *
   * <pre>
   *   switch ( <em>expression</em> ) {
   *     <em>cases</em>
   *   }
   * </pre>
   *
   * @param switchExpressionTree the switch expression whose cases are returned
   * @return the cases of {@code switchExpressionTree}
   */
  public static List<? extends CaseTree> switchExpressionTreeGetCases(Tree switchExpressionTree) {
    if (atLeastJava12) {
      List<? extends CaseTree> cases;
      try {
        @SuppressWarnings({"unchecked", "nullness"})
        List<? extends CaseTree> localcases =
            (List<? extends CaseTree>) SWITCHEXPRTREE_GETCASES.invoke(switchExpressionTree);
        cases = localcases;
      } catch (InvocationTargetException | IllegalAccessException e) {
        throw new BugInCF(
            "TreeUtils.switchExpressionTreeGetCases: reflection failed for tree: %s",
            switchExpressionTree, e);
      }
      if (cases != null) {
        return cases;
      }
      throw new BugInCF(
          "TreeUtils.switchExpressionTreeGetCases: cases is null for tree: %s",
          switchExpressionTree);
    } else {
      throw new BugInCF("TreeUtils.switchExpressionTreeGetCases: requires at least Java 12");
    }
  }

  /**
   * Returns true if the given tree is a switch statement (as opposed to a switch expression).
   *
   * @param tree the switch statement or expression to check
   * @return true if the given tree is a switch statement (as opposed to a switch expression)
   */
  public static boolean isSwitchStatement(Tree tree) {
    return tree.getKind() == Tree.Kind.SWITCH;
  }

  /**
   * Returns the value (expression) for {@code yieldTree}.
   *
   * @param yieldTree the yield tree
   * @return the value (expression) for {@code yieldTree}
   */
  public static ExpressionTree yieldTreeGetValue(Tree yieldTree) {
    if (atLeastJava13) {
      ExpressionTree expressionTree;
      try {
        @SuppressWarnings("nullness")
        ExpressionTree localET = (ExpressionTree) YIELDTREE_GETVALUE.invoke(yieldTree);
        expressionTree = localET;
      } catch (InvocationTargetException | IllegalAccessException e) {
        throw new BugInCF(
            "TreeUtils.yieldTreeGetValue: reflection failed for tree: %s", yieldTree, e);
      }
      if (expressionTree != null) {
        return expressionTree;
      }
      throw new BugInCF("TreeUtils.yieldTreeGetValue: expression is null for tree: %s", yieldTree);
    } else {
      throw new BugInCF("TreeUtils.yieldTreeGetValue: requires at least Java 13");
    }
  }

  /**
   * Returns true if the given method/constructor invocation is a varargs invocation.
   *
   * @param tree a method/constructor invocation
   * @return true if the given method/constructor invocation is a varargs invocation
   */
  public static boolean isVarArgs(Tree tree) {
    switch (tree.getKind()) {
      case METHOD_INVOCATION:
        return isVarArgs((MethodInvocationTree) tree);
      case NEW_CLASS:
        return isVarArgs((NewClassTree) tree);
      default:
        throw new BugInCF("TreeUtils.isVarArgs: unexpected kind of tree: " + tree);
    }
  }

  /**
   * Returns true if the given method invocation is a varargs invocation.
   *
   * @param invok the method invocation
   * @return true if the given method invocation is a varargs invocation
   */
  public static boolean isVarArgs(MethodInvocationTree invok) {
    return isVarArgs(elementFromUse(invok), invok.getArguments());
  }

  /**
   * Returns true if the given constructor invocation is a varargs invocation.
   *
   * @param newClassTree the constructor invocation
   * @return true if the given method invocation is a varargs invocation
   */
  public static boolean isVarArgs(NewClassTree newClassTree) {
    return isVarArgs(elementFromUse(newClassTree), newClassTree.getArguments());
  }

  /**
   * Returns true if a method/constructor invocation is a varargs invocation.
   *
   * @param method the method or constructor
   * @param args the arguments passed at the invocation
   * @return true if the given method/constructor invocation is a varargs invocation
   */
  private static boolean isVarArgs(ExecutableElement method, List<? extends ExpressionTree> args) {
    if (!method.isVarArgs()) {
      return false;
    }

    List<? extends VariableElement> parameters = method.getParameters();
    if (parameters.size() != args.size()) {
      return true;
    }

    TypeMirror lastArgType = typeOf(args.get(args.size() - 1));
    if (lastArgType.getKind() == TypeKind.NULL) {
      return false;
    }
    if (lastArgType.getKind() != TypeKind.ARRAY) {
      return true;
    }

    TypeMirror varargsParamType = parameters.get(parameters.size() - 1).asType();
    return TypesUtils.getArrayDepth(varargsParamType) != TypesUtils.getArrayDepth(lastArgType);
  }

  /**
   * Calls getKind() on the given tree, but returns CLASS if the Kind is RECORD. This is needed
   * because the Checker Framework runs on JDKs before the RECORD item was added, so RECORD can't be
   * used in case statements, and usually we want to treat them the same as classes.
   *
   * @param tree the tree to get the kind for
   * @return the kind of the tree, but CLASS if the kind was RECORD
   */
  public static Tree.Kind getKindRecordAsClass(Tree tree) {
    Tree.Kind kind = tree.getKind();
    // Must use String comparison because we may be on an older JDK:
    if (kind.name().equals("RECORD")) {
      kind = Tree.Kind.CLASS;
    }
    return kind;
  }

  /**
   * Returns true if the {@code tree} is a binary tree that performs a comparison.
   *
   * @param tree the tree to check
   * @return whether the tree represents a binary comparison
   */
  public static boolean isBinaryComparison(BinaryTree tree) {
    return BINARY_COMPARISON_TREE_KINDS.contains(tree.getKind());
  }
}
