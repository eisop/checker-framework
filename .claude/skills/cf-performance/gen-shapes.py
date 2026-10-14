#!/usr/bin/env python3
"""Generate Java programs that each scale ONE structural dimension D, for perf sweeps.

The programs hunt for super-linear (quadratic) costs in the Checker Framework that are invisible
on the tiny all-systems corpus; each scales one per-compilation-unit STRUCTURAL dimension D.

Companion to gen-sized-program.py: that one sweeps the *method count* per file (and fixes the
per-construct shape, e.g. deep-nesting is always 20 deep); this one fixes the count (R reps) and
sweeps a single *structural* dimension D (nesting depth, chain length, case count, ...), which is
what isolates a cost that is super-linear in that dimension. Each file contains R independent reps
of a construct parameterized by D. Sweep D and look at the MARGINAL allocation between consecutive
D (Delta alloc / Delta D, via sweep.sh): constant marginal = linear; doubling-per-D-doubling =
quadratic. Differencing removes the fixed JDK-stub allocation floor and the per-rep fixed cost.

Shapes (which machinery each stresses):
  control      R methods x D independent trivial statements  (linear calibration: always run this)
  cond         R methods x one ternary nested D deep  (#602 conditional non-caching)
  chain        R methods x one .self() chain of length D  (methodFromUse receiver recompute)
  inherit      D-deep class chain + R methods x D up-assignments  (asSuper / directSupertypes walk)
  switchc      R methods x one switch with D cases  (CFG branch fan-in + store merge)
  repeat       R methods x D calls to the SAME method/receiver  (methodAsMemberOf element caching)
  tryfin       R methods x D nested try/finally  (CFG finally duplication)
  loops        R methods x D nested for-loops  (dataflow fixpoint over back-edges)
  fbound       D mutually F-bounded interfaces I1..ID (Ii's bound mentions T1..Ti) + R calls to
               a generic factory method returning ID<?,...,?> (D wildcards) with no explicit
               type witness -- self-bounded/F-bounded generics with mutual dependencies among
               the inference variables, the shape javac.comp.Infer's own worklist and CF's
               typeinference8 (Resolution#resolve/resolveWithCapture, incorporateToFixedPoint)
               both have to solve together. Modeled on Guava's
               MapMakerInternalMap<K,V,E extends InternalEntry<K,V,E>,S extends
               Segment<K,V,E,S>> (which is D=2 with an extra K,V pair) -- ordinary use of that
               class does NOT exercise typeinference8 at all (0 self-time samples over 7800 on
               the real 625-file Guava module); this isolates the specific shape (many MUTUALLY
               bounded inference variables resolved via one wildcarded generic call) that a
               size sweep can check for super-linear cost in D.

Found (June 2026): `cond` is severe super-linear (28 GB at D=160; #602 conditional non-caching);
`inherit` is quadratic (asSuper depth). `repeat`/`tryfin`/`loops` are linear; `chain`/`switchc`
only mildly super-linear. See docs/developer/performance-notes.md (Short list size-sweep audit).

Usage:
    gen-shapes.py <D> --shape cond [--reps R]   > /tmp/cond_<D>.java
    .claude/skills/cf-performance/sweep.sh cond 60 20 40 80 160     # generate + measure the sweep
"""

import argparse
import sys

PREAMBLE = """\
class C {
    C self() { return this; }
    void act() {}
    static <T> T id(T x) { return x; }
}
"""


def control(d: int, r: int) -> str:
    """Return r methods that each contain d independent trivial statements.

    Args:
        d: the number of statements per method
        r: the number of methods

    Returns:
        the Java source text
    """
    out = [PREAMBLE, "class Big {"]
    for k in range(r):
        out.append(f"    void ctrl_{k}() {{")
        out.extend(f"        Object v{j} = new Object();" for j in range(d))
        out.append("    }")
    out.append("}")
    return "\n".join(out) + "\n"


def cond(d: int, r: int) -> str:
    """Return r methods that each contain one ternary expression nested d deep.

    Args:
        d: the nesting depth
        r: the number of methods

    Returns:
        the Java source text
    """
    out = [PREAMBLE, "class Big {"]
    expr = "x"
    for _ in range(d):
        expr = f"(b ? x : {expr})"
    out.extend(f"    Object cond_{k}(boolean b, Object x) {{ return {expr}; }}" for k in range(r))
    out.append("}")
    return "\n".join(out) + "\n"


def chain(d: int, r: int) -> str:
    """Return r methods that each contain one ``.self()`` chain of length d.

    Args:
        d: the length of the chain
        r: the number of methods

    Returns:
        the Java source text
    """
    out = [PREAMBLE, "class Big {"]
    calls = "c" + ".self()" * d
    out.extend(f"    void chain_{k}(C c) {{ {calls}; }}" for k in range(r))
    out.append("}")
    return "\n".join(out) + "\n"


def inherit(d: int, r: int) -> str:
    """Return a class chain of depth d and r methods that each assign up the chain.

    Args:
        d: the depth of the class chain
        r: the number of methods

    Returns:
        the Java source text
    """
    out = ["class K0 {}"]
    out.extend(f"class K{i} extends K{i - 1} {{}}" for i in range(1, d + 1))
    out.append("class Big {")
    for k in range(r):
        out.append(f"    void inh_{k}(K{d} x) {{")
        out.extend(f"        K{i} a{i} = x;" for i in range(d))
        out.append("    }")
    out.append("}")
    return "\n".join(out) + "\n"


def switchc(d: int, r: int) -> str:
    """Return r methods that each contain a switch with d cases.

    Args:
        d: the number of cases
        r: the number of methods

    Returns:
        the Java source text
    """
    out = [PREAMBLE, "class Big {"]
    for k in range(r):
        out.extend(
            [
                f"    int sw_{k}(int s) {{",
                "        int res = 0;",
                "        switch (s) {",
            ]
        )
        out.extend(f"            case {j}: res = {j}; break;" for j in range(d))
        out.extend(
            [
                "            default: break;",
                "        }",
                "        return res;",
                "    }",
            ]
        )
    out.append("}")
    return "\n".join(out) + "\n"


def repeat(d: int, r: int) -> str:
    """Return r methods that each call the same method d times on the same receiver.

    Args:
        d: the number of calls per method
        r: the number of methods

    Returns:
        the Java source text
    """
    out = [PREAMBLE, "class Big {"]
    for k in range(r):
        out.append(f"    void rep_{k}(C c) {{")
        out.extend("        c.act();" for _ in range(d))
        out.append("    }")
    out.append("}")
    return "\n".join(out) + "\n"


def tryfin(d: int, r: int) -> str:
    """Return r methods that each contain d nested try/finally statements.

    Args:
        d: the nesting depth
        r: the number of methods

    Returns:
        the Java source text
    """
    out = [PREAMBLE, "class Big {"]
    for k in range(r):
        out.append(f"    void tf_{k}(C c) {{")
        indent = "        "
        for _ in range(d):
            out.append(indent + "try {")
            indent += "    "
        out.append(indent + "c.act();")
        for _ in range(d):
            indent = indent[:-4]
            out.append(indent + "} finally { c.act(); }")
        out.append("    }")
    out.append("}")
    return "\n".join(out) + "\n"


def loops(d: int, r: int) -> str:
    """Return r methods that each contain d nested for-loops.

    Args:
        d: the nesting depth
        r: the number of methods

    Returns:
        the Java source text
    """
    out = [PREAMBLE, "class Big {"]
    for k in range(r):
        out.append(f"    void lp_{k}(C c) {{")
        indent = "        "
        for i in range(d):
            out.append(indent + f"for (int i{i} = 0; i{i} < 2; i{i}++) {{")
            indent += "    "
        out.append(indent + "c.act();")
        for _ in range(d):
            indent = indent[:-4]
            out.append(indent + "}")
        out.append("    }")
    out.append("}")
    return "\n".join(out) + "\n"


def fbound(d: int, r: int) -> str:
    """Return d mutually F-bounded interfaces and r calls to a wildcarded generic factory.

    Args:
        d: the number of interfaces and type parameters
        r: the number of calls

    Returns:
        the Java source text
    """
    out = []
    for i in range(1, d + 1):
        params = ", ".join(
            f"T{j} extends I{j}<{', '.join('T' + str(k) for k in range(1, j + 1))}>"
            for j in range(1, i + 1)
        )
        out.append(f"interface I{i}<{params}> {{}}")
    targs = ", ".join(
        f"T{j} extends I{j}<{', '.join('T' + str(k) for k in range(1, j + 1))}>"
        for j in range(1, d + 1)
    )
    wargs = ", ".join("?" for _ in range(d))
    out.extend(
        [
            "class Factory {",
            '    @SuppressWarnings("nullness")',
            f"    static <{targs}> I{d}<{wargs}> create() {{ return null; }}",
            "}",
            "class Big {",
        ]
    )
    out.extend(f"    void fb_{k}() {{ var s = Factory.create(); }}" for k in range(r))
    out.append("}")
    return "\n".join(out) + "\n"


SHAPES = {
    "control": control,
    "cond": cond,
    "chain": chain,
    "inherit": inherit,
    "switchc": switchc,
    "repeat": repeat,
    "tryfin": tryfin,
    "loops": loops,
    "fbound": fbound,
}

if __name__ == "__main__":
    p = argparse.ArgumentParser(
        description="Generate a Java program scaling one structural dimension D, for perf sweeps."
    )
    p.add_argument("d", type=int, help="per-construct dimension D (depth/length/count)")
    p.add_argument("--shape", choices=sorted(SHAPES), required=True)
    p.add_argument("--reps", type=int, default=60, help="independent constructs per file")
    a = p.parse_args()
    sys.stdout.write(SHAPES[a.shape](a.d, a.reps))
