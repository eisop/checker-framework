/*
 * @test
 * @summary Test that inherited declaration annotations are stored in bytecode.
 *
 * @requires jdk.version.major >= 25
 * @compile ../PersistUtil25.java Driver.java ReferenceInfoUtil.java ../inheritDeclAnnoPersist/Extends.java ../inheritDeclAnnoPersist/Super.java
 * @run main Driver Extends
 *
 */
