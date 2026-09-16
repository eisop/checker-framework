package org.checkerframework.dataflow.expression;

import com.sun.source.tree.Tree;
import com.sun.tools.javac.main.JavaCompiler;
import com.sun.tools.javac.main.Option;
import com.sun.tools.javac.processing.JavacProcessingEnvironment;
import com.sun.tools.javac.util.Context;
import com.sun.tools.javac.util.List;
import com.sun.tools.javac.util.Options;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;

/**
 * Tests that {@link BinaryOperation#equals(Object)} and {@link BinaryOperation#hashCode()} stay
 * consistent with each other (see the contract of {@link Object#hashCode()}), in particular for
 * commutative operators, where {@code equals} considers {@code a OP b} and {@code b OP a} equal.
 */
public class BinaryOperationTest {

    /** The {@code int} type, used as both the operand type and the result type below. */
    private TypeMirror intType;

    /** A {@code ValueLiteral} operand with value {@code 1}. */
    private JavaExpression one;

    /** A {@code ValueLiteral} operand with value {@code 2}, distinct from {@link #one}. */
    private JavaExpression two;

    /**
     * Builds a minimal {@link ProcessingEnvironment} (following the same approach as {@code
     * AnnotationBuilderTest}) so that a real {@link TypeMirror} is available for the operands and
     * result type of the {@link BinaryOperation}s under test.
     */
    @Before
    public void setUp() {
        Context context = new Context();
        // Set source and target to 8, as in AnnotationBuilderTest.
        Options options = Options.instance(context);
        options.put(Option.SOURCE, "8");
        options.put(Option.TARGET, "8");

        ProcessingEnvironment env = JavacProcessingEnvironment.instance(context);
        JavaCompiler javac = JavaCompiler.instance(context);
        // Even though source/target are set to 8, the modules in the JavaCompiler
        // need to be initialized by setting the list of modules to nil.
        javac.initModules(List.nil());
        javac.enterDone();

        intType = env.getTypeUtils().getPrimitiveType(TypeKind.INT);
        one = new ValueLiteral(intType, 1);
        two = new ValueLiteral(intType, 2);
    }

    /** Checks that {@code equals} considers {@code 1 + 2} and {@code 2 + 1} equal. */
    @Test
    public void commutativeEqualsIsOrderIndependent() {
        BinaryOperation oneTwo = new BinaryOperation(intType, Tree.Kind.PLUS, one, two);
        BinaryOperation twoOne = new BinaryOperation(intType, Tree.Kind.PLUS, two, one);
        Assert.assertEquals(oneTwo, twoOne);
    }

    /**
     * Checks that {@code hashCode} agrees with {@code equals}: since {@code 1 + 2} and {@code 2 +
     * 1} are equal, they must hash identically. This is the contract violation reported in issue
     * #1653.
     */
    @Test
    public void commutativeHashCodeIsOrderIndependent() {
        BinaryOperation oneTwo = new BinaryOperation(intType, Tree.Kind.PLUS, one, two);
        BinaryOperation twoOne = new BinaryOperation(intType, Tree.Kind.PLUS, two, one);
        Assert.assertEquals(
                "equal commutative BinaryOperations must have equal hash codes",
                oneTwo.hashCode(),
                twoOne.hashCode());
    }

    /** Checks that {@code equals} distinguishes {@code 1 - 2} from {@code 2 - 1}. */
    @Test
    public void nonCommutativeEqualsIsOrderSensitive() {
        BinaryOperation oneTwo = new BinaryOperation(intType, Tree.Kind.MINUS, one, two);
        BinaryOperation twoOne = new BinaryOperation(intType, Tree.Kind.MINUS, two, one);
        Assert.assertNotEquals(oneTwo, twoOne);
    }

    /**
     * Checks that {@code hashCode} remains order-sensitive for a non-commutative operator, i.e.
     * that the fix for commutative operators did not make {@code hashCode} order-independent in
     * general.
     */
    @Test
    public void nonCommutativeHashCodeIsOrderSensitive() {
        BinaryOperation oneTwo = new BinaryOperation(intType, Tree.Kind.MINUS, one, two);
        BinaryOperation twoOne = new BinaryOperation(intType, Tree.Kind.MINUS, two, one);
        Assert.assertNotEquals(oneTwo.hashCode(), twoOne.hashCode());
    }
}
