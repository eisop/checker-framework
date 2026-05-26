package viewpointtest.quals;

import org.checkerframework.framework.qual.SubtypeOf;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * The Lost qualifier indicates that a relationship cannot be expressed. It is the result of
 * viewpoint adaptation that combines {@link Top} and {@link ReceiverDependentQual}.
 *
 * <p>It is valid as a viewpoint-adaptation result but not as an assignment target, including
 * compound assignments and increments/decrements.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
@SubtypeOf({Top.class})
public @interface Lost {}
