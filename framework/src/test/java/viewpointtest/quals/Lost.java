package viewpointtest.quals;

import org.checkerframework.framework.qual.SubtypeOf;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * The {@code Lost} qualifier indicates that a relationship cannot be expressed. It results from
 * viewpoint adaptation that combines {@link Top} and {@link ReceiverDependentQual}.
 *
 * <p>It is a valid viewpoint-adaptation result but is invalid as an assignment target, including in
 * compound assignments, increments, and decrements.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
@SubtypeOf({Top.class})
public @interface Lost {}
