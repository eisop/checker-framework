package org.checkerframework.framework.type;

import org.checkerframework.dataflow.qual.SideEffectFree;

/**
 * Converts an AnnotatedTypeMirror mirror into a formatted string. For converting AnnotationMirrors:
 *
 * @see org.checkerframework.framework.util.AnnotationFormatter
 */
public interface AnnotatedTypeFormatter {
    /**
     * Formats type into a String. Uses an implementation specific default for printing "invisible
     * annotations"
     *
     * @see org.checkerframework.framework.qual.InvisibleQualifier
     * @param type the type to be converted
     * @return a string representation of type
     */
    @SideEffectFree
    public String format(AnnotatedTypeMirror type);

    /**
     * Formats type into a String. Verbose printing shows details, such as invisible annotations,
     * that an implementation's default might omit; it never hides a detail that {@link
     * #format(AnnotatedTypeMirror)} shows. Therefore, {@code format(type, false)} returns the same
     * string as {@code format(type)}: {@code printVerbose} asks for optional extra detail on top of
     * {@link #format(AnnotatedTypeMirror)}'s output, not for a specific level of detail in its own
     * right, so there is no argument that requests less detail than {@link
     * #format(AnnotatedTypeMirror)} already provides.
     *
     * @param type the type to be converted
     * @param printVerbose whether or not to print verbosely
     * @see org.checkerframework.framework.qual.InvisibleQualifier
     * @return a string representation of type
     */
    @SideEffectFree
    public String format(AnnotatedTypeMirror type, boolean printVerbose);
}
