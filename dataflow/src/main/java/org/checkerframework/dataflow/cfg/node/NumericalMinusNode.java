package org.checkerframework.dataflow.cfg.node;

import com.sun.source.tree.Tree;
import com.sun.source.tree.UnaryTree;
import java.util.Objects;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * A node for the unary minus operation:
 *
 * <pre>
 *   - <em>expression</em>
 * </pre>
 */
public class NumericalMinusNode extends UnaryOperationNode {

  public NumericalMinusNode(UnaryTree tree, Node operand) {
    super(tree, operand);
    assert tree.getKind() == Tree.Kind.UNARY_MINUS;
  }

  @Override
  public <R, P> R accept(NodeVisitor<R, P> visitor, P p) {
    return visitor.visitNumericalMinus(this, p);
  }

  @Override
  public String toString() {
    return "(- " + getOperand() + ")";
  }

  @Override
  public boolean equals(@Nullable Object obj) {
    if (!(obj instanceof NumericalMinusNode)) {
      return false;
    }
    NumericalMinusNode other = (NumericalMinusNode) obj;
    return getOperand().equals(other.getOperand());
  }

  @Override
  public int hashCode() {
    return Objects.hash(NumericalMinusNode.class, getOperand());
  }
}
