package org.checkerframework.dataflow.cfg.node;

/**
 * A default implementation of the node visitor interface. The class introduces several 'summary'
 * methods, that can be overridden to change the behavior of several related visit methods at once.
 *
 * <p>The summary methods and the nodes they handle are:
 *
 * <ul>
 *   <li>{@link #visitNode} - All {@code visit*} methods eventually call this method by default.
 *       <ul>
 *         <li>{@link #visitValueLiteral} - {@link ValueLiteralNode}s
 *         <li>{@link #visitUnaryOperation} - {@link UnaryOperationNode}s
 *         <li>{@link #visitBinaryOperation} - {@link BinaryOperationNode}s
 *             <ul>
 *               <li>{@link #visitBinaryNumericalOperation} - numerical operations (arithmetic,
 *                   bitwise, shifts)
 *               <li>{@link #visitBinaryComparisonOperation} - comparison operations ({@code <},
 *                   {@code <=} {@code >}, {@code >=}, {@code ==}, {@code !=})
 *               <li>{@link #visitBinaryConditionalOperation} - conditional operations ({@code &&},
 *                   {@code ||})
 *             </ul>
 *         <li>{@link #visitThis} - {@code this} references (implicit and explicit)
 *       </ul>
 * </ul>
 *
 * <p>This is useful to implement a visitor that performs the same operation (e.g., nothing) for
 * most {@link Node}s and only has special behavior for a few.
 *
 * @param <R> return type of the visitor
 * @param <P> parameter type of the visitor
 */
public abstract class AbstractNodeVisitor<R, P> implements NodeVisitor<R, P> {

    /**
     * Visits a node. All other visit methods delegate to this method by default.
     *
     * @param n The node to visit.
     * @param p The input for the visitor.
     * @return The output produced by the visitor.
     */
    public abstract R visitNode(Node n, P p);

    // Literals

    /**
     * Visits a value literal.
     *
     * @param p The input for the visitor.
     * @return The output produced by the visitor.
     */
    public R visitValueLiteral(ValueLiteralNode n, P p) {
        return visitNode(n, p);
    }

    @Override
    public R visitShortLiteral(ShortLiteralNode n, P p) {
        return visitValueLiteral(n, p);
    }

    @Override
    public R visitIntegerLiteral(IntegerLiteralNode n, P p) {
        return visitValueLiteral(n, p);
    }

    @Override
    public R visitLongLiteral(LongLiteralNode n, P p) {
        return visitValueLiteral(n, p);
    }

    @Override
    public R visitFloatLiteral(FloatLiteralNode n, P p) {
        return visitValueLiteral(n, p);
    }

    @Override
    public R visitDoubleLiteral(DoubleLiteralNode n, P p) {
        return visitValueLiteral(n, p);
    }

    @Override
    public R visitBooleanLiteral(BooleanLiteralNode n, P p) {
        return visitValueLiteral(n, p);
    }

    @Override
    public R visitCharacterLiteral(CharacterLiteralNode n, P p) {
        return visitValueLiteral(n, p);
    }

    @Override
    public R visitStringLiteral(StringLiteralNode n, P p) {
        return visitValueLiteral(n, p);
    }

    @Override
    public R visitNullLiteral(NullLiteralNode n, P p) {
        return visitValueLiteral(n, p);
    }

    // Unary operations

    /**
     * Visits any unary operation.
     *
     * @param n The unary operation node to visit.
     * @param p The input for the visitor.
     * @return The output produced by the visitor.
     */
    public R visitUnaryOperation(UnaryOperationNode n, P p) {
        return visitNode(n, p);
    }

    @Override
    public R visitNumericalMinus(NumericalMinusNode n, P p) {
        return visitUnaryOperation(n, p);
    }

    @Override
    public R visitNumericalPlus(NumericalPlusNode n, P p) {
        return visitUnaryOperation(n, p);
    }

    @Override
    public R visitBitwiseComplement(BitwiseComplementNode n, P p) {
        return visitUnaryOperation(n, p);
    }

    @Override
    public R visitNullChk(NullChkNode n, P p) {
        return visitNode(n, p);
    }

    // Binary operations

    /**
     * Visits any binary operation.
     *
     * @param n The binary operation node to visit.
     * @param p The input for the visitor.
     * @return The output produced by the visitor.
     */
    public R visitBinaryOperation(BinaryOperationNode n, P p) {
        return visitNode(n, p);
    }

    @Override
    public R visitStringConcatenate(StringConcatenateNode n, P p) {
        return visitBinaryOperation(n, p);
    }

    /**
     * Visits a binary numerical operation node ({@code +}, {@code -}, {@code *}, {@code /}, {@code
     * %}, {@code <<}, {@code >>}, {@code >>>}, {@code &}, {@code |}, {@code ^}).
     *
     * @param n The binary numerical operation node to visit.
     * @param p The input for the visitor.
     * @return The output produced by the visitor.
     */
    public R visitBinaryNumericalOperation(BinaryOperationNode n, P p) {
        return visitBinaryOperation(n, p);
    }

    @Override
    public R visitNumericalAddition(NumericalAdditionNode n, P p) {
        return visitBinaryNumericalOperation(n, p);
    }

    @Override
    public R visitNumericalSubtraction(NumericalSubtractionNode n, P p) {
        return visitBinaryNumericalOperation(n, p);
    }

    @Override
    public R visitNumericalMultiplication(NumericalMultiplicationNode n, P p) {
        return visitBinaryNumericalOperation(n, p);
    }

    @Override
    public R visitIntegerDivision(IntegerDivisionNode n, P p) {
        return visitBinaryNumericalOperation(n, p);
    }

    @Override
    public R visitFloatingDivision(FloatingDivisionNode n, P p) {
        return visitBinaryNumericalOperation(n, p);
    }

    @Override
    public R visitIntegerRemainder(IntegerRemainderNode n, P p) {
        return visitBinaryNumericalOperation(n, p);
    }

    @Override
    public R visitFloatingRemainder(FloatingRemainderNode n, P p) {
        return visitBinaryNumericalOperation(n, p);
    }

    @Override
    public R visitLeftShift(LeftShiftNode n, P p) {
        return visitBinaryNumericalOperation(n, p);
    }

    @Override
    public R visitSignedRightShift(SignedRightShiftNode n, P p) {
        return visitBinaryNumericalOperation(n, p);
    }

    @Override
    public R visitUnsignedRightShift(UnsignedRightShiftNode n, P p) {
        return visitBinaryNumericalOperation(n, p);
    }

    @Override
    public R visitBitwiseAnd(BitwiseAndNode n, P p) {
        return visitBinaryNumericalOperation(n, p);
    }

    @Override
    public R visitBitwiseOr(BitwiseOrNode n, P p) {
        return visitBinaryNumericalOperation(n, p);
    }

    @Override
    public R visitBitwiseXor(BitwiseXorNode n, P p) {
        return visitBinaryNumericalOperation(n, p);
    }

    // Comparison operations

    /**
     * Visits a binary comparison operation ({@code <}, {@code <=} {@code >}, {@code >=}, {@code
     * ==}, {@code !=}).
     *
     * @param n The binary comparison operation node to visit.
     * @param p The input for the visitor.
     * @return The output produced by the visitor.
     */
    public R visitBinaryComparisonOperation(BinaryOperationNode n, P p) {
        return visitBinaryOperation(n, p);
    }

    @Override
    public R visitLessThan(LessThanNode n, P p) {
        return visitBinaryComparisonOperation(n, p);
    }

    @Override
    public R visitLessThanOrEqual(LessThanOrEqualNode n, P p) {
        return visitBinaryComparisonOperation(n, p);
    }

    @Override
    public R visitGreaterThan(GreaterThanNode n, P p) {
        return visitBinaryComparisonOperation(n, p);
    }

    @Override
    public R visitGreaterThanOrEqual(GreaterThanOrEqualNode n, P p) {
        return visitBinaryComparisonOperation(n, p);
    }

    @Override
    public R visitEqualTo(EqualToNode n, P p) {
        return visitBinaryComparisonOperation(n, p);
    }

    @Override
    public R visitNotEqual(NotEqualNode n, P p) {
        return visitBinaryComparisonOperation(n, p);
    }

    // Conditional operations

    /**
     * Visits a binary conditional operation ({@code &&}, {@code ||}).
     *
     * @param n The binary conditional operation node to visit.
     * @param p The input for the visitor.
     * @return The output produced by the visitor.
     */
    public R visitBinaryConditionalOperation(BinaryOperationNode n, P p) {
        return visitBinaryOperation(n, p);
    }

    @Override
    public R visitConditionalAnd(ConditionalAndNode n, P p) {
        return visitBinaryConditionalOperation(n, p);
    }

    @Override
    public R visitConditionalOr(ConditionalOrNode n, P p) {
        return visitBinaryConditionalOperation(n, p);
    }

    @Override
    public R visitConditionalNot(ConditionalNotNode n, P p) {
        return visitUnaryOperation(n, p);
    }

    @Override
    public R visitTernaryExpression(TernaryExpressionNode n, P p) {
        return visitNode(n, p);
    }

    @Override
    public R visitSwitchExpressionNode(SwitchExpressionNode n, P p) {
        return visitNode(n, p);
    }

    @Override
    public R visitAssignment(AssignmentNode n, P p) {
        return visitNode(n, p);
    }

    @Override
    public R visitLocalVariable(LocalVariableNode n, P p) {
        return visitNode(n, p);
    }

    @Override
    public R visitVariableDeclaration(VariableDeclarationNode n, P p) {
        return visitNode(n, p);
    }

    @Override
    public R visitFieldAccess(FieldAccessNode n, P p) {
        return visitNode(n, p);
    }

    @Override
    public R visitMethodAccess(MethodAccessNode n, P p) {
        return visitNode(n, p);
    }

    @Override
    public R visitArrayAccess(ArrayAccessNode n, P p) {
        return visitNode(n, p);
    }

    /**
     * Visits a explicit or implicit {@code this} reference node.
     *
     * @param n The {@code this} node to visit.
     * @param p The input for the visitor.
     * @return The output produced by the visitor.
     */
    public R visitThis(ThisNode n, P p) {
        return visitNode(n, p);
    }

    @Override
    public R visitImplicitThis(ImplicitThisNode n, P p) {
        return visitThis(n, p);
    }

    @Override
    public R visitExplicitThis(ExplicitThisNode n, P p) {
        return visitThis(n, p);
    }

    @Override
    public R visitSuper(SuperNode n, P p) {
        return visitNode(n, p);
    }

    @Override
    public R visitReturn(ReturnNode n, P p) {
        return visitNode(n, p);
    }

    @Override
    public R visitLambdaResultExpression(LambdaResultExpressionNode n, P p) {
        return visitNode(n, p);
    }

    @Override
    public R visitStringConversion(StringConversionNode n, P p) {
        return visitNode(n, p);
    }

    @Override
    public R visitWideningConversion(WideningConversionNode n, P p) {
        return visitNode(n, p);
    }

    @Override
    public R visitNarrowingConversion(NarrowingConversionNode n, P p) {
        return visitNode(n, p);
    }

    @Override
    public R visitInstanceOf(InstanceOfNode n, P p) {
        return visitNode(n, p);
    }

    @Override
    public R visitTypeCast(TypeCastNode n, P p) {
        return visitNode(n, p);
    }

    // Statements
    @Override
    public R visitAssertionError(AssertionErrorNode n, P p) {
        return visitNode(n, p);
    }

    @Override
    public R visitSynchronized(SynchronizedNode n, P p) {
        return visitNode(n, p);
    }

    @Override
    public R visitThrow(ThrowNode n, P p) {
        return visitNode(n, p);
    }

    // Cases
    @Override
    public R visitCase(CaseNode n, P p) {
        return visitNode(n, p);
    }

    // Method and constructor invocations
    @Override
    public R visitMethodInvocation(MethodInvocationNode n, P p) {
        return visitNode(n, p);
    }

    @Override
    public R visitObjectCreation(ObjectCreationNode n, P p) {
        return visitNode(n, p);
    }

    @Override
    public R visitClassDeclaration(ClassDeclarationNode n, P p) {
        return visitNode(n, p);
    }

    @Override
    public R visitMemberReference(FunctionalInterfaceNode n, P p) {
        return visitNode(n, p);
    }

    @Override
    public R visitArrayCreation(ArrayCreationNode n, P p) {
        return visitNode(n, p);
    }

    // Type, package and class names
    @Override
    public R visitArrayType(ArrayTypeNode n, P p) {
        return visitNode(n, p);
    }

    @Override
    public R visitPrimitiveType(PrimitiveTypeNode n, P p) {
        return visitNode(n, p);
    }

    @Override
    public R visitClassName(ClassNameNode n, P p) {
        return visitNode(n, p);
    }

    @Override
    public R visitPackageName(PackageNameNode n, P p) {
        return visitNode(n, p);
    }

    // Parameterized types
    @Override
    public R visitParameterizedType(ParameterizedTypeNode n, P p) {
        return visitNode(n, p);
    }

    // Marker nodes
    @Override
    public R visitMarker(MarkerNode n, P p) {
        return visitNode(n, p);
    }

    @Override
    public R visitExpressionStatement(ExpressionStatementNode n, P p) {
        return visitNode(n, p);
    }

    @Override
    public R visitDeconstructorPattern(DeconstructorPatternNode n, P p) {
        return visitNode(n, p);
    }

    @Override
    public R visitAnyPattern(AnyPatternNode n, P p) {
        return visitNode(n, p);
    }
}
