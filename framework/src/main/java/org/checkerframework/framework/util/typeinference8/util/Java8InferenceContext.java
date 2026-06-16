package org.checkerframework.framework.util.typeinference8.util;

import com.sun.source.tree.ClassTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.VariableTree;
import com.sun.source.util.TreePath;
import com.sun.tools.javac.code.Types;
import com.sun.tools.javac.processing.JavacProcessingEnvironment;

import org.checkerframework.framework.type.AnnotatedTypeFactory;
import org.checkerframework.framework.util.typeinference8.InvocationTypeInference;
import org.checkerframework.framework.util.typeinference8.types.InferenceFactory;
import org.checkerframework.framework.util.typeinference8.types.ProperType;
import org.checkerframework.javacutil.TreePathUtil;
import org.checkerframework.javacutil.TreeUtils;
import org.checkerframework.javacutil.TypesUtils;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;

/**
 * An object to pass around for use during invocation type inference. One context is created per
 * top-level invocation expression.
 */
public class Java8InferenceContext {

    /** Path to the top level expression whose type arguments are inferred. */
    public TreePath pathToExpression;

    /** javax.annotation.processing.ProcessingEnvironment */
    public final ProcessingEnvironment env;

    /** ProperType for java.lang.Object. */
    public final ProperType object;

    /** Invocation type inference object. */
    public final InvocationTypeInference inference;

    /** com.sun.tools.javac.code.Types */
    public final Types types;

    /** javax.lang.model.util.Types */
    public final javax.lang.model.util.Types modelTypes;

    /**
     * The type of class that encloses the top level expression whose type arguments are inferred.
     */
    public final DeclaredType enclosingType;

    /**
     * Store previously created type variable to inference variable maps as a map from invocation
     * expression to Theta.
     */
    public final IdentityHashMap<ExpressionTree, Theta> maps;

    /** Number of non-capture variables in this inference problem. */
    private int variableCount = 1;

    /** Number of capture variables in this inference problem. */
    private int captureVariableCount = 1;

    /** Number of qualifier variables in this inference problem. */
    private int qualifierVarCount = 1;

    /** TypeMirror for java.lang.RuntimeException. */
    public final TypeMirror runtimeEx;

    /** The inference factory. */
    public final InferenceFactory inferenceTypeFactory;

    /** The annotated type factory. */
    public final AnnotatedTypeFactory typeFactory;

    /** There's no way to tell if an element is a parameter of a lambda, so keep track of them. */
    public final Set<VariableElement> lambdaParms =
            Collections.newSetFromMap(new IdentityHashMap<>());

    /**
     * Maximum amount of bound-incorporation work (sum of bound-list sizes visited by {@link
     * org.checkerframework.framework.util.typeinference8.types.VariableBounds#applyInstantiationsToBounds})
     * permitted for a single inference problem before inference is abandoned.
     *
     * <p>Incorporating bounds to a fixed point (JLS 18.3) is roughly cubic in the nesting depth of
     * a generic invocation, so a single deeply nested (often machine-generated) invocation can take
     * many seconds. This bound caps that work: when it is exceeded an {@link
     * org.checkerframework.framework.util.typeinference8.util.InferenceBudgetExceededError} is
     * thrown and inference falls back to a sound, conservative result. The value has roughly two
     * orders of magnitude of headroom over the largest amount of work observed on hand-written
     * code, so it does not affect realistic programs.
     */
    public static final int MAX_INCORPORATION_WORK = 100_000;

    /** Bound-incorporation work performed so far for this inference problem. */
    private int incorporationWork = 0;

    /**
     * Creates a context
     *
     * @param factory type factory
     * @param pathToExpression path to the expression whose type arguments are inferred
     * @param inference inference object
     */
    @SuppressWarnings("this-escape")
    public Java8InferenceContext(
            AnnotatedTypeFactory factory,
            TreePath pathToExpression,
            InvocationTypeInference inference) {
        this.typeFactory = factory;
        this.pathToExpression = pathToExpression;
        this.env = factory.getProcessingEnv();
        this.inference = inference;
        JavacProcessingEnvironment javacEnv = (JavacProcessingEnvironment) env;
        this.types = Types.instance(javacEnv.getContext());
        this.modelTypes = factory.getProcessingEnv().getTypeUtils();
        ClassTree clazz = TreePathUtil.enclosingClass(pathToExpression);
        this.enclosingType = (DeclaredType) TreeUtils.typeOf(clazz);
        this.maps = new IdentityHashMap<>();
        this.runtimeEx =
                TypesUtils.typeFromClass(
                        RuntimeException.class, env.getTypeUtils(), env.getElementUtils());
        this.inferenceTypeFactory = new InferenceFactory(this);
        this.object = inferenceTypeFactory.getObject();
    }

    /**
     * Records {@code amount} units of bound-incorporation work for this inference problem and
     * throws {@link InferenceBudgetExceededError} if the total exceeds {@link
     * #MAX_INCORPORATION_WORK}.
     *
     * @param amount amount of work to add to this problem's running total
     */
    public void recordIncorporationWork(int amount) {
        incorporationWork += amount;
        if (incorporationWork > MAX_INCORPORATION_WORK) {
            // The error's message (the work counts) is for debugging only; it is not surfaced to
            // the user. DefaultTypeArgumentInference.inferTypeArgs catches this and reports the
            // user-facing type.argument.inference.budget error, which explains the abandonment and
            // suggests explicit type arguments without the (unactionable) work numbers.
            throw new InferenceBudgetExceededError(incorporationWork, MAX_INCORPORATION_WORK);
        }
    }

    /**
     * Returns the next number to use as the id for a non-capture variable. This id is only unique
     * for this inference problem.
     *
     * @return the next number to use as the id for a non-capture variable
     */
    public int getNextVariableId() {
        return variableCount++;
    }

    /**
     * Return the next number to use as the id for a capture variable. This id is only unique for
     * this inference problem.
     *
     * @return the next number to use as the id for a capture variable
     */
    public int getNextCaptureVariableId() {
        return captureVariableCount++;
    }

    /**
     * Returns the next number to use as the id for a qualifier variable. This id is only unique for
     * this inference problem.
     *
     * @return the next number to use as the id for a qualifier variable
     */
    public int getNextQualifierVariableId() {
        return qualifierVarCount++;
    }

    /**
     * Adds the parameters to the list of trees that are lambda parameters.
     *
     * <p>There's no way to tell if a tree is a parameter of a lambda, so keep track of them.
     *
     * @param parameters list of lambda parameters
     */
    public void addLambdaParms(List<? extends VariableTree> parameters) {
        for (VariableTree tree : parameters) {
            lambdaParms.add(TreeUtils.elementFromDeclaration(tree));
        }
    }

    /**
     * Return whether the {@code expression} is a lambda parameter.
     *
     * @param expression an expression
     * @return whether the {@code expression} is a lambda parameter
     */
    public boolean isLambdaParam(ExpressionTree expression) {
        Element element = TreeUtils.elementFromTree(expression);
        if (element == null || element.getKind() != ElementKind.PARAMETER) {
            return false;
        }
        return lambdaParms.contains((VariableElement) element);
    }
}
