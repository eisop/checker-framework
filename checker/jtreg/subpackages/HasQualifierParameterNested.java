/*
 * @test
 * @summary A HasQualifierParameter with applyToSubpackages=false limits only its own annotation.
 * An enclosing package whose annotation applies to subpackages still reaches through it.
 *
 * @compile -processor org.checkerframework.checker.tainting.TaintingChecker -Werror hqp/package-info.java hqp/sub/package-info.java hqp/sub/deep/Deep.java
 */
public class HasQualifierParameterNested {}
