# Framework tests (`framework/` subdirectory)

This directory contains framework tests that are valid Java.
Framework tests that are allowed to contain Java errors can be found
in ../framework-javac-errors.

To run the tests, do

```sh
  cd $CHECKERFRAMEWORK/framework
  ../gradlew FrameworkTest
```

To run a single test, do something like:

<!-- markdownlint-disable line-length -->
```sh
  cd $CHECKERFRAMEWORK/framework/tests/framework
  (cd $CHECKERFRAMEWORK && ./gradlew assemble :framework:compileTestJava) && javacheck -processor org.checkerframework.framework.testchecker.util.H1H2Checker -cp $CHECKERFRAMEWORK/framework/build/classes/java/test/
```
<!-- markdownlint-enable line-length -->
