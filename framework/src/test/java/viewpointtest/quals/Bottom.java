package viewpointtest.quals;

import org.checkerframework.framework.qual.DefaultFor;
import org.checkerframework.framework.qual.SubtypeOf;
import org.checkerframework.framework.qual.TypeUseLocation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** The Bottom qualifier. */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
@SubtypeOf({A.class, B.class, ReceiverDependentQual.class, Top.class, PolyVP.class, Lost.class})
@DefaultFor(TypeUseLocation.LOWER_BOUND)
public @interface Bottom {}
