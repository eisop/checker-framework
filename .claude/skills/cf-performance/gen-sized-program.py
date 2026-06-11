#!/usr/bin/env python3
"""Generate a single-class Java program with N methods, for perf size sweeps.

Some costs are super-linear in a per-compilation-unit dimension (e.g. the per-body
`Trees.getPath` search in CFG construction was quadratic in methods-per-file) and are
therefore *invisible* on the all-systems corpus, whose files are tiny (1-3 method bodies
each). Sweeping N and plotting allocation/wall vs N exposes the quadratic and separates
the realistic case (small N) from the worst case (large N).

Usage:
    gen-sized-program.py 1500 > Big1500.java          # one size
    for n in 100 300 600 1500; do                     # a sweep
        gen-sized-program.py $n > Big$n.java
    done

Then A/B each side with the deterministic allocation reader:
    checker/bin/javac -J-XX:StartFlightRecording=settings=profile,filename=r.jfr,dumponexit=true \\
        -processor nullness -d /tmp/out Big$n.java
    java .claude/skills/cf-performance/alloc-total.java r.jfr

Each method has a non-trivial body (locals, a generic call, a branch, a return) so it
triggers CFG construction, flow analysis, and getPath/type-argument inference -- the
machinery that exercises the tree-path and dataflow hot paths. Tune `body` for other
mechanisms (deep expression nesting, many fields, large switch, etc.).
"""
import sys


def generate(n: int) -> str:
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


if __name__ == "__main__":
    if len(sys.argv) != 2:
        sys.exit("usage: gen-sized-program.py <method-count>")
    sys.stdout.write(generate(int(sys.argv[1])))
