#!/usr/bin/env python3
"""Generate a single-class Java program with N methods, for perf size sweeps.

Some costs are super-linear in a per-compilation-unit dimension (e.g. the per-body
`Trees.getPath` search in CFG construction was quadratic in methods-per-file) and are
therefore *invisible* on the all-systems corpus, whose files are tiny (1-3 method bodies
each). Sweeping N and plotting allocation/wall vs N exposes the quadratic and separates
the realistic case (small N) from the worst case (large N).

Different costs need different method bodies, so the body shape is selectable with
`--shape` (the body is what decides which machinery you stress):

  generic       (default) a generic call + branch + return: CFG construction, flow
                analysis, getPath, and type-argument inference.
  vararg        calls to JDK vararg methods (Arrays.asList, String.format,
                Class.getMethod): exercises AnnotatedExecutableType deep-copying and the
                vararg-type path. Use this to stress the methodAsMemberOf cache and the
                AnnotatedTypeCopier vararg handling (it is what exposed PR #1798's copier
                aliasing bug). A *user* vararg method does not reproduce it; JDK ones do,
                because their executable types become cached masters.
  deep-nesting  one deeply nested expression per method: stresses tree-path / scan depth.
  many-fields   N fields instead of N methods: stresses field defaulting and fromElement.

Usage:
    gen-sized-program.py 1500 > Big1500.java                 # one size, default shape
    gen-sized-program.py --shape vararg 400 > Varargs.java   # a different shape
    for n in 100 300 600 1500; do                            # a sweep
        gen-sized-program.py $n > Big$n.java
    done

Then A/B each side with the deterministic allocation reader (or ab-measure.sh):
    checker/bin/javac -J-XX:StartFlightRecording=settings=profile,filename=r.jfr,dumponexit=true \\
        -processor nullness -d /tmp/out Big$n.java
    java .claude/skills/cf-performance/alloc-total.java r.jfr
"""

import argparse
import sys


def generic(n: int) -> str:
    lines = ["class Big {", "    static <T> T id(T x) { return x; }"]
    for i in range(n):
        lines += [
            f"    Object m{i}(Object a{i}, Object b{i}) {{",
            f"        Object x{i} = id(a{i});",
            f"        Object y{i} = id(b{i});",
            f"        if (x{i} == y{i}) {{ return x{i}; }}",
            f"        return id(y{i});",
            "    }",
        ]
    lines.append("}")
    return "\n".join(lines) + "\n"


def vararg(n: int) -> str:
    # Calls to JDK vararg methods: their executable types are cached and re-defaulted,
    # and deep-copied per use, so this stresses AnnotatedExecutableType copying and the
    # vararg-type subtree.
    lines = [
        "import java.util.Arrays;",
        "import java.util.List;",
        "class Big {",
    ]
    for i in range(n):
        lines += [
            f"    void m{i}(String s{i}) throws Exception {{",
            f"        List<String> xs{i} = Arrays.asList(s{i}, s{i}, s{i});",
            f"        List<String> ys{i} = Arrays.asList(s{i});",
            f'        String f{i} = String.format("%s %s", s{i}, s{i});',
            f"        java.lang.reflect.Method mm{i} ="
            f' Big.class.getMethod("m{i}", String.class);',
            "    }",
        ]
    lines.append("}")
    return "\n".join(lines) + "\n"


def deep_nesting(n: int) -> str:
    lines = ["class Big {", "    static <T> T id(T x) { return x; }"]
    for i in range(n):
        # A single deeply nested generic-call expression.
        expr = f"a{i}"
        for _ in range(20):
            expr = f"id({expr})"
        lines += [
            f"    Object m{i}(Object a{i}) {{",
            f"        return {expr};",
            "    }",
        ]
    lines.append("}")
    return "\n".join(lines) + "\n"


def many_fields(n: int) -> str:
    lines = ["import java.util.List;", "class Big {"]
    for i in range(n):
        lines.append(f"    List<String> f{i};")
    lines.append("}")
    return "\n".join(lines) + "\n"


SHAPES = {
    "generic": generic,
    "vararg": vararg,
    "deep-nesting": deep_nesting,
    "many-fields": many_fields,
}


if __name__ == "__main__":
    parser = argparse.ArgumentParser(
        description="Generate a sized Java program for perf sweeps."
    )
    parser.add_argument(
        "n", type=int, help="number of methods (or fields, for many-fields)"
    )
    parser.add_argument(
        "--shape",
        choices=sorted(SHAPES),
        default="generic",
        help="method-body shape to stress",
    )
    args = parser.parse_args()
    sys.stdout.write(SHAPES[args.shape](args.n))
