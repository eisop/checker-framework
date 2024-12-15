package org.checkerframework.dataflow.cfg.node;

/**
 * A visitor for a {@link Node} tree.
 *
 * @param <R> return type of the visitor. Use {@link Void} if the visitor does not have a return
 *     value.
 * @param <P> parameter type of the visitor. Use {@link Void} if the visitor does not have a
 *     parameter.
 */
public interface NodeVisitor<R, P> {
    // Literals
    /**
     * Visits a short literal node.
     *
     * @param n the {@link ShortLiteralNode} to be visited
     * @param p the argument for the operation implemented by this visitor
     * @return the return value of the operation implemented by this visitor
     */
    R visitShortLiteral(ShortLiteralNode n, P p);

    /**
     * Visits an integer literal node.
     *
     * @param n the {@link IntegerLiteralNode} to be visited
     * @param p the argument for the operation implemented by this visitor
     * @return the return value of the operation implemented by this visitor
     */
    R visitIntegerLiteral(IntegerLiteralNode n, P p);

    /**
     * Visits a long literal node.
     *
     * @param n the {@link LongLiteralNode} to be visited
     * @param p the argument for the operation implemented by this visitor
     * @return the return value of the operation implemented by this visitor
     */
    R visitLongLiteral(LongLiteralNode n, P p);

    /**
     * Visits a float literal node.
     *
     * @param n the {@link FloatLiteralNode} to be visited
     * @param p the argument for the operation implemented by this visitor
     * @return the return value of the operation implemented by this visitor
     */
    R visitFloatLiteral(FloatLiteralNode n, P p);

    /**
     * Visits a double literal node.
     *
     * @param n the {@link DoubleLiteralNode} to be visited
     * @param p the argument for the operation implemented by this visitor
     * @return the return value of the operation implemented by this visitor
     */
    R visitDoubleLiteral(DoubleLiteralNode n, P p);

    /**
     * Visits a boolean literal node.
     *
     * @param n the {@link BooleanLiteralNode} to be visited
     * @param p the argument for the operation implemented by this visitor
     * @return the return value of the operation implemented by this visitor
     */
    R visitBooleanLiteral(BooleanLiteralNode n, P p);

    /**
     * Visits a character literal node.
     *
     * @param n the {@link CharacterLiteralNode} to be visited
     * @param p the argument for the operation implemented by this visitor
     * @return the return value of the operation implemented by this visitor
     */
    R visitCharacterLiteral(CharacterLiteralNode n, P p);

    /**
     * Visits a string literal node.
     *
     * @param n the {@link StringLiteralNode} to be visited
     * @param p the argument for the operation implemented by this visitor
     * @return the return value of the operation implemented by this visitor
     */
    R visitStringLiteral(StringLiteralNode n, P p);

    /**
     * Visits a null literal node.
     *
     * @param n the {@link NullLiteralNode} to be visited
     * @param p the argument for the operation implemented by this visitor
     * @return the return value of the operation implemented by this visitor
     */
    R visitNullLiteral(NullLiteralNode n, P p);

    // Unary operations
    /**
     * Visits a unary minus node.
     *
     * @param n the {@link NumericalMinusNode} to be visited
     * @param p the argument for the operation implemented by this visitor
     * @return the return value of the operation implemented by this visitor
     */
    R visitNumericalMinus(NumericalMinusNode n, P p);

    /**
     * Visits a unary plus node.
     *
     * @param n the {@link NumericalPlusNode} to be visited
     * @param p the argument for the operation implemented by this visitor
     * @return the return value of the operation implemented by this visitor
     */
    R visitNumericalPlus(NumericalPlusNode n, P p);

    /**
     * Visits a bitwise complement node.
     *
     * @param n the {@link BitwiseComplementNode} to be visited
     * @param p the argument for the operation implemented by this visitor
     * @return the return value of the operation implemented by this visitor
     */
    R visitBitwiseComplement(BitwiseComplementNode n, P p);

    /**
     * Visits a NullChk node.
     *
     * @param n the {@link NullChkNode} to be visited
     * @param p the argument for the operation implemented by this visitor
     * @return the return value of the operation implemented by this visitor
     */
    R visitNullChk(NullChkNode n, P p);

    // Binary operations
    /**
     * Visits a string concatenation node.
     *
     * @param n the {@link StringConcatenateNode} to be visited
     * @param p the argument for the operation implemented by this visitor
     * @return the return value of the operation implemented by this visitor
     */
    R visitStringConcatenate(StringConcatenateNode n, P p);

    /**
     * Visits a numerical addition node.
     *
     * @param n the {@link NumericalAdditionNode} to be visited
     * @param p the argument for the operation implemented by this visitor
     * @return the return value of the operation implemented by this visitor
     */
    R visitNumericalAddition(NumericalAdditionNode n, P p);

    /**
     * Visits a numerical subtraction node.
     *
     * @param n the {@link NumericalSubtractionNode} to be visited
     * @param p the argument for the operation implemented by this visitor
     * @return the return value of the operation implemented by this visitor
     */
    R visitNumericalSubtraction(NumericalSubtractionNode n, P p);

    /**
     * Visits a numerical multiplication node.
     *
     * @param n the {@link NumericalMultiplicationNode} to be visited
     * @param p the argument for the operation implemented by this visitor
     * @return the return value of the operation implemented by this visitor
     */
    R visitNumericalMultiplication(NumericalMultiplicationNode n, P p);

    /**
     * Visits a integer division node.
     *
     * @param n the {@link IntegerDivisionNode} to be visited
     * @param p the argument for the operation implemented by this visitor
     * @return the return value of the operation implemented by this visitor
     */
    R visitIntegerDivision(IntegerDivisionNode n, P p);

    /**
     * Visits a floating division node.
     *
     * @param n the {@link FloatingDivisionNode} to be visited
     * @param p the argument for the operation implemented by this visitor
     * @return the return value of the operation implemented by this visitor
     */
    R visitFloatingDivision(FloatingDivisionNode n, P p);

    /**
     * Visits a integer remainder node.
     *
     * @param n the {@link IntegerRemainderNode} to be visited
     * @param p the argument for the operation implemented by this visitor
     * @return the return value of the operation implemented by this visitor
     */
    R visitIntegerRemainder(IntegerRemainderNode n, P p);

    /**
     * Visits a floating remainder node.
     *
     * @param n the {@link FloatingRemainderNode} to be visited
     * @param p the argument for the operation implemented by this visitor
     * @return the return value of the operation implemented by this visitor
     */
    R visitFloatingRemainder(FloatingRemainderNode n, P p);

    /**
     * Visits a left shift node.
     *
     * @param n the {@link LeftShiftNode} to be visited
     * @param p the argument for the operation implemented by this visitor
     * @return the return value of the operation implemented by this visitor
     */
    R visitLeftShift(LeftShiftNode n, P p);

    /**
     * Visits a signed right shift node.
     *
     * @param n the {@link SignedRightShiftNode} to be visited
     * @param p the argument for the operation implemented by this visitor
     * @return the return value of the operation implemented by this visitor
     */
    R visitSignedRightShift(SignedRightShiftNode n, P p);

    /**
     * Visits an unsigned right shift node.
     *
     * @param n the {@link UnsignedRightShiftNode} to be visited
     * @param p the argument for the operation implemented by this visitor
     * @return the return value of the operation implemented by this visitor
     */
    R visitUnsignedRightShift(UnsignedRightShiftNode n, P p);

    /**
     * Visits a bitwise and node.
     *
     * @param n the {@link BitwiseAndNode} to be visited
     * @param p the argument for the operation implemented by this visitor
     * @return the return value of the operation implemented by this visitor
     */
    R visitBitwiseAnd(BitwiseAndNode n, P p);

    /**
     * Visits a bitwise or node.
     *
     * @param n the {@link BitwiseOrNode} to be visited
     * @param p the argument for the operation implemented by this visitor
     * @return the return value of the operation implemented by this visitor
     */
    R visitBitwiseOr(BitwiseOrNode n, P p);

    /**
     * Visits a bitwise xor node.
     *
     * @param n the {@link BitwiseXorNode} to be visited
     * @param p the argument for the operation implemented by this visitor
     * @return the return value of the operation implemented by this visitor
     */
    R visitBitwiseXor(BitwiseXorNode n, P p);

    // Comparison operations
    /**
     * Visits a less than node.
     *
     * @param n the {@link LessThanNode} to be visited
     * @param p the argument for the operation implemented by this visitor
     * @return the return value of the operation implemented by this visitor
     */
    R visitLessThan(LessThanNode n, P p);

    /**
     * Visits a less than or equal node.
     *
     * @param n the {@link LessThanOrEqualNode} to be visited
     * @param p the argument for the operation implemented by this visitor
     * @return the return value of the operation implemented by this visitor
     */
    R visitLessThanOrEqual(LessThanOrEqualNode n, P p);

    /**
     * Visits a greater than node.
     *
     * @param n the {@link GreaterThanNode} to be visited
     * @param p the argument for the operation implemented by this visitor
     * @return the return value of the operation implemented by this visitor
     */
    R visitGreaterThan(GreaterThanNode n, P p);

    /**
     * Visits a greater than or equal node.
     *
     * @param n the {@link GreaterThanOrEqualNode} to be visited
     * @param p the argument for the operation implemented by this visitor
     * @return the return value of the operation implemented by this visitor
     */
    R visitGreaterThanOrEqual(GreaterThanOrEqualNode n, P p);

    /**
     * Visits an equal to node.
     *
     * @param n the {@link EqualToNode} to be visited
     * @param p the argument for the operation implemented by this visitor
     * @return the return value of the operation implemented by this visitor
     */
    R visitEqualTo(EqualToNode n, P p);

    /**
     * Visits a not equal node.
     *
     * @param n the {@link NotEqualNode} to be visited
     * @param p the argument for the operation implemented by this visitor
     * @return the return value of the operation implemented by this visitor
     */
    R visitNotEqual(NotEqualNode n, P p);

    // Conditional operations
    /**
     * Visits a conditional and node.
     *
     * @param n the {@link ConditionalAndNode} to be visited
     * @param p the argument for the operation implemented by this visitor
     * @return the return value of the operation implemented by this visitor
     */
    R visitConditionalAnd(ConditionalAndNode n, P p);

    /**
     * Visits a conditional or node.
     *
     * @param n the {@link ConditionalOrNode} to be visited
     * @param p the argument for the operation implemented by this visitor
     * @return the return value of the operation implemented by this visitor
     */
    R visitConditionalOr(ConditionalOrNode n, P p);

    /**
     * Visits a conditional not node.
     *
     * @param n the {@link ConditionalNotNode} to be visited
     * @param p the argument for the operation implemented by this visitor
     * @return the return value of the operation implemented by this visitor
     */
    R visitConditionalNot(ConditionalNotNode n, P p);

    /**
     * Visits a ternary expression node.
     *
     * @param n the {@link TernaryExpressionNode} to be visited
     * @param p the argument for the operation implemented by this visitor
     * @return the return value of the operation implemented by this visitor
     */
    R visitTernaryExpression(TernaryExpressionNode n, P p);

    /**
     * Visits a switch expression node.
     *
     * @param n the {@link SwitchExpressionNode} to be visited
     * @param p the argument for the operation implemented by this visitor
     * @return the return value of the operation implemented by this visitor
     */
    R visitSwitchExpressionNode(SwitchExpressionNode n, P p);

    /**
     * Visits a switch expression node.
     *
     * @param n the {@link SwitchExpressionNode} to be visited
     * @param p the argument for the operation implemented by this visitor
     * @return the return value of the operation implemented by this visitor
     */
    R visitAssignment(AssignmentNode n, P p);

    /**
     * Visits a local variable node.
     *
     * @param n the {@link LocalVariableNode} to be visited
     * @param p the argument for the operation implemented by this visitor
     * @return the return value of the operation implemented by this visitor
     */
    R visitLocalVariable(LocalVariableNode n, P p);

    /**
     * Visits a variable declaration node.
     *
     * @param n the {@link VariableDeclarationNode} to be visited
     * @param p the argument for the operation implemented by this visitor
     * @return the return value of the operation implemented by this visitor
     */
    R visitVariableDeclaration(VariableDeclarationNode n, P p);

    /**
     * Visits a field access node.
     *
     * @param n the {@link FieldAccessNode} to be visited
     * @param p the argument for the operation implemented by this visitor
     * @return the return value of the operation implemented by this visitor
     */
    R visitFieldAccess(FieldAccessNode n, P p);

    /**
     * Visits a method access node.
     *
     * @param n the {@link MethodAccessNode} to be visited
     * @param p the argument for the operation implemented by this visitor
     * @return the return value of the operation implemented by this visitor
     */
    R visitMethodAccess(MethodAccessNode n, P p);

    /**
     * Visits an array access node.
     *
     * @param n the {@link ArrayAccessNode} to be visited
     * @param p the argument for the operation implemented by this visitor
     * @return the return value of the operation implemented by this visitor
     */
    R visitArrayAccess(ArrayAccessNode n, P p);

    /**
     * Visits an implicit this node.
     *
     * @param n the {@link ImplicitThisNode} to be visited
     * @param p the argument for the operation implemented by this visitor
     * @return the return value of the operation implemented by this visitor
     */
    R visitImplicitThis(ImplicitThisNode n, P p);

    /**
     * Visits an explicit this node.
     *
     * @param n the {@link ExplicitThisNode} to be visited
     * @param p the argument for the operation implemented by this visitor
     * @return the return value of the operation implemented by this visitor
     */
    R visitExplicitThis(ExplicitThisNode n, P p);

    /**
     * Visits a super node.
     *
     * @param n the {@link SuperNode} to be visited
     * @param p the argument for the operation implemented by this visitor
     * @return the return value of the operation implemented by this visitor
     */
    R visitSuper(SuperNode n, P p);

    /**
     * Visits a return node.
     *
     * @param n the {@link ReturnNode} to be visited
     * @param p the argument for the operation implemented by this visitor
     * @return the return value of the operation implemented by this visitor
     */
    R visitReturn(ReturnNode n, P p);

    /**
     * Visits a lambda result expression node.
     *
     * @param n the {@link LambdaResultExpressionNode} to be visited
     * @param p the argument for the operation implemented by this visitor
     * @return the return value of the operation implemented by this visitor
     */
    R visitLambdaResultExpression(LambdaResultExpressionNode n, P p);

    /**
     * Visits a string conversion node.
     *
     * @param n the {@link StringConversionNode} to be visited
     * @param p the argument for the operation implemented by this visitor
     * @return the return value of the operation implemented by this visitor
     */
    R visitStringConversion(StringConversionNode n, P p);

    /**
     * Visits a widening conversion node.
     *
     * @param n the {@link WideningConversionNode} to be visited
     * @param p the argument for the operation implemented by this visitor
     * @return the return value of the operation implemented by this visitor
     */
    R visitWideningConversion(WideningConversionNode n, P p);

    /**
     * Visits a narrowing conversion node.
     *
     * @param n the {@link NarrowingConversionNode} to be visited
     * @param p the argument for the operation implemented by this visitor
     * @return the return value of the operation implemented by this visitor
     */
    R visitNarrowingConversion(NarrowingConversionNode n, P p);

    /**
     * Visits an instance of node.
     *
     * @param n the {@link InstanceOfNode} to be visited
     * @param p the argument for the operation implemented by this visitor
     * @return the return value of the operation implemented by this visitor
     */
    R visitInstanceOf(InstanceOfNode n, P p);

    /**
     * Visits a type cast node.
     *
     * @param n the {@link TypeCastNode} to be visited
     * @param p the argument for the operation implemented by this visitor
     * @return the return value of the operation implemented by this visitor
     */
    R visitTypeCast(TypeCastNode n, P p);

    // Blocks
    /**
     * Visits a synchronized node.
     *
     * @param n the {@link SynchronizedNode} to be visited
     * @param p the argument for the operation implemented by this visitor
     * @return the return value of the operation implemented by this visitor
     */
    R visitSynchronized(SynchronizedNode n, P p);

    // Statements
    /**
     * Visits an assertion error node.
     *
     * @param n the {@link AssertionErrorNode} to be visited
     * @param p the argument for the operation implemented by this visitor
     * @return the return value of the operation implemented by this visitor
     */
    R visitAssertionError(AssertionErrorNode n, P p);

    /**
     * Visits a throw node.
     *
     * @param n the {@link ThrowNode} to be visited
     * @param p the argument for the operation implemented by this visitor
     * @return the return value of the operation implemented by this visitor
     */
    R visitThrow(ThrowNode n, P p);

    // Cases
    /**
     * Visits a case node.
     *
     * @param n the {@link CaseNode} to be visited
     * @param p the argument for the operation implemented by this visitor
     * @return the return value of the operation implemented by this visitor
     */
    R visitCase(CaseNode n, P p);

    // Method and constructor invocations
    /**
     * Visits a method invocation node.
     *
     * @param n the {@link MethodInvocationNode} to be visited
     * @param p the argument for the operation implemented by this visitor
     * @return the return value of the operation implemented by this visitor
     */
    R visitMethodInvocation(MethodInvocationNode n, P p);

    /**
     * Visits a object creation node.
     *
     * @param n the {@link ObjectCreationNode} to be visited
     * @param p the argument for the operation implemented by this visitor
     * @return the return value of the operation implemented by this visitor
     */
    R visitObjectCreation(ObjectCreationNode n, P p);

    /**
     * Visits a Member Reference node.
     *
     * @param n the {@link FunctionalInterfaceNode} to be visited
     * @param p the argument for the operation implemented by this visitor
     * @return the return value of the operation implemented by this visitor
     */
    R visitMemberReference(FunctionalInterfaceNode n, P p);

    /**
     * Visits an array creation node.
     *
     * @param n the {@link ArrayCreationNode} to be visited
     * @param p the argument for the operation implemented by this visitor
     * @return the return value of the operation implemented by this visitor
     */
    R visitArrayCreation(ArrayCreationNode n, P p);

    // Type, package and class names
    /**
     * Visits an array type node.
     *
     * @param n the {@link ArrayTypeNode} to be visited
     * @param p the argument for the operation implemented by this visitor
     * @return the return value of the operation implemented by this visitor
     */
    R visitArrayType(ArrayTypeNode n, P p);

    /**
     * Visits a primitive type node.
     *
     * @param n the {@link PrimitiveTypeNode} to be visited
     * @param p the argument for the operation implemented by this visitor
     * @return the return value of the operation implemented by this visitor
     */
    R visitPrimitiveType(PrimitiveTypeNode n, P p);

    /**
     * Visits a class name node.
     *
     * @param n the {@link ClassNameNode} to be visited
     * @param p the argument for the operation implemented by this visitor
     * @return the return value of the operation implemented by this visitor
     */
    R visitClassName(ClassNameNode n, P p);

    /**
     * Visits a package name node.
     *
     * @param n the {@link PackageNameNode} to be visited
     * @param p the argument for the operation implemented by this visitor
     * @return the return value of the operation implemented by this visitor
     */
    R visitPackageName(PackageNameNode n, P p);

    // Parameterized types
    /**
     * Visits a parameterized type node.
     *
     * @param n the {@link ParameterizedTypeNode} to be visited
     * @param p the argument for the operation implemented by this visitor
     * @return the return value of the operation implemented by this visitor
     */
    R visitParameterizedType(ParameterizedTypeNode n, P p);

    // Marker nodes
    /**
     * Visits a marker node.
     *
     * @param n the {@link MarkerNode} to be visited
     * @param p the argument for the operation implemented by this visitor
     * @return the return value of the operation implemented by this visitor
     */
    R visitMarker(MarkerNode n, P p);

    /**
     * Visits an anonymous/inner/nested class declaration within a method.
     *
     * @param classDeclarationNode the {@link ClassDeclarationNode} to be visited
     * @param p the argument for the operation implemented by this visitor
     * @return the return value of the operation implemented by this visitor
     */
    R visitClassDeclaration(ClassDeclarationNode classDeclarationNode, P p);

    /**
     * Visits an expression that is used as a statement. This node is a marker after the expression
     * node(s).
     *
     * @param n the {@link ExpressionStatementNode} to be visited
     * @param p the argument for the operation implemented by this visitor
     * @return the return value of the operation implemented by this visitor
     */
    R visitExpressionStatement(ExpressionStatementNode n, P p);

    /**
     * Visits a deconstructor pattern node.
     *
     * @param n the {@link DeconstructorPatternNode} to be visited
     * @param p the argument for the operation implemented by this visitor
     * @return the return value of the operation implemented by this visitor
     */
    R visitDeconstructorPattern(DeconstructorPatternNode n, P p);
}
