import org.checkerframework.checker.immutability.qual.Immutable;
import org.checkerframework.checker.immutability.qual.Mutable;
import org.checkerframework.checker.immutability.qual.ReceiverDependantMutable;

@Mutable
public class CompatabilityBEI1 {}

@Immutable
class A {}

@ReceiverDependantMutable
class B {}

@Mutable
class C implements Cloneable {}

@Immutable
class D implements Cloneable {}

@ReceiverDependantMutable
class E implements Cloneable {}

@Mutable
class F extends Object {}

@Immutable
class G extends Object {}

@ReceiverDependantMutable
class H extends Object {}

@Mutable
class I implements @Mutable Cloneable {}

// :: error: (declaration.inconsistent.with.implements.clause)
@Mutable class J implements @Immutable Cloneable {}

@Mutable
class K implements @ReceiverDependantMutable Cloneable {}

// :: error: (declaration.inconsistent.with.extends.clause)
@Immutable class L extends @Mutable Object {}

@Immutable
class M extends @Immutable Object {}

@Immutable
class N extends @ReceiverDependantMutable Object {}

abstract class O implements CharSequence {}

@Mutable
abstract class P implements CharSequence {}

@Immutable
abstract class Q implements CharSequence {}

@ReceiverDependantMutable
abstract class R implements CharSequence {}
