package org.checkerframework.checker.initialization;

import com.sun.source.tree.ClassTree;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.common.basetype.BaseTypeVisitor;

/** The visitor for the {@link InitializationFieldAccessSubchecker}. */
public class InitializationFieldAccessVisitor
    extends BaseTypeVisitor<InitializationFieldAccessAnnotatedTypeFactory> {

  /**
   * Create an InitializationFieldAccessVisitor.
   *
   * @param checker the initialization field-access checker
   */
  public InitializationFieldAccessVisitor(BaseTypeChecker checker) {
    super(checker);
  }

  @Override
  public void processClassTree(ClassTree classTree) {
    // As stated in the documentation for the InitializationFieldAccessChecker
    // and InitializationChecker, this checker performs the flow analysis
    // (which is handled in the BaseTypeVisitor), but does not perform
    // any type checking.
    // Thus, this method does nothing.
  }
}
