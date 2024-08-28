This directory contains an example Bazel project that downloads the latest
EISOP Checker Framework Maven artifacts and runs the Nullness Checker.

To run the example, simply execute:
`bazel run example`

You should see output that includes:

````
ERROR: docs/examples/BazelExample/BUILD:1:12: Building example.jar (1 source file) and running annotation processors (NullnessChecker) failed: (Exit 1): java failed: error executing Javac command (from target //:example) external/rules_java~~toolchains~remotejdk21_linux/bin/java '--add-exports=jdk.compiler/com.sun.tools.javac.api=ALL-UNNAMED' '--add-exports=jdk.compiler/com.sun.tools.javac.main=ALL-UNNAMED' ... (remaining 19 arguments skipped)
BazelExample.java:25: error: [assignment.type.incompatible] incompatible types in assignment.
        @NonNull Object nn = null; // error on this line
                             ^
  found   : null (NullType)
  required: @NonNull Object
Target //:example failed to build
Use --verbose_failures to see the command lines of failed build steps.
````

## Notes:

I used bazelisk from
https://github.com/bazelbuild/bazelisk/releases/download/v1.20.0/bazelisk-linux-amd64

For getting Maven dependencies to work see
https://github.com/bazelbuild/rules_jvm_external/blob/master/docs/bzlmod.md

````
bazel run @maven//:pin
mv -i rules_jvm_external\~\~maven\~maven_install.json maven_install.json
````
