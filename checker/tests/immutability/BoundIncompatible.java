import org.checkerframework.checker.immutability.qual.Immutable;
import org.checkerframework.checker.immutability.qual.Mutable;
import org.checkerframework.checker.immutability.qual.ReceiverDependantMutable;

public class BoundIncompatible implements java.io.Serializable {}

@Mutable
class A implements java.io.Serializable {}

@ReceiverDependantMutable
class B implements java.io.Serializable {}

@Immutable
class C implements java.io.Serializable {}
