# Bazel example

This directory contains an example Bazel project that downloads the latest
EISOP Checker Framework Maven artifacts and runs the Nullness Checker.

To run the example, simply execute:
`bazel run example`

You should see output that includes:

````text
ERROR: docs/examples/BazelExample/BUILD:1:12: Building example.jar (1 source file) and running annotation processors (NullnessChecker) failed: (Exit 1): java failed: error executing Javac command (from target //:example) external/rules_java~~toolchains~remotejdk21_linux/bin/java '--add-exports=jdk.compiler/com.sun.tools.javac.api=ALL-UNNAMED' '--add-exports=jdk.compiler/com.sun.tools.javac.main=ALL-UNNAMED' ... (remaining 19 arguments skipped)
BazelExample.java:25: error: [assignment.type.incompatible] incompatible types in assignment.
        @NonNull Object nn = null; // error on this line
                             ^
  found   : @Nullable NullType
  required: @NonNull Object
Target //:example failed to build
Use --verbose_failures to see the command lines of failed build steps.
````

## Notes

I used bazelisk from
<https://github.com/bazelbuild/bazelisk/releases/download/v1.20.0/bazelisk-linux-amd64>

The Maven artifacts are not pinned with a `maven_install.json` lock file, so that Renovate can
update the Checker Framework version in `MODULE.bazel` once a release is on Maven Central.  See
<https://github.com/bazelbuild/rules_jvm_external/blob/master/docs/bzlmod.md>.
