package org.checkerframework.framework.flow;

import com.sun.source.tree.AssertTree;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.ExpressionTree;

import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.dataflow.cfg.ControlFlowGraph;
import org.checkerframework.dataflow.cfg.UnderlyingAST;
import org.checkerframework.dataflow.cfg.builder.CFGBuilder;
import org.checkerframework.dataflow.cfg.builder.CFGTranslationPhaseThree;
import org.checkerframework.dataflow.cfg.builder.CFGTranslationPhaseTwo;
import org.checkerframework.dataflow.cfg.builder.PhaseOneResult;
import org.checkerframework.framework.type.AnnotatedTypeFactory;
import org.checkerframework.framework.type.GenericAnnotatedTypeFactory;
import org.checkerframework.javacutil.UserError;

import java.util.Collection;

import javax.annotation.processing.ProcessingEnvironment;

/**
 * A control-flow graph builder (see {@link CFGBuilder}) that knows about the Checker Framework
 * annotations and their representation as {@link
 * org.checkerframework.framework.type.AnnotatedTypeMirror}s.
 */
public class CFCFGBuilder extends CFGBuilder {
    /** This class should never be instantiated. Protected to still allow subclasses. */
    protected CFCFGBuilder() {}

    /** Build the control flow graph of some code. */
    public static ControlFlowGraph build(
            CompilationUnitTree root,
            UnderlyingAST underlyingAST,
            BaseTypeChecker checker,
            AnnotatedTypeFactory atypeFactory,
            ProcessingEnvironment env) {
        boolean assumeAssertionsEnabled = checker.hasOption("assumeAssertionsAreEnabled");
        boolean assumeAssertionsDisabled = checker.hasOption("assumeAssertionsAreDisabled");
        if (assumeAssertionsEnabled && assumeAssertionsDisabled) {
            throw new UserError(
                    "Assertions cannot be assumed to be enabled and disabled at the same time.");
        }

        // Subcheckers with dataflow share control-flow graph structure to
        // allow a super-checker to query the stores of a subchecker.
        if (atypeFactory instanceof GenericAnnotatedTypeFactory) {
            GenericAnnotatedTypeFactory<?, ?, ?, ?> asGATF =
                    (GenericAnnotatedTypeFactory<?, ?, ?, ?>) atypeFactory;
            if (asGATF.hasOrIsSubchecker) {
                ControlFlowGraph sharedCFG = asGATF.getSharedCFGForTree(underlyingAST.getCode());
                if (sharedCFG != null) {
                    return sharedCFG;
                }
            }
        }

        CFTreeBuilder builder = new CFTreeBuilder(env);
        PhaseOneResult phase1result =
                new CFCFGTranslationPhaseOne(
                                builder,
                                checker,
                                atypeFactory,
                                assumeAssertionsEnabled,
                                assumeAssertionsDisabled,
                                env)
                        .process(root, underlyingAST);
        ControlFlowGraph phase2result = CFGTranslationPhaseTwo.process(phase1result);
        ControlFlowGraph phase3result = CFGTranslationPhaseThree.process(phase2result);
        if (atypeFactory instanceof GenericAnnotatedTypeFactory) {
            GenericAnnotatedTypeFactory<?, ?, ?, ?> asGATF =
                    (GenericAnnotatedTypeFactory<?, ?, ?, ?>) atypeFactory;
            if (asGATF.hasOrIsSubchecker) {
                asGATF.addSharedCFGForTree(underlyingAST.getCode(), phase3result);
            }
        }
        return phase3result;
    }

    /**
     * Given a SourceChecker and an AssertTree, returns whether the AssertTree uses an
     * {@code @AssumeAssertion} string that is relevant to the SourceChecker.
     *
     * @param checker the checker
     * @param tree an assert tree
     * @return true if the assert tree contains an {@code @AssumeAssertion(checker)} message string
     *     for any subchecker of the given checker's ultimate parent checker
     */
    public static boolean assumeAssertionsActivatedForAssertTree(
            BaseTypeChecker checker, AssertTree tree) {
        ExpressionTree detail = tree.getDetail();
        if (detail != null) {
            String msg = detail.toString();
            BaseTypeChecker ultimateParent = checker.getUltimateParentChecker();
            Collection<String> prefixes = ultimateParent.getSuppressWarningsPrefixesOfSubcheckers();
            for (String prefix : prefixes) {
                String assumeAssert = "@AssumeAssertion(" + prefix + ")";
                if (msg.contains(assumeAssert)) {
                    return true;
                }
            }
        }

        return false;
    }
}
