package org.checkerframework.framework.testchecker.elementdefault;

import com.sun.source.tree.ClassTree;

import org.checkerframework.common.basetype.BaseAnnotatedTypeFactory;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.framework.qual.TypeUseLocation;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.util.defaults.QualifierDefaults;
import org.checkerframework.javacutil.AnnotationBuilder;
import org.checkerframework.javacutil.TreeUtils;

import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;

/**
 * Calls {@link QualifierDefaults#addElementDefault} directly on package {@code elementdefault.pkg}
 * and on two of its classes, rather than through a written {@code @DefaultQualifier} annotation.
 *
 * <p>All the calls happen while this factory is being initialized, but they are deliberately
 * interleaved with queries that populate {@code QualifierDefaults}' memoization caches, so that the
 * tests in {@code framework/tests/elementdefault} check that a programmatic default does not depend
 * on which defaults happened to be queried before it was added. See eisop#2037 and eisop#2047.
 *
 * <p>Every test that uses this checker must therefore compile all of {@code
 * framework/tests/elementdefault}: the elements this factory annotates are resolved by name, and a
 * name that does not resolve is an error rather than a silently skipped scenario.
 */
public class ElementDefaultAnnotatedTypeFactory extends BaseAnnotatedTypeFactory {

    /** Command-line option that makes this factory add an element default too late. */
    public static final String LATE_OPTION = "lateElementDefault";

    /**
     * Command-line option that makes this factory register a programmatic element default that
     * conflicts with a {@code @DefaultQualifier} written on the same declaration.
     */
    public static final String CONFLICT_OPTION = "conflictingElementDefault";

    /**
     * Creates a new ElementDefaultAnnotatedTypeFactory.
     *
     * @param checker the checker
     */
    @SuppressWarnings("this-escape")
    public ElementDefaultAnnotatedTypeFactory(BaseTypeChecker checker) {
        super(checker);
        this.postInit();
    }

    @Override
    protected void addCheckedCodeDefaults(QualifierDefaults defs) {
        AnnotationMirror top = AnnotationBuilder.fromClass(elements, ElementDefaultTop.class);
        defs.addCheckedCodeDefault(top, TypeUseLocation.OTHERWISE);

        AnnotationMirror bottom = AnnotationBuilder.fromClass(elements, ElementDefaultBottom.class);

        // Query defaults on the subpackage first, so that the propagating-defaults cache for it is
        // already populated when the default is added to its parent package below.
        defs.annotate(requirePackage("elementdefault.pkg.sub"), dummyType());

        defs.addElementDefault(requirePackage("elementdefault.pkg"), bottom, TypeUseLocation.FIELD);

        // OrderBeforeClass: nothing queries the class's defaults before the programmatic default
        // is added.
        TypeElement before = requireType("elementdefault.pkg.OrderBeforeClass");
        defs.addElementDefault(before, bottom, TypeUseLocation.PARAMETER);

        if (checker.hasOption(CONFLICT_OPTION)) {
            // OrderBeforeClass writes @DefaultQualifier(Bottom, RETURN). Registering Top for
            // RETURN on the same class is a conflict: the two qualifiers are in one hierarchy and
            // only one of them can be the default for a location. QualifierDefaults must report
            // that rather than silently picking one; ElementDefaultConflictTest checks that it
            // does.
            defs.addElementDefault(before, top, TypeUseLocation.RETURN);
        }

        // OrderAfterClass: the defaults of the class *and* of one of its members are queried, and
        // therefore memoized, before the programmatic default is added. The member is a nested
        // class with a written @DefaultQualifier of its own, so QualifierDefaults memoizes a
        // DefaultSet for it that is a distinct object from the enclosing class's; invalidating
        // only the enclosing class's entry would leave the member's entry stale.
        // OrderAfterClass must nevertheless produce exactly the same diagnostics as
        // OrderBeforeClass.
        TypeElement after = requireType("elementdefault.pkg.OrderAfterClass");
        defs.annotate(after, dummyType());
        for (Element member : after.getEnclosedElements()) {
            if (member.getKind() == ElementKind.CLASS) {
                defs.annotate(member, dummyType());
            }
        }
        defs.addElementDefault(after, bottom, TypeUseLocation.PARAMETER);
    }

    /**
     * Returns the package with the given canonical name, which the compilation under test must
     * contain.
     *
     * @param name the canonical name of a package of {@code framework/tests/elementdefault}
     * @return the package element for {@code name}
     */
    private PackageElement requirePackage(String name) {
        PackageElement result = elements.getPackageElement(name);
        if (result == null) {
            throw new AssertionError(
                    "ElementDefaultChecker: package "
                            + name
                            + " is not in the compilation, so the scenario it tests would be"
                            + " silently skipped. Compile all of framework/tests/elementdefault.");
        }
        return result;
    }

    /**
     * Returns the type with the given canonical name, which the compilation under test must
     * contain.
     *
     * @param name the canonical name of a class of {@code framework/tests/elementdefault}
     * @return the type element for {@code name}
     */
    private TypeElement requireType(String name) {
        TypeElement result = elements.getTypeElement(name);
        if (result == null) {
            throw new AssertionError(
                    "ElementDefaultChecker: class "
                            + name
                            + " is not in the compilation, so the scenario it tests would be"
                            + " silently skipped. Compile all of framework/tests/elementdefault.");
        }
        return result;
    }

    /**
     * Returns a throwaway type to hand to {@link QualifierDefaults#annotate(Element,
     * AnnotatedTypeMirror)}, whose only purpose here is to populate {@code QualifierDefaults}'
     * memoization caches for the given element.
     *
     * @return a fresh type that the caller discards
     */
    private AnnotatedTypeMirror dummyType() {
        return AnnotatedTypeMirror.createType(types.getNullType(), this, false);
    }

    @Override
    public void preProcessClassTree(ClassTree classTree) {
        if (checker.hasOption(LATE_OPTION)) {
            TypeElement elem = TreeUtils.elementFromDeclaration(classTree);
            // Adding an element default here, rather than during initialization, is a type-system
            // error. ElementDefaultLateTest checks that it is reported as one.
            if (elem.getSimpleName().contentEquals("InPkg")) {
                AnnotationMirror bottom =
                        AnnotationBuilder.fromClass(elements, ElementDefaultBottom.class);
                defaults.addElementDefault(elem, bottom, TypeUseLocation.PARAMETER);
            }
        }
        super.preProcessClassTree(classTree);
    }

    @Override
    protected Set<Class<? extends Annotation>> createSupportedTypeQualifiers() {
        return new HashSet<>(Arrays.asList(ElementDefaultTop.class, ElementDefaultBottom.class));
    }
}
