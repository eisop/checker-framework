package viewpointtest.quals;

import org.checkerframework.framework.qual.SubtypeOf;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * The {@link Lost} qualifier indicates that a relationship cannot be expressed. It is the result of
 * viewpoint adaptation that combines {@link Top} and {@link ReceiverDependentQual}.
 *
 * <p>It is not reflexive in the subtyping relationship and the only subtype for {@link Lost} is
 * {@link Bottom}.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
@SubtypeOf({Top.class})
public @interface Lost {}
