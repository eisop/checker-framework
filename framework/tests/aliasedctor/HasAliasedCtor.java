import org.checkerframework.framework.testchecker.aliasedctor.AliasedCtorBottom;
import org.checkerframework.framework.testchecker.aliasedctor.AliasedCtorLegacyBottom;

import java.util.function.Supplier;

/**
 * The constructor's own declared type is annotated with the alias, not the canonical annotation.
 * AnnotatedTypes#copyOnlyExplicitConstructorAnnotations must still recognize it when computing the
 * type of the constructor reference below -- this is what eisop#2022's alias-resolution work is
 * responsible for.
 */
class HasAliasedCtor {
    @SuppressWarnings({"inconsistent.constructor.type", "super.invocation.invalid"})
    @AliasedCtorLegacyBottom
    HasAliasedCtor() {}
}

class UsesAliasedCtorReference {
    void use() {
        Supplier<@AliasedCtorBottom HasAliasedCtor> s = HasAliasedCtor::new;
    }
}
