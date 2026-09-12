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
 */
public class ElementDefaultAnnotatedTypeFactory extends BaseAnnotatedTypeFactory {

    /** Command-line option that makes this factory add an element default too late. */
    public static final String LATE_OPTION = "lateElementDefault";

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

        PackageElement subPkg = elements.getPackageElement("elementdefault.pkg.sub");
        if (subPkg != null) {
            // Query defaults on the subpackage first, so that the propagating-defaults cache for
            // it is already populated when the default is added to its parent package below.
            defs.annotate(subPkg, dummyType());
        }

        PackageElement pkg = elements.getPackageElement("elementdefault.pkg");
        if (pkg != null) {
            defs.addElementDefault(pkg, bottom, TypeUseLocation.FIELD);
        }

        // OrderBeforeClass: nothing queries the class's defaults before the programmatic default
        // is added.
        TypeElement before = elements.getTypeElement("elementdefault.pkg.OrderBeforeClass");
        if (before != null) {
            defs.addElementDefault(before, bottom, TypeUseLocation.PARAMETER);
        }

        // OrderAfterClass: the defaults of the class *and* of one of its members are queried, and
        // therefore memoized, before the programmatic default is added. The member is a nested
        // class with a written @DefaultQualifier of its own, so QualifierDefaults memoizes a
        // DefaultSet for it that is a distinct object from the enclosing class's; invalidating
        // only the enclosing class's entry would leave the member's entry stale.
        // OrderAfterClass must nevertheless produce exactly the same diagnostics as
        // OrderBeforeClass.
        TypeElement after = elements.getTypeElement("elementdefault.pkg.OrderAfterClass");
        if (after != null) {
            defs.annotate(after, dummyType());
            for (Element member : after.getEnclosedElements()) {
                if (member.getKind() == ElementKind.CLASS) {
                    defs.annotate(member, dummyType());
                }
            }
            defs.addElementDefault(after, bottom, TypeUseLocation.PARAMETER);
        }
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
            if (elem != null && elem.getSimpleName().contentEquals("InPkg")) {
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
