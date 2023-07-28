package org.checkerframework.checker.initialization;

import com.sun.source.tree.ClassTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.VariableTree;

import org.checkerframework.checker.initialization.InitializationFieldAccessAnnotatedTypeFactory.CommitmentFieldAccessTreeAnnotator;
import org.checkerframework.checker.initialization.qual.Initialized;
import org.checkerframework.checker.nullness.NullnessChecker;
import org.checkerframework.checker.nullness.NullnessNoInitAnnotatedTypeFactory;
import org.checkerframework.checker.nullness.NullnessNoInitSubchecker;
import org.checkerframework.checker.nullness.qual.EnsuresNonNull;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.framework.qual.InvariantQualifier;

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
 * using this checker is more complex than for others. Specifically, the target checker must:
 *
 * <ol>
 *   <li>Use a subclass of this checker as its parent checker. This is necessary because this
 *       checker is dependent on the target checker to know which fields should be checked for
 *       initialization, and when such a field is initialized: A field is checked for initialization
 *       if its declared type has an {@link InvariantQualifier} (e.g., {@link NonNull}). Such a
 *       field becomes initialized when its refined type has that same invariant qualifier (which
 *       can happen either by assigning the field or by a contract annotation like {@link
 *       EnsuresNonNull}). You can look at the {@link NullnessChecker} for an example: The {@link
 *       NullnessChecker} is a subclass of this checker and uses the {@link
 *       NullnessNoInitSubchecker} as the target checker; thus, the {@link NullnessNoInitSubchecker}
 *       actually checks {@link NonNull} and related qualifiers, while the NullnessChecker checks
 *       {@link Initialized} and related qualifers.
 *   <li>Use the {@link InitializationFieldAccessSubchecker} as a subchecker and add its {@link
 *       CommitmentFieldAccessTreeAnnotator} as a tree annotator. This is necessary to give possibly
 *       uninitialized fields the top type of the target hierarchy (e.g., {@link Nullable}),
 *       ensuring that all fields are initialized before being used. This needs to be a separate
 *       checker because the target checker cannot access any type information from its parent,
 *       which is only initialized after all subcheckers have finished.
 *   <li>Override all necessary methods in the target checker's type factory to take the type
 *       information from the InitializationDeclarationChecker into account. You can look at {@link
 *       NullnessNoInitAnnotatedTypeFactory} for examples.
 *   <li>The subclass should support the command-line option {@code -AassumeInitialized} via
 *       {@code @SupportedOptions({"assumeInitialized"})}, so initialization checking can be turned
 *       off. This gives users of, e.g., the {@link NullnessChecker} an easy way to turn off
 *       initialization checking without having to directly call the {@link
 *       NullnessNoInitSubchecker}.
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
