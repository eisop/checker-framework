import org.checkerframework.checker.pico.qual.Immutable;

class Mut {
    int y = 3;
}

// ::error: (glacier.nonfinalmember)
@Immutable class Immut extends Mut {}

class SafeAbstractSuperclass {
    final int x = 3;
    final String y = "Hello";
    final Immut i = null;
}

@Immutable class Immut2 extends SafeAbstractSuperclass {}
;

class UnsafeAbstractSuperclass {
    final int x = 3;
    String y = "Hello"; // Oops, not final
    final Immut i = null;
}

// ::error: (glacier.nonfinalmember)
@Immutable class Immut3 extends UnsafeAbstractSuperclass {}
;

class UnsafeAbstractSuperclass2 {
    final int x = 3;
    java.util.Date y = null;
    final Immut i = null;
}

// ::error: (glacier.nonfinalmember) ::error: (glacier.mutablemember)
@Immutable class Immut4 extends UnsafeAbstractSuperclass2 {}
;
