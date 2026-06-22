#!/usr/bin/env python3
"""Generate Java programs that each scale ONE per-compilation-unit STRUCTURAL dimension D,
to hunt for super-linear (quadratic) costs in the Checker Framework that are invisible on the
tiny all-systems corpus.

Companion to gen-sized-program.py: that one sweeps the *method count* per file (and fixes the
per-construct shape, e.g. deep-nesting is always 20 deep); this one fixes the count (R reps) and
sweeps a single *structural* dimension D (nesting depth, chain length, case count, ...), which is
what isolates a cost that is super-linear in that dimension. Each file contains R independent reps
of a construct parameterized by D. Sweep D and look at the MARGINAL allocation between consecutive
D (Delta alloc / Delta D, via sweep.sh): constant marginal = linear; doubling-per-D-doubling =
quadratic. Differencing removes the fixed JDK-stub allocation floor and the per-rep fixed cost.

Shapes (which machinery each stresses):
  control      R methods x D independent trivial statements        (linear calibration -- always run this)
  cond         R methods x one ternary nested D deep                (#602 conditional non-caching)
  chain        R methods x one .self() chain of length D            (methodFromUse receiver recompute)
  inherit      D-deep class chain + R methods x D up-assignments    (asSuper / directSupertypes walk)
  switchc      R methods x one switch with D cases                  (CFG branch fan-in + store merge)
  repeat       R methods x D calls to the SAME method/receiver      (methodAsMemberOf element caching)
  tryfin       R methods x D nested try/finally                     (CFG finally duplication)
  loops        R methods x D nested for-loops                       (dataflow fixpoint over back-edges)

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


def control(d, r):
    out = [PREAMBLE, "class Big {"]
    for k in range(r):
        out.append(f"    void ctrl_{k}() {{")
        for j in range(d):
            out.append(f"        Object v{j} = new Object();")
        out.append("    }")
    out.append("}")
    return "\n".join(out) + "\n"


def cond(d, r):
    out = [PREAMBLE, "class Big {"]
    expr = "x"
    for _ in range(d):
        expr = f"(b ? x : {expr})"
    for k in range(r):
        out.append(f"    Object cond_{k}(boolean b, Object x) {{ return {expr}; }}")
    out.append("}")
    return "\n".join(out) + "\n"


def chain(d, r):
    out = [PREAMBLE, "class Big {"]
    calls = "c" + ".self()" * d
    for k in range(r):
        out.append(f"    void chain_{k}(C c) {{ {calls}; }}")
    out.append("}")
    return "\n".join(out) + "\n"


def inherit(d, r):
    out = ["class K0 {}"]
    for i in range(1, d + 1):
        out.append(f"class K{i} extends K{i-1} {{}}")
    out.append("class Big {")
    for k in range(r):
        out.append(f"    void inh_{k}(K{d} x) {{")
        for i in range(d):
            out.append(f"        K{i} a{i} = x;")
        out.append("    }")
    out.append("}")
    return "\n".join(out) + "\n"


def switchc(d, r):
    out = [PREAMBLE, "class Big {"]
    for k in range(r):
        out.append(f"    int sw_{k}(int s) {{")
        out.append("        int res = 0;")
        out.append("        switch (s) {")
        for j in range(d):
            out.append(f"            case {j}: res = {j}; break;")
        out.append("            default: break;")
        out.append("        }")
        out.append("        return res;")
        out.append("    }")
    out.append("}")
    return "\n".join(out) + "\n"


def repeat(d, r):
    out = [PREAMBLE, "class Big {"]
    for k in range(r):
        out.append(f"    void rep_{k}(C c) {{")
        for _ in range(d):
            out.append("        c.act();")
        out.append("    }")
    out.append("}")
    return "\n".join(out) + "\n"


def tryfin(d, r):
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


def loops(d, r):
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


SHAPES = {
    "control": control,
    "cond": cond,
    "chain": chain,
    "inherit": inherit,
    "switchc": switchc,
    "repeat": repeat,
    "tryfin": tryfin,
    "loops": loops,
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
