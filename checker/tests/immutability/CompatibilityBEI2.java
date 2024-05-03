import org.checkerframework.checker.immutability.qual.Immutable;
import org.checkerframework.checker.immutability.qual.Mutable;
import org.checkerframework.checker.immutability.qual.ReceiverDependantMutable;

import java.util.ArrayList;
import java.util.List;

@Mutable
abstract class C implements List<@Immutable Object> {}

@Immutable
abstract class D implements List<@Immutable Object> {}

@ReceiverDependantMutable
abstract class E implements List<@Immutable Object> {}

@Mutable
class F extends ArrayList<@Immutable Object> {}

@Immutable
class G extends ArrayList<@Immutable Object> {}

@ReceiverDependantMutable
class H extends ArrayList<@Immutable Object> {}

@Mutable
abstract class I implements @Mutable List<@Immutable Object> {}

// :: error: (declaration.inconsistent.with.implements.clause)
@Mutable
abstract class J implements @Immutable List<@Immutable Object> {}

@Mutable
abstract class K implements @ReceiverDependantMutable List<@Immutable Object> {}

// :: error: (declaration.inconsistent.with.extends.clause)
@Immutable
class L extends @Mutable ArrayList<@Immutable Object> {}

@Immutable
class M extends @Immutable ArrayList<@Immutable Object> {}

@Immutable
class N extends @ReceiverDependantMutable ArrayList<@Immutable Object> {}

abstract class O implements CharSequence {}

@Immutable
interface ImmutableInterface<E extends @ReceiverDependantMutable Object> {}

// :: error: (type.invalid.annotations.on.use)
@Mutable
abstract class P implements ImmutableInterface<@Mutable Object> {}

@Immutable
abstract class Q implements ImmutableInterface<@Immutable Object> {}

// :: error: (type.invalid.annotations.on.use)
@ReceiverDependantMutable
abstract class R implements ImmutableInterface<@ReceiverDependantMutable Object> {}
