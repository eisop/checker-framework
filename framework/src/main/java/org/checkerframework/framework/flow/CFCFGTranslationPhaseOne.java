package org.checkerframework.framework.flow;

import com.sun.source.tree.AssertTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.VariableTree;
import com.sun.source.util.TreePath;

import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.dataflow.cfg.builder.CFGTranslationPhaseOne;
import org.checkerframework.dataflow.cfg.builder.ParameterConditionalThrowSpec;
import org.checkerframework.framework.type.AnnotatedTypeFactory;
import org.checkerframework.javacutil.ElementUtils;
import org.checkerframework.javacutil.TreeUtils;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.ArrayType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;

/**
 * Checker Framework phase-one CFG translation: uses the {@link AnnotatedTypeFactory} during
 * translation (e.g. foreach, artificial trees, assertion-related CFG structure) and merges
 * {@linkplain AnnotatedTypeFactory#getAdditionalParameterConditionalThrowSpecs(ExecutableElement)
 * additional conditional-throw specs} from the type system.
 */
public class CFCFGTranslationPhaseOne extends CFGTranslationPhaseOne {

    /** The associated checker. */
    protected final BaseTypeChecker checker;

    /** Type factory to provide types used during CFG building. */
    protected final AnnotatedTypeFactory atypeFactory;

    /**
     * Creates a Checker Framework phase-one CFG translator.
     *
     * @param builder tree builder (typically a {@link CFTreeBuilder})
     * @param checker the checker
     * @param atypeFactory type factory (also the {@link
     *     org.checkerframework.javacutil.AnnotationProvider})
     * @param assumeAssertionsEnabled whether assertions may be assumed enabled
     * @param assumeAssertionsDisabled whether assertions may be assumed disabled
     * @param env processing environment
     */
    public CFCFGTranslationPhaseOne(
            CFTreeBuilder builder,
            BaseTypeChecker checker,
            AnnotatedTypeFactory atypeFactory,
            boolean assumeAssertionsEnabled,
            boolean assumeAssertionsDisabled,
            ProcessingEnvironment env) {
        super(builder, atypeFactory, assumeAssertionsEnabled, assumeAssertionsDisabled, env);
        this.checker = checker;
        this.atypeFactory = atypeFactory;
    }

    @Override
    protected List<ParameterConditionalThrowSpec> getParameterConditionalThrowSpecs(
            ExecutableElement method) {
        List<ParameterConditionalThrowSpec> result =
                new ArrayList<>(super.getParameterConditionalThrowSpecs(method));
        result.addAll(atypeFactory.getAdditionalParameterConditionalThrowSpecs(method));
        return result;
    }

    @Override
    protected boolean assumeAssertionsEnabledFor(AssertTree tree) {
        if (CFCFGBuilder.assumeAssertionsActivatedForAssertTree(checker, tree)) {
            return true;
        }
        return super.assumeAssertionsEnabledFor(tree);
    }

    /**
     * {@inheritDoc}
     *
     * <p>Assigns a path to the artificial tree.
     *
     * @param tree the newly created Tree
     */
    @Override
    public void handleArtificialTree(Tree tree) {
        TreePath artificialPath = new TreePath(getCurrentPath(), tree);
        atypeFactory.setPathForArtificialTree(tree, artificialPath);
    }

    @Override
    protected VariableTree createEnhancedForLoopIteratorVariable(
            MethodInvocationTree iteratorCall, VariableElement variableElement) {
        Tree annotatedIteratorTypeTree =
                ((CFTreeBuilder) treeBuilder).buildAnnotatedType(TreeUtils.typeOf(iteratorCall));
        handleArtificialTree(annotatedIteratorTypeTree);

        VariableTree iteratorVariable =
                treeBuilder.buildVariableDecl(
                        annotatedIteratorTypeTree,
                        uniqueName("iter"),
                        variableElement.getEnclosingElement(),
                        iteratorCall);
        return iteratorVariable;
    }

    @Override
    protected VariableTree createEnhancedForLoopArrayVariable(
            ExpressionTree expression, VariableElement variableElement) {

        TypeMirror type = null;
        if (TreeUtils.isLocalVariable(expression)) {
            Element elt = TreeUtils.elementFromTree(expression);
            if (elt != null) {
                type = ElementUtils.getType(elt);
            }
        }

        if (type == null || type.getKind() != TypeKind.ARRAY) {
            TypeMirror expressionType = TreeUtils.typeOf(expression);
            type = expressionType;
        }

        assert (type instanceof ArrayType) : "array types must be represented by ArrayType";

        Tree annotatedArrayTypeTree = ((CFTreeBuilder) treeBuilder).buildAnnotatedType(type);
        handleArtificialTree(annotatedArrayTypeTree);

        VariableTree arrayVariable =
                treeBuilder.buildVariableDecl(
                        annotatedArrayTypeTree,
                        uniqueName("array"),
                        variableElement.getEnclosingElement(),
                        expression);
        return arrayVariable;
    }
}
