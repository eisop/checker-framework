/*
 * @test
 * @summary Test that inherited declaration annotations are stored in bytecode.
 *
 * @requires jdk.version.major >= 25
 * @compile ../PersistUtil25.java Driver.java ReferenceInfoUtil.java ../inheritDeclAnnoPersist/Implements.java ../inheritDeclAnnoPersist/AbstractClass.java
 * @run main Driver Implements
 *
 */
