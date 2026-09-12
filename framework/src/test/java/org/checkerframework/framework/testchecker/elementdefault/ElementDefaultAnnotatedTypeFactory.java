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
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;

/**
 * Calls {@link QualifierDefaults#addElementDefault} directly on package {@code elementdefault.pkg},
 * defaulting its {@code FIELD} locations to {@link ElementDefaultBottom} -- the same way a written
 * {@code @DefaultQualifier(ElementDefaultBottom.class, TypeUseLocation.FIELD)} on that package's
 * package-info.java would, but through the programmatic API instead. See {@code
 * framework/tests/elementdefault} for what this is testing: that the default set this way still
 * reaches {@code elementdefault.pkg.sub}, a subpackage with no default of its own.
 */
public class ElementDefaultAnnotatedTypeFactory extends BaseAnnotatedTypeFactory {

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

        PackageElement subPkg = elements.getPackageElement("elementdefault.pkg.sub");
        if (subPkg != null) {
            // Query defaults on the subpackage first (populating the packagePropagatingDefaults
            // cache before adding the default to the parent package).
            AnnotatedTypeMirror dummy =
                    AnnotatedTypeMirror.createType(types.getNullType(), this, false);
            defs.annotate(subPkg, dummy);
        }

        PackageElement pkg = elements.getPackageElement("elementdefault.pkg");
        if (pkg != null) {
            AnnotationMirror bottom =
                    AnnotationBuilder.fromClass(elements, ElementDefaultBottom.class);
            defs.addElementDefault(pkg, bottom, TypeUseLocation.FIELD);
        }
    }

    @Override
    public void preProcessClassTree(ClassTree classTree) {
        TypeElement elem = TreeUtils.elementFromDeclaration(classTree);
        if (elem != null) {
            if (elem.getSimpleName().contentEquals("OrderBeforeClass")) {
                AnnotationMirror bottom =
                        AnnotationBuilder.fromClass(elements, ElementDefaultBottom.class);
                defaults.addElementDefault(elem, bottom, TypeUseLocation.PARAMETER);
            } else if (elem.getSimpleName().contentEquals("OrderAfterClass")) {
                // Query defaults on the class and its child members first (populating the
                // elementDefaults memoization cache in QualifierDefaults as well as the
                // elementTypeCache and classAndMethodTreeCache in AnnotatedTypeFactory for both
                // the class and its children prior to calling addElementDefault on the class).
                defaults.annotate(elem, getAnnotatedType(classTree));
                for (Element member : elem.getEnclosedElements()) {
                    defaults.annotate(member, fromElement(member));
                    getAnnotatedType(member);
                }
                // Then call addElementDefault on the element
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
