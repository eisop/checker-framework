import org.checkerframework.checker.tainting.qual.Tainted;
import org.checkerframework.checker.tainting.qual.Untainted;

@Untainted class Issue1015 extends Super1015 implements Interface1015 {}

@Untainted class Super1015 {}

@Tainted interface Interface1015 {}
