import org.checkerframework.framework.testchecker.h1h2checker.quals.H1Bot;
import org.checkerframework.framework.testchecker.h1h2checker.quals.H1S1;
import org.checkerframework.framework.testchecker.h1h2checker.quals.H1Top;

// @C <: @B <: @A

// Testing Rule 1 (constructor declaration type <: class type)
@H1Top class Issue2163FinalAA {
    @H1Top Issue2163FinalAA() {}
}

@H1Top class Issue2163FinalAB {
    // :: error: (super.invocation.invalid) :: warning: (inconsistent.constructor.type)
    @H1S1 Issue2163FinalAB() {}
}

@H1Top class Issue2163FinalAC {
    // :: error: (super.invocation.invalid) :: warning: (inconsistent.constructor.type)
    @H1Bot Issue2163FinalAC() {}
}

@H1S1 class Issue2163FinalBA {
    // :: error: (type.invalid.annotations.on.use)
    @H1Top Issue2163FinalBA() {}
}

@H1S1 class Issue2163FinalBB {
    // :: error: (super.invocation.invalid) :: warning: (inconsistent.constructor.type)
    @H1S1 Issue2163FinalBB() {}
}

@H1S1 class Issue2163FinalBC {
    // :: error: (super.invocation.invalid) :: warning: (inconsistent.constructor.type)
    @H1Bot Issue2163FinalBC() {}
}

@H1Bot class Issue2163FinalCA {
    // :: error: (type.invalid.annotations.on.use)
    @H1Top Issue2163FinalCA() {}
}

@H1Bot class Issue2163FinalCB {
    // :: error: (type.invalid.annotations.on.use) :: warning: (inconsistent.constructor.type)
    // :: error: (super.invocation.invalid)
    @H1S1 Issue2163FinalCB() {}
}

@H1Bot class Issue2163FinalCC {
    // :: error: (super.invocation.invalid) :: warning: (inconsistent.constructor.type)
    @H1Bot Issue2163FinalCC() {}
}

// Testing Rule 2 (Issue type cast warning if constructor declaration type <: invocation type)
@SuppressWarnings("anno.on.irrelevant")
class Issue2163FinalAAClient {
    void test() {
        new @H1Top Issue2163FinalAA();
        // :: warning: (cast.unsafe.constructor.invocation)
        new @H1S1 Issue2163FinalAA();
        // :: warning: (cast.unsafe.constructor.invocation)
        new @H1Bot Issue2163FinalAA();
    }
}

// Testing Default
@SuppressWarnings("anno.on.irrelevant")
class Issue2163FinalBCClient {
    @H1Bot Issue2163FinalBC obj = new Issue2163FinalBC();
}
