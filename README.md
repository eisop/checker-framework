# The EISOP Checker Framework:  pluggable type-checking for Java

Please see the EISOP Checker Framework manual
([HTML](https://eisop.github.io/cf/manual/),
[PDF](https://eisop.github.io/cf/manual/checker-framework-manual.pdf)).

The history of releases and changes is in file
[docs/CHANGELOG.md](docs/CHANGELOG.md).

See below for EISOP Checker Framework development notes.

## Quick Start Guide

1. Clone this repository in some empty directory:
   ```
   git clone git@github.com:eisop/checker-framework.git
   cd checker-framework
   ```

2. Build the EISOP Checker Framework (requires JDK 8+):
   ```
   ./gradlew assemble
   ```
   This will clone the required [eisop/jdk](https://github.com/eisop/jdk) project to a sibling directory called `jdk` and build everything without running the test suite.
   There will be warnings about missing javadoc, but overall the build should be successful.

3. Run a simple test:
   ```
   ./checker/bin/javac -processor nullness docs/examples/NullnessExampleWithWarnings.java
   ```
   This will result in two errors:
   ```
   docs/examples/NullnessExampleWithWarnings.java:24: error: [assignment.type.incompatible] incompatible types in assignment.
        foo = bar;
              ^
     found   : @Nullable String
     required: @NonNull String
   docs/examples/NullnessExampleWithWarnings.java:34: error: [argument.type.incompatible] incompatible argument for parameter arg0 of List.add.
        foo.add(quux);
                ^
     found   : @Nullable String
     required: @NonNull String
   2 errors
   ```

### Development jars without building

If you only want to run the latest development version rather than modify it,
every [CI run](https://github.com/eisop/checker-framework/actions/workflows/ci.yml)
attaches the jars it built to the run, under "Artifacts" on the run's summary
page.  The archive holds `checker.jar`, `checker-qual.jar`, `checker-util.jar`
and `javac.jar` -- the contents of `checker/dist` after a build.  Runs happen on
every pull request as well as on master, so there is an artifact for a proposed
fix too.  With the [GitHub CLI](https://cli.github.com/):

```
gh run download --repo eisop/checker-framework --branch master \
  --name checker-framework-jars
```

GitHub serves these only to signed-in users and deletes them 30 days after the
run, so they are for trying a build out, not for depending on.

There is a lot more to explore!

Please see the EISOP Checker Framework manual
([HTML](https://eisop.github.io/cf/manual/),
[PDF](https://eisop.github.io/cf/manual/checker-framework-manual.pdf))
to learn about all the different type systems, how to integrate them into your build system, and much more.

## Developers

Developer notes are in [docs/developer](docs/developer),
including a [Developer Manual](https://htmlpreview.github.io/?https://github.com/eisop/checker-framework/blob/master/docs/developer/developer-manual.html).

Import the EISOP Checker Framework source folder into your IDE of choice.
See the [IDE configuration](https://htmlpreview.github.io/?https://github.com/eisop/checker-framework/blob/master/docs/developer/developer-manual.html#IDE_configuration) section for notes.

To run all test cases, run:
```
./gradlew alltests
```

There are many AI assistants that might help you navigate the source code, for example
[DeepWiki](https://deepwiki.com/eisop/checker-framework/).

Contributions are always welcome!
For guidelines, see the [Contributing summary](CONTRIBUTING.md) and the
[Contributing](https://eisop.github.io/cf/manual/#contributing)
section in the manual.
