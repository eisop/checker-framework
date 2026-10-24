package org.checkerframework.framework.util.typeinference8.types;

import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MemberReferenceTree;
import com.sun.source.tree.MemberReferenceTree.ReferenceMode;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.NewClassTree;
import com.sun.source.tree.Tree;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.code.TypeTag;

import org.checkerframework.framework.type.AnnotatedTypeFactory;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedArrayType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedDeclaredType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedExecutableType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedTypeVariable;
import org.checkerframework.framework.type.visitor.SimpleAnnotatedTypeScanner;
import org.checkerframework.framework.util.AnnotatedTypes;
import org.checkerframework.framework.util.typeinference8.util.Java8InferenceContext;
import org.checkerframework.framework.util.typeinference8.util.Theta;
import org.checkerframework.javacutil.AnnotationMirrorMap;
import org.checkerframework.javacutil.AnnotationMirrorSet;
import org.checkerframework.javacutil.ElementUtils;
import org.checkerframework.javacutil.TreeUtils;
import org.checkerframework.javacutil.TreeUtils.MemberReferenceKind;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.type.ArrayType;
import javax.lang.model.type.ExecutableType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.TypeVariable;

/** A method type for an invocation of a method or constructor. */
public class InvocationType {

    /** A method or constructor invocation. */
    private final ExpressionTree invocation;

    /** The annotated method type. */
    private final AnnotatedExecutableType annotatedExecutableType;

    /** The Java method type. */
    private final ExecutableType methodType;

    /** The context. */
    private final Java8InferenceContext context;

    /** The annotated type factory. */
    private final AnnotatedTypeFactory typeFactory;

    /** A mapping from polymorphic annotation to {@link QualifierVar}. */
    private final AnnotationMirrorMap<QualifierVar> qualifierVars;

    /**
     * Creates an invocation type.
     *
     * @param annotatedExecutableType annotated method type
     * @param methodType java method type
     * @param invocation a method or constructor invocation
     * @param context the context
     */
    public InvocationType(
            AnnotatedExecutableType annotatedExecutableType,
            ExecutableType methodType,
            ExpressionTree invocation,
            Java8InferenceContext context) {
        assert annotatedExecutableType != null && methodType != null;
        this.annotatedExecutableType = annotatedExecutableType;
        this.methodType = methodType;
        this.invocation = invocation;
        this.context = context;
        this.typeFactory = context.typeFactory;

        SimpleAnnotatedTypeScanner<Void, Set<AnnotationMirror>> s =
                new SimpleAnnotatedTypeScanner<>(
                        (type, polys) -> {
                            for (AnnotationMirror a : type.getAnnotations()) {
                                if (typeFactory.getQualifierHierarchy().isPolymorphicQualifier(a)) {
                                    polys.add(a);
                                }
                            }
                            return null;
                        });
        Set<AnnotationMirror> polys = new AnnotationMirrorSet();
        s.visit(annotatedExecutableType, polys);
        AnnotationMirrorMap<QualifierVar> qualifierVars = new AnnotationMirrorMap<>();
        for (AnnotationMirror poly : polys) {
            qualifierVars.put(poly, new QualifierVar(invocation, poly, context));
        }
        this.qualifierVars = qualifierVars;
    }

    /**
     * Returns the method or constructor invocation.
     *
     * @return the method or constructor invocation
     */
    public ExpressionTree getInvocation() {
        return invocation;
    }

    /**
     * Returns the java method type.
     *
     * @return the java method type
     */
    public ExecutableType getJavaType() {
        return annotatedExecutableType.getUnderlyingType();
    }

    /**
     * Returns the thrown types.
     *
     * @param map a mapping from type variable to inference variable
     * @return the thrown types
     */
    public List<? extends AbstractType> getThrownTypes(Theta map) {
        List<AbstractType> thrown = new ArrayList<>();
        Iterator<? extends TypeMirror> iter = methodType.getThrownTypes().iterator();
        for (AnnotatedTypeMirror t : annotatedExecutableType.getThrownTypes()) {
            thrown.add(InferenceType.create(t, iter.next(), map, context));
        }
        return thrown;
    }

    /**
     * Returns the return type.
     *
     * @param map a mapping from type variable to inference variable
     * @return the return type
     */
    public AbstractType getReturnType(Theta map) {
        TypeMirror returnTypeJava;
        AnnotatedTypeMirror returnType;

        if (TreeUtils.isDiamondTree(invocation)) {
            NewClassTree newClassTree = (NewClassTree) invocation;
            // For an anonymous class, `new C<>(args) {...}`, the type arguments of its superclass
            // C are inferred as for `new C<>(args)` (JLS 15.9.3).
            Tree classTypeTree =
                    newClassTree.getClassBody() == null
                            ? newClassTree
                            : newClassTree.getIdentifier();
            Type classTypeJava = (Type) TreeUtils.typeOf(classTypeTree);
            Element e = classTypeJava.asElement();
            returnTypeJava = e.asType();
            returnType = typeFactory.getAnnotatedType(e);
            // The type arguments of the class are inferred, but those of an enclosing type are
            // the type arguments of the type of the enclosing instance.
            AnnotatedDeclaredType declaredEnclosing =
                    ((AnnotatedDeclaredType) returnType).getEnclosingType();
            Type javaEnclosing = classTypeJava.getEnclosingType();
            AnnotatedTypeMirror receiver = typeFactory.getReceiverType(invocation);
            if (declaredEnclosing != null
                    && receiver != null
                    && javaEnclosing.hasTag(TypeTag.CLASS)) {
                Type classType = (Type) returnTypeJava;
                returnTypeJava =
                        new Type.ClassType(
                                javaEnclosing, classType.getTypeArguments(), classType.tsym);
                returnType = returnType.deepCopy();
                ((AnnotatedDeclaredType) returnType)
                        .setEnclosingType(
                                AnnotatedTypes.asSuper(typeFactory, receiver, declaredEnclosing));
            }
        } else if (invocation instanceof MethodInvocationTree
                || invocation instanceof MemberReferenceTree) {
            if (invocation instanceof MemberReferenceTree
                    && ((MemberReferenceTree) invocation).getMode() == ReferenceMode.NEW) {
                returnType =
                        context.typeFactory.getResultingTypeOfConstructorMemberReference(
                                (MemberReferenceTree) invocation, annotatedExecutableType);
                returnTypeJava = returnType.getUnderlyingType();
            } else {
                returnTypeJava = methodType.getReturnType();
                returnType = annotatedExecutableType.getReturnType();
            }

        } else {
            returnTypeJava = TreeUtils.typeOf(invocation);
            returnType = typeFactory.getAnnotatedType(invocation);
        }

        if (map == null) {
            return new ProperType(returnType, returnTypeJava, context);
        }
        return InferenceType.create(returnType, returnTypeJava, map, context);
    }

    /**
     * Returns a list of the parameter types of {@code InvocationType} where the vararg parameter
     * has been modified to match the arguments in {@code expression}.
     *
     * @param map a mapping from type variable to inference variable
     * @param size the number of parameters to return; used to expand the vararg
     * @return a list of the parameter types of {@code InvocationType} where the vararg parameter
     *     has been modified to match the arguments in {@code expression}
     */
    public List<AbstractType> getParameterTypes(Theta map, int size) {
        int skip = numSyntheticParameters();
        List<AnnotatedTypeMirror> params =
                new ArrayList<>(
                        annotatedExecutableType
                                .getParameterTypes()
                                .subList(skip, annotatedExecutableType.getParameterTypes().size()));

        if (TreeUtils.isVarargsCall(invocation)) {
            AnnotatedArrayType vararg = (AnnotatedArrayType) params.remove(params.size() - 1);
            for (int i = params.size(); i < size; i++) {
                params.add(vararg.getComponentType());
            }
        }

        List<TypeMirror> paramsJava =
                new ArrayList<>(
                        methodType
                                .getParameterTypes()
                                .subList(skip, methodType.getParameterTypes().size()));

        if (TreeUtils.isVarargsCall(invocation)) {
            ArrayType vararg = (ArrayType) paramsJava.remove(paramsJava.size() - 1);
            for (int i = paramsJava.size(); i < size; i++) {
                paramsJava.add(vararg.getComponentType());
            }
        }
        if (invocation instanceof MemberReferenceTree
                && MemberReferenceKind.getMemberReferenceKind((MemberReferenceTree) invocation)
                        .isUnbound()) {
            params.add(0, annotatedExecutableType.getReceiverType());
            paramsJava.add(0, annotatedExecutableType.getReceiverType().getUnderlyingType());
        }
        return InferenceType.create(params, paramsJava, map, qualifierVars, context);
    }

    /**
     * Returns the parameter types. (Varags are not expanded.)
     *
     * @param map a mapping from type variable to inference variable
     * @return the parameter types
     */
    public List<AbstractType> getParameterTypes(Theta map) {
        return getParameterTypes(
                map, annotatedExecutableType.getParameterTypes().size() - numSyntheticParameters());
    }

    /**
     * Returns the number of leading parameters of the invoked constructor that do not correspond to
     * an argument of the invocation. The constructor of an anonymous class whose superclass is an
     * inner class takes the enclosing instance as its first parameter if the class instance
     * creation expression is qualified, as in {@code outer.new Inner<>() {}} (JLS 15.9.5.1).
     *
     * @return 1 if the first parameter of the invoked constructor is the enclosing instance of an
     *     anonymous class's superclass; otherwise, 0
     */
    private int numSyntheticParameters() {
        if (invocation instanceof NewClassTree) {
            NewClassTree newClassTree = (NewClassTree) invocation;
            if (newClassTree.getEnclosingExpression() != null
                    && newClassTree.getClassBody() != null
                    && ElementUtils.isAnonymousConstructor(annotatedExecutableType.getElement())) {
                return 1;
            }
        }
        return 0;
    }

    /**
     * Returns true if this method has type variables.
     *
     * @return true if this method has type variables
     */
    public boolean hasTypeVariables() {
        return !annotatedExecutableType.getTypeVariables().isEmpty();
    }

    /**
     * Returns the annotated type variables.
     *
     * @return the annotated type variables
     */
    public List<? extends AnnotatedTypeVariable> getAnnotatedTypeVariables() {
        return annotatedExecutableType.getTypeVariables();
    }

    /**
     * Returns the type variables.
     *
     * @return the type variables
     */
    public List<? extends TypeVariable> getTypeVariables() {
        return methodType.getTypeVariables();
    }

    /**
     * Returns true if this method is void.
     *
     * @return true if this method is void
     */
    public boolean isVoid() {
        return annotatedExecutableType.getReturnType().getKind() == TypeKind.VOID;
    }

    /**
     * Returns the annotated method type.
     *
     * @return the annotated method type
     */
    public AnnotatedExecutableType getAnnotatedType() {
        return annotatedExecutableType;
    }
}
