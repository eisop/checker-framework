/*
 * @test
 * @summary Test that defaulted types on fields are stored in bytecode, using the java.lang.classfile API.
 *
 * The test cases are shared with the com.sun.tools.classfile harness in ../defaultsPersist:
 * this file only selects the JDK and the driver, and ../defaultsPersist/Fields.java holds the
 * expectations for both.
 *
 * @requires jdk.version.major >= 25
 * @compile ../PersistUtil.java Driver.java ReferenceInfoUtil.java ../defaultsPersist/Fields.java
 * @run main Driver Fields
 */
