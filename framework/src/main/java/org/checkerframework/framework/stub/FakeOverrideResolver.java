package org.checkerframework.framework.stub;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.javacutil.ElementUtils;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;

import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;

/**
 * The shared "fake override" search that both stub-annotation loaders use to bind a stub-declared
 * method that a class only inherits to the method it fake-overrides.
 *
 * <p>A <em>fake override</em> is a method a stub file (or the annotated JDK) declares on a class
 * that does not itself declare that method, only inherits it; the declaration's annotations apply
 * to the inherited method as seen through that subtype (see {@code
 * AnnotationFileParser#processFakeOverride}). To store such a declaration, a loader must find the
 * inherited method it targets. Both loaders search the same way, so the search lives here and each
 * loader supplies only the leaf comparison -- "which method, if any, does <em>this one</em> class
 * declare that this fake override matches" -- via a {@link FakeOverrideMatcher}, because the two
 * loaders represent the fake-override declaration differently (the text parser matches against a
 * parsed {@code .astub} {@code MethodDeclaration}; the binary reader matches against a compact
 * signature string). Sharing the traversal is what keeps the two paths from drifting apart, as they
 * once did: the binary path searched supertypes before the class's own methods and silently dropped
 * 21 fake overrides in the annotated JDK (see commit db83617a4 and issue #1865).
 *
 * <p>The search is two passes over the class hierarchy. The first (exact) pass requires every
 * parameter to match; only if it finds nothing does the second (lenient) pass run, in which a
 * candidate parameter whose type is a type variable matches whatever the fake override spells in
 * that position -- a type-variable parameter has no textual form that reliably matches a stub's
 * spelling, and a type variable renamed between the stub and the JDK being compiled against (e.g.
 * {@code KeySet<K,V>.ceiling(K)} in the stub versus {@code KeySet<E>.ceiling(E)} in JDK 8) means
 * the class does declare the method, under another spelling. Running only the lenient pass would
 * let a stub parameter such as {@code String} bind to a type-variable overload, so the exact pass
 * is tried first. Within each pass, a class's own declared methods are searched before its
 * superclass, and its superclass before its interfaces, as Java resolves overrides; the {@link
 * FakeOverrideMatcher} decides an ambiguous match (two of one class's overloads matching) is no
 * match, again as an override would be reported ambiguous.
 */
final class FakeOverrideResolver {

    /** Do not instantiate. */
    private FakeOverrideResolver() {
        throw new AssertionError("Class FakeOverrideResolver cannot be instantiated.");
    }

    /**
     * Decides, for one class at a time, which method (if any) that class itself declares matches
     * the fake override being resolved. This is the only part of the search that differs between
     * the text and binary loaders; {@link FakeOverrideResolver#findFakeOverridden} supplies the
     * shared traversal over the class hierarchy that repeatedly calls it.
     */
    @FunctionalInterface
    interface FakeOverrideMatcher {
        /**
         * Returns the method that {@code typeElt} itself declares that matches the fake override
         * being resolved, or {@code null} if it declares none (including when the match is
         * ambiguous, i.e. two of {@code typeElt}'s overloads match: annotating whichever javac
         * enumerates first would be arbitrary, so this returns {@code null}). This inspects only
         * {@code typeElt}'s own declared methods, never its supertypes'; the shared traversal walks
         * the hierarchy.
         *
         * @param typeElt the class whose own declared methods to search
         * @param typevarLenient if true, a candidate parameter whose type in {@code typeElt} is a
         *     type variable matches whatever the fake override spells in that position; if false,
         *     every parameter must match exactly
         * @return the method {@code typeElt} declares that matches, or {@code null} if none does or
         *     the match is ambiguous
         */
        @Nullable ExecutableElement matchDeclaredMethod(
                TypeElement typeElt, boolean typevarLenient);
    }

    /**
     * Returns the method that a fake override declared on {@code typeElt} overrides or implements,
     * or {@code null} if none matches. As Java does, this prefers a method in a superclass to one
     * in an interface, and a class's own method to an inherited one.
     *
     * <p>Runs the exact pass first over the whole hierarchy, then the lenient pass; see the class
     * Javadoc for why. The {@code matcher} performs the per-class leaf comparison.
     *
     * @param typeElt the class the fake override is declared on
     * @param matcher the loader-specific per-class leaf comparison
     * @return the method the fake override overrides or implements, or {@code null} if none matches
     */
    static @Nullable ExecutableElement findFakeOverridden(
            TypeElement typeElt, FakeOverrideMatcher matcher) {
        ExecutableElement exact =
                findFakeOverridden(typeElt, matcher, /* typevarLenient= */ false, newVisitedSet());
        if (exact != null) {
            return exact;
        }
        return findFakeOverridden(typeElt, matcher, /* typevarLenient= */ true, newVisitedSet());
    }

    /**
     * Searches {@code typeElt} and its supertypes for a method matching the fake override in the
     * given mode: {@code typeElt}'s own declared methods first, then (recursively) its superclass,
     * then (recursively) its interfaces.
     *
     * @param typeElt the class to search, together with its supertypes
     * @param matcher the loader-specific per-class leaf comparison
     * @param typevarLenient whether a candidate type-variable parameter matches any spelled type;
     *     see {@link FakeOverrideMatcher#matchDeclaredMethod}
     * @param visited interfaces already searched in this pass, so a shared ancestor interface
     *     reachable by more than one path (diamond inheritance) is searched only once
     * @return the matching method, or {@code null} if none matches in this subtree
     */
    private static @Nullable ExecutableElement findFakeOverridden(
            TypeElement typeElt,
            FakeOverrideMatcher matcher,
            boolean typevarLenient,
            Set<TypeElement> visited) {
        ExecutableElement own = matcher.matchDeclaredMethod(typeElt, typevarLenient);
        if (own != null) {
            return own;
        }

        TypeElement superClass = ElementUtils.getSuperClass(typeElt);
        if (superClass != null) {
            ExecutableElement result =
                    findFakeOverridden(superClass, matcher, typevarLenient, visited);
            if (result != null) {
                return result;
            }
        }

        for (TypeMirror interfaceType : typeElt.getInterfaces()) {
            if (interfaceType.getKind() != TypeKind.DECLARED) {
                continue;
            }
            Element interfaceElt = ((DeclaredType) interfaceType).asElement();
            if (!(interfaceElt instanceof TypeElement)
                    || !visited.add((TypeElement) interfaceElt)) {
                continue;
            }
            ExecutableElement result =
                    findFakeOverridden(
                            (TypeElement) interfaceElt, matcher, typevarLenient, visited);
            if (result != null) {
                return result;
            }
        }

        return null;
    }

    /**
     * Returns a fresh, identity-keyed set for tracking already-searched interfaces within one pass.
     *
     * @return a fresh mutable set of {@link TypeElement}, using identity comparison
     */
    private static Set<TypeElement> newVisitedSet() {
        return Collections.newSetFromMap(new IdentityHashMap<>());
    }
}
