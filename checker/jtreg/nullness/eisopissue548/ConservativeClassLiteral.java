/*
 * @test
 * @summary Test class literals in CFGs and their type with conservative nullness.
 *
 * @compile MyEnum.java
 * @compile -processor org.checkerframework.checker.nullness.NullnessChecker -AuseConservativeDefaultsForUncheckedCode=bytecode,-source ConservativeClassLiteral.java
 */

import java.util.EnumSet;

class ConservativeClassLiteral {
    EnumSet<MyEnum> none() {
        return EnumSet.noneOf(MyEnum.class);
    }
}
