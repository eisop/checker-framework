package org.checkerframework.dataflow.cfg.node;

import com.sun.source.tree.Tree;
import java.util.Collection;
import java.util.Collections;
import java.util.Objects;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.dataflow.qual.SideEffectFree;
import org.checkerframework.javacutil.TreeUtils;

/**
 * A node for the unary 'nullchk' operation (generated by the Java compiler):
 *
 * <pre>
 *   &lt;*nullchk*&gt;<em>expression</em>
 * </pre>
 */
public class NullChkNode extends Node {
  /** The entire tree of the null check */
  protected final Tree tree;
  /** The operand of the null check */
  protected final Node operand;

  /**
   * Constructs a {@link NullChkNode}.
   *
   * @param tree the nullchk tree
   * @param operand the operand of the null check
   */
  public NullChkNode(Tree tree, Node operand) {
    super(TreeUtils.typeOf(tree));
    assert tree.getKind() == Tree.Kind.OTHER;
    this.tree = tree;
    this.operand = operand;
  }

  public Node getOperand() {
    return operand;
  }

  @Override
  public Tree getTree() {
    return tree;
  }

  @Override
  public <R, P> R accept(NodeVisitor<R, P> visitor, P p) {
    return visitor.visitNullChk(this, p);
  }

  @Override
  public String toString() {
    return "(+ " + getOperand() + ")";
  }

  @Override
  public boolean equals(@Nullable Object obj) {
    if (!(obj instanceof NumericalPlusNode)) {
      return false;
    }
    NumericalPlusNode other = (NumericalPlusNode) obj;
    return getOperand().equals(other.getOperand());
  }

  @Override
  public int hashCode() {
    return Objects.hash(NullChkNode.class, getOperand());
  }

  @Override
  @SideEffectFree
  public Collection<Node> getOperands() {
    return Collections.singletonList(getOperand());
  }
}
