package org.checkerframework.checker.nullness;

import com.sun.source.tree.MethodInvocationTree;

import org.checkerframework.framework.flow.FlowAnalysisRelevance;
import org.checkerframework.javacutil.AnnotationUtils;

import java.util.Set;

import javax.lang.model.element.AnnotationMirror;

/**
 * Decides whether the Key For dataflow analysis of a method can refine any type. Every qualifier
 * other than {@code @UnknownKeyFor} is relevant, and so are the calls of {@code Map.put} and {@code
 * Map.containsKey}, which establish a key.
 */
final class KeyForFlowRelevance extends FlowAnalysisRelevance<KeyForValue> {

    /** The type factory. */
    private final KeyForAnnotatedTypeFactory atypeFactory;

    /**
     * Creates a KeyForFlowRelevance.
     *
     * @param atypeFactory the type factory
     */
    KeyForFlowRelevance(KeyForAnnotatedTypeFactory atypeFactory) {
        super(atypeFactory);
        this.atypeFactory = atypeFactory;
    }

    @Override
    protected boolean isIrrelevantQualifier(AnnotationMirror anno) {
        return AnnotationUtils.areSame(anno, atypeFactory.UNKNOWNKEYFOR);
    }

    @Override
    protected boolean isRelevantValue(KeyForValue value) {
        Set<String> keyForMaps = value.getKeyForMaps();
        return (keyForMaps != null && !keyForMaps.isEmpty()) || super.isRelevantValue(value);
    }

    @Override
    protected boolean isRelevantInvocation(MethodInvocationTree tree) {
        // KeyForTransfer refines the argument of these methods.
        return atypeFactory.isMapPut(tree) || atypeFactory.isMapContainsKey(tree);
    }
}
