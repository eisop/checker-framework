package org.checkerframework.framework.source;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import org.checkerframework.framework.qual.RelevantJavaTypes;
import org.junit.Assert;
import org.junit.Test;

/** Unit tests for {@link RelevantJavaTypes} inheritance across checker class hierarchies. */
public class RelevantJavaTypesTest {

  /** Default constructor. */
  public RelevantJavaTypesTest() {}

  /** Dummy SourceVisitor for testing. */
  private static class DummySourceVisitor extends SourceVisitor<Void, Void> {
    /**
     * Creates a DummySourceVisitor.
     *
     * @param checker checker
     */
    DummySourceVisitor(SourceChecker checker) {
      super(checker);
    }
  }

  /** Base test checker with explicit relevant Java types. */
  @RelevantJavaTypes({CharSequence.class, int.class})
  public static class BaseRelevantChecker extends SourceChecker {
    /** Default constructor. */
    public BaseRelevantChecker() {}

    @Override
    protected SourceVisitor<?, ?> createSourceVisitor() {
      return new DummySourceVisitor(this);
    }
  }

  /** Subclass checker adding its own relevant Java types. */
  @RelevantJavaTypes({Double.class, Object[].class})
  public static class DerivedRelevantChecker extends BaseRelevantChecker {
    /** Default constructor. */
    public DerivedRelevantChecker() {}
  }

  /** Subclass checker without any annotation. */
  public static class NoAnnotationDerivedChecker extends BaseRelevantChecker {
    /** Default constructor. */
    public NoAnnotationDerivedChecker() {}
  }

  /** Test checker with no annotation and no annotated superclass. */
  public static class PlainUnannotatedChecker extends SourceChecker {
    /** Default constructor. */
    public PlainUnannotatedChecker() {}

    @Override
    protected SourceVisitor<?, ?> createSourceVisitor() {
      return new DummySourceVisitor(this);
    }
  }

  /** Checker that overrides createRelevantJavaTypes programmatically. */
  public static class CustomOverridingChecker extends BaseRelevantChecker {
    /** Default constructor. */
    public CustomOverridingChecker() {}

    @Override
    protected Set<Class<?>> createRelevantJavaTypes() {
      Set<Class<?>> custom = new HashSet<>();
      custom.add(Boolean.class);
      return Collections.unmodifiableSet(custom);
    }
  }

  /**
   * Tests that a subclass inherits and combines types declared on superclasses in the hierarchy.
   */
  @Test
  public void testSuperclassRelevantJavaTypesInheritance() {
    DerivedRelevantChecker checker = new DerivedRelevantChecker();
    Set<Class<?>> types = checker.getRelevantJavaTypes();
    Assert.assertNotNull(types);
    Assert.assertEquals(4, types.size());
    Assert.assertTrue(types.contains(CharSequence.class));
    Assert.assertTrue(types.contains(int.class));
    Assert.assertTrue(types.contains(Double.class));
    Assert.assertTrue(types.contains(Object[].class));
  }

  /** Tests that an unannotated subclass inherits types from an annotated superclass. */
  @Test
  public void testUnannotatedSubclassInheritsSuperclassTypes() {
    NoAnnotationDerivedChecker checker = new NoAnnotationDerivedChecker();
    Set<Class<?>> types = checker.getRelevantJavaTypes();
    Assert.assertNotNull(types);
    Assert.assertEquals(2, types.size());
    Assert.assertTrue(types.contains(CharSequence.class));
    Assert.assertTrue(types.contains(int.class));
  }

  /** Tests that a checker with no annotation returns null (unrestricted). */
  @Test
  public void testUnrestrictedWhenNoAnnotation() {
    PlainUnannotatedChecker checker = new PlainUnannotatedChecker();
    Set<Class<?>> types = checker.getRelevantJavaTypes();
    Assert.assertNull(types);
  }

  /** Tests programmatic override via createRelevantJavaTypes. */
  @Test
  public void testProgrammaticOverride() {
    CustomOverridingChecker checker = new CustomOverridingChecker();
    Set<Class<?>> types = checker.getRelevantJavaTypes();
    Assert.assertNotNull(types);
    Assert.assertEquals(1, types.size());
    Assert.assertTrue(types.contains(Boolean.class));
  }
}
