package org.checkerframework.checker.initialization;

import com.sun.source.tree.ClassTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.VariableTree;

import org.checkerframework.checker.initialization.qual.HoldsForDefaultValue;
import org.checkerframework.checker.initialization.qual.Initialized;
import org.checkerframework.checker.nullness.NullnessChecker;
import org.checkerframework.checker.nullness.NullnessNoInitAnnotatedTypeFactory;
import org.checkerframework.checker.nullness.NullnessNoInitSubchecker;
import org.checkerframework.checker.nullness.qual.EnsuresNonNull;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.checker.signature.qual.FullyQualifiedName;
import org.checkerframework.common.basetype.BaseTypeChecker;

import java.util.ArrayList;
import java.util.List;
import java.util.NavigableSet;
import java.util.Set;

/**
 * Tracks whether a value is initialized (all its fields are set), and checks that values are
 * initialized before being used. Implements the freedom-before-commitment scheme for
 * initialization, augmented by type frames.
 *
 * <p>Because there is a cyclic dependency between this type system and the target type system,
 * using this checker is more complex than for others. Specifically:
 *
 * <ol>
 *   <li>Any target type system must provide two checkers: the target checker does all of the
 *       checking of the target type system (e.g., nullness), while the target checker's parent
 *       belongs to a subclass of the {@code InitializationChecker}. You can look at the {@link
 *       NullnessChecker} for an example: For the nullness type system, the {@link
 *       NullnessNoInitSubchecker} is the target checker which actually checks {@link NonNull} and
 *       related qualifiers, while the {@link NullnessChecker} is a subclass of this checker and
 *       thus checks {@link Initialized} and related qualifiers. The parent-child relationship
 *       between the checkers is necessary because this checker is dependent on the target checker
 *       to know which fields should be checked for initialization, and when such a field is
 *       initialized: A field is checked for initialization if its declared type is not the top type
 *       and does not have the meta-annotation {@link HoldsForDefaultValue} (e.g., {@link NonNull}).
 *       Such a field becomes initialized as soon as its refined type agrees with its declared type
 *       (which can happen either by assigning the field or by a contract annotation like {@link
 *       EnsuresNonNull}).
 *   <li>The target checker must add the {@link InitializationFieldAccessSubchecker} as a subchecker
 *       and the {@link InitializationFieldAccessTreeAnnotator} as a tree annotator. This is
 *       necessary to give possibly uninitialized fields the top type of the target hierarchy (e.g.,
 *       {@link Nullable}), ensuring that all fields are initialized before being used. This needs
 *       to be a separate checker because the target checker cannot access any type information from
 *       its parent, which is only initialized after all subcheckers have finished.
 *   <li>The target checker must override all necessary methods in the target checker's type factory
 *       to take the type information from the InitializationFieldAccessSubchecker into account. You
 *       can look at {@link NullnessNoInitAnnotatedTypeFactory} for examples.
 *   <li>Any subclass of the {@code InitializationChecker} should support the command-line option
 *       {@code -AassumeInitialized} via {@code @SupportedOptions({"assumeInitialized"})}, so
 *       initialization checking can be turned off. This gives users of, e.g., the {@link
 *       NullnessChecker} an easy way to turn off initialization checking without having to directly
 *       call the {@link NullnessNoInitSubchecker}.
 * </ol>
 *
 * <p>If you want to modify the freedom-before-commitment scheme in your subclass, note that the
 * InitializationChecker does not use the default convention where, e.g., the annotated type factory
 * for {@code NameChecker} is {@code NameAnnotatedTypeFactory}. Instead every subclass of this
 * checker always uses the {@link InitializationAnnotatedTypeFactory} unless this behavior is
 * overridden. Note also that the flow-sensitive type refinement for this type system is performed
 * by the {@link InitializationFieldAccessSubchecker}; this checker performs no refinement, instead
 * reusing the results from that one.
 *
 * @checker_framework.manual #initialization-checker Initialization Checker
 */
public abstract class InitializationChecker extends BaseTypeChecker {

    /** Default constructor for InitializationChecker. */
    public InitializationChecker() {}

    /**
     * Whether to check primitives for initialization.
     *
     * @return whether to check primitives for initialization
     */
    public abstract boolean checkPrimitives();

    /**
     * The checker for the target type system for which to check initialization.
     *
     * @return the checker for the target type system.
     */
    public abstract Class<? extends BaseTypeChecker> getTargetCheckerClass();

    /**
     * Also handle {@code AnnotatedFor} annotations for this checker. See {@link
     * InitializationFieldAccessSubchecker#getUpstreamCheckerNames()} and the two implementations
     * should be kept in sync.
     */
    @Override
    public List<@FullyQualifiedName String> getUpstreamCheckerNames() {
        if (upstreamCheckerNames == null) {
            super.getUpstreamCheckerNames();
            upstreamCheckerNames.add(InitializationChecker.class.getName());
        }
        return upstreamCheckerNames;
    }

    @Override
    public NavigableSet<String> getSuppressWarningsPrefixes() {
        NavigableSet<String> result = super.getSuppressWarningsPrefixes();
        // "fbc" is for backward compatibility only; you should use
        // "initialization" instead.
        result.add("fbc");
        // The default prefix "initialization" must be added manually because this checker class
        // is abstract and its subclasses are not named "InitializationChecker".
        result.add("initialization");
        return result;
    }

    @Override
    protected Set<Class<? extends BaseTypeChecker>> getImmediateSubcheckerClasses() {
        Set<Class<? extends BaseTypeChecker>> checkers = super.getImmediateSubcheckerClasses();
        checkers.add(getTargetCheckerClass());
        return checkers;
    }

    /**
     * Returns a list of all fields of the given class.
     *
     * @param clazz the class
     * @return a list of all fields of {@code clazz}
     */
    public static List<VariableTree> getAllFields(ClassTree clazz) {
        List<VariableTree> fields = new ArrayList<>();
        for (Tree t : clazz.getMembers()) {
            if (t.getKind() == Tree.Kind.VARIABLE) {
                VariableTree vt = (VariableTree) t;
                fields.add(vt);
            }
        }
        return fields;
    }

    @Override
    public InitializationAnnotatedTypeFactory getTypeFactory() {
        return (InitializationAnnotatedTypeFactory) super.getTypeFactory();
    }

    @Override
    protected InitializationVisitor createSourceVisitor() {
        return new InitializationVisitor(this);
    }

    @Override
    protected boolean messageKeyMatches(
            String messageKey, String messageKeyInSuppressWarningsString) {
        // Also support the shorter keys used by typetools
        return super.messageKeyMatches(messageKey, messageKeyInSuppressWarningsString)
                || super.messageKeyMatches(
                        messageKey.replace(".invalid", ""), messageKeyInSuppressWarningsString);
    }
}
