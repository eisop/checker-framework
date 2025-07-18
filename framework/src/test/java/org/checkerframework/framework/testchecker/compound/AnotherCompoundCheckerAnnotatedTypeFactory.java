package org.checkerframework.framework.testchecker.compound;

import com.sun.source.tree.Tree;

import org.checkerframework.common.aliasing.AliasingChecker;
import org.checkerframework.common.basetype.BaseAnnotatedTypeFactory;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.common.value.ValueChecker;
import org.checkerframework.framework.testchecker.compound.qual.ACCBottom;
import org.checkerframework.framework.testchecker.compound.qual.ACCTop;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.type.GenericAnnotatedTypeFactory;
import org.checkerframework.framework.type.treeannotator.ListTreeAnnotator;
import org.checkerframework.framework.type.treeannotator.TreeAnnotator;

import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class AnotherCompoundCheckerAnnotatedTypeFactory extends BaseAnnotatedTypeFactory {

    /**
     * Creates a new AnotherCompoundCheckerAnnotatedTypeFactory.
     *
     * @param checker the checker
     */
    @SuppressWarnings("this-escape")
    public AnotherCompoundCheckerAnnotatedTypeFactory(BaseTypeChecker checker) {
        super(checker);
        this.postInit();
    }

    @Override
    protected Set<Class<? extends Annotation>> createSupportedTypeQualifiers() {
        return new HashSet<Class<? extends Annotation>>(
                Arrays.asList(ACCTop.class, ACCBottom.class));
    }

    @Override
    protected TreeAnnotator createTreeAnnotator() {
        return new ListTreeAnnotator(
                super.createTreeAnnotator(),
                new TreeAnnotator(this) {
                    @Override
                    protected Void defaultAction(Tree tree, AnnotatedTypeMirror p) {
                        // Just access the subchecker type factories to make
                        // sure they were created properly
                        GenericAnnotatedTypeFactory<?, ?, ?, ?> aliasingATF =
                                getTypeFactoryOfSubchecker(AliasingChecker.class);
                        @SuppressWarnings("unused")
                        AnnotatedTypeMirror aliasing = aliasingATF.getAnnotatedType(tree);
                        GenericAnnotatedTypeFactory<?, ?, ?, ?> valueATF =
                                getTypeFactoryOfSubchecker(ValueChecker.class);
                        @SuppressWarnings("unused")
                        AnnotatedTypeMirror value = valueATF.getAnnotatedType(tree);
                        return super.defaultAction(tree, p);
                    }
                });
    }
}
