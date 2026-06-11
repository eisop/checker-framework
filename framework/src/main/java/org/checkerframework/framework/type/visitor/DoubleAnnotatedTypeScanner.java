package org.checkerframework.framework.type.visitor;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedArrayType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedDeclaredType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedExecutableType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedIntersectionType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedTypeVariable;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedUnionType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedWildcardType;
import org.checkerframework.javacutil.BugInCF;

import java.util.List;

/**
 * An {@link AnnotatedTypeScanner} that scans two {@link AnnotatedTypeMirror}s simultaneously and
 * performs {@link #defaultAction(AnnotatedTypeMirror, AnnotatedTypeMirror)} on the pair. Both
 * AnnotatedTypeMirrors must have the same structure, or a subclass must arrange not to continue
 * recursing past the point at which their structure diverges.
 *
 * <p>If the default action does not return a result, then {@code R} should be {@link Void} and
 * {@code DoubleAnnotatedTypeScanner()} should be used to construct the scanner. If the default
 * action returns a result, then specify a {@link #reduce} function and use {@code
 * DoubleAnnotatedTypeScanner(Reduce, Object)}.
 *
 * @see AnnotatedTypeScanner
 * @param <R> the result of scanning the two {@code AnnotatedTypeMirror}s
 */
public abstract class DoubleAnnotatedTypeScanner<R>
        extends AnnotatedTypeScanner<R, AnnotatedTypeMirror> {

    /**
     * Constructs an AnnotatedTypeScanner where the reduce function returns the first result if it
     * is nonnull; otherwise the second result is returned. The default result is {@code null}.
     */
    protected DoubleAnnotatedTypeScanner() {
        super();
    }

    /**
     * Creates a scanner with the given {@code reduce} function and {@code defaultResult}.
     *
     * @param reduce function used to combine the results of scan
     * @param defaultResult result to use by default
     */
    protected DoubleAnnotatedTypeScanner(Reduce<R> reduce, R defaultResult) {
        super(reduce, defaultResult);
    }

    /**
     * Called by default for any visit method that is not overridden.
     *
     * @param type the type to visit
     * @param p a visitor-specified parameter
     * @return a visitor-specified result
     */
    protected abstract R defaultAction(AnnotatedTypeMirror type, AnnotatedTypeMirror p);

    /**
     * Scans {@code types1} and {@code types2} in parallel and returns the reduced result. Uses
     * index-based access to avoid allocating iterators over the (typically unmodifiable) lists.
     *
     * @param types1 types; may be null
     * @param types2 types; may be null
     * @return the result of scanning and reducing all paired types, or {@link #defaultResult} if
     *     either list is null or both lists are empty
     */
    protected R scan(
            @Nullable List<? extends AnnotatedTypeMirror> types1,
            @Nullable List<? extends AnnotatedTypeMirror> types2) {
        if (types1 == null || types2 == null) {
            return defaultResult;
        }
        int n = Math.min(types1.size(), types2.size());
        if (n == 0) {
            return defaultResult;
        }
        R r = scan(types1.get(0), types2.get(0));
        for (int i = 1; i < n; ++i) {
            r = scanAndReduce(types1.get(i), types2.get(i), r);
        }
        return r;
    }

    /**
     * Scans {@code types1} and {@code types2} in parallel and reduces the result with {@code r}.
     *
     * @param types1 types; may be null
     * @param types2 types; may be null
     * @param r result to combine with the result of scanning the paired types
     * @return the combination of {@code r} with the result of scanning all paired types
     */
    protected R scanAndReduce(
            @Nullable List<? extends AnnotatedTypeMirror> types1,
            @Nullable List<? extends AnnotatedTypeMirror> types2,
            R r) {
        return reduce(scan(types1, types2), r);
    }

    @Override
    protected final R scanAndReduce(
            List<? extends AnnotatedTypeMirror> types, AnnotatedTypeMirror p, R r) {
        throw new BugInCF(
                "DoubleAnnotatedTypeScanner.scanAndReduce: "
                        + p
                        + " is not List<? extends AnnotatedTypeMirror>");
    }

    @Override
    protected R scan(AnnotatedTypeMirror type, AnnotatedTypeMirror p) {
        return reduce(defaultAction(type, p), super.scan(type, p));
    }

    @Override
    public final R visitDeclared(AnnotatedDeclaredType type, AnnotatedTypeMirror p) {
        assert p instanceof AnnotatedDeclaredType : p;
        R r = scan(type.getTypeArguments(), ((AnnotatedDeclaredType) p).getTypeArguments());
        if (type.getEnclosingType() != null) {
            r =
                    scanAndReduce(
                            type.getEnclosingType(),
                            ((AnnotatedDeclaredType) p).getEnclosingType(),
                            r);
        }
        return r;
    }

    @Override
    public final R visitArray(AnnotatedArrayType type, AnnotatedTypeMirror p) {
        assert p instanceof AnnotatedArrayType : p;
        R r = scan(type.getComponentType(), ((AnnotatedArrayType) p).getComponentType());
        return r;
    }

    @Override
    public final R visitExecutable(AnnotatedExecutableType type, AnnotatedTypeMirror p) {
        assert p instanceof AnnotatedExecutableType : p;
        AnnotatedExecutableType ex = (AnnotatedExecutableType) p;
        R r = scan(type.getReturnType(), ex.getReturnType());
        if (type.getReceiverType() != null) {
            r = scanAndReduce(type.getReceiverType(), ex.getReceiverType(), r);
        }
        r = scanAndReduce(type.getParameterTypes(), ex.getParameterTypes(), r);
        r = scanAndReduce(type.getThrownTypes(), ex.getThrownTypes(), r);
        r = scanAndReduce(type.getTypeVariables(), ex.getTypeVariables(), r);
        return r;
    }

    @Override
    public R visitTypeVariable(AnnotatedTypeVariable type, AnnotatedTypeMirror p) {
        if (visitedNodes.containsKey(type)) {
            return visitedNodes.get(type);
        }
        visitedNodes.put(type, null);

        R r;
        if (p instanceof AnnotatedTypeVariable) {
            AnnotatedTypeVariable tv = (AnnotatedTypeVariable) p;
            r = scan(type.getLowerBound(), tv.getLowerBound());
            visitedNodes.put(type, r);
            r = scanAndReduce(type.getUpperBound(), tv.getUpperBound(), r);
            visitedNodes.put(type, r);
        } else {
            r = scan(type.getLowerBound(), p.getErased());
            visitedNodes.put(type, r);
            r = scanAndReduce(type.getUpperBound(), p.getErased(), r);
            visitedNodes.put(type, r);
        }
        return r;
    }

    @Override
    public R visitWildcard(AnnotatedWildcardType type, AnnotatedTypeMirror p) {
        if (visitedNodes.containsKey(type)) {
            return visitedNodes.get(type);
        }
        visitedNodes.put(type, null);

        R r;
        if (p instanceof AnnotatedWildcardType) {
            AnnotatedWildcardType w = (AnnotatedWildcardType) p;
            r = scan(type.getExtendsBound(), w.getExtendsBound());
            visitedNodes.put(type, r);
            r = scanAndReduce(type.getSuperBound(), w.getSuperBound(), r);
            visitedNodes.put(type, r);
        } else {
            r = scan(type.getExtendsBound(), p.getErased());
            visitedNodes.put(type, r);
            r = scanAndReduce(type.getSuperBound(), p.getErased(), r);
            visitedNodes.put(type, r);
        }
        return r;
    }

    @Override
    public R visitIntersection(AnnotatedIntersectionType type, AnnotatedTypeMirror p) {
        assert p instanceof AnnotatedIntersectionType : p;

        if (visitedNodes.containsKey(type)) {
            return visitedNodes.get(type);
        }
        visitedNodes.put(type, null);
        R r = scan(type.getBounds(), ((AnnotatedIntersectionType) p).getBounds());
        return r;
    }

    @Override
    public R visitUnion(AnnotatedUnionType type, AnnotatedTypeMirror p) {
        assert p instanceof AnnotatedUnionType : p;
        if (visitedNodes.containsKey(type)) {
            return visitedNodes.get(type);
        }
        visitedNodes.put(type, null);
        R r = scan(type.getAlternatives(), ((AnnotatedUnionType) p).getAlternatives());
        return r;
    }
}
