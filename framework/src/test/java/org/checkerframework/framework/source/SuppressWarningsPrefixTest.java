package org.checkerframework.framework.source;

import java.util.Collections;
import java.util.List;
import java.util.NavigableSet;
import org.junit.Assert;
import org.junit.Test;

/**
 * Unit tests for {@link SuppressWarningsPrefix} inheritance across checker class hierarchies and
 * propagation from parent checkers to subcheckers.
 */
public class SuppressWarningsPrefixTest {

  /** Default constructor. */
  public SuppressWarningsPrefixTest() {}

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

  /** Base test checker with explicit prefixes. */
  @SuppressWarningsPrefix({"base", "shared"})
  public static class BasePrefixChecker extends SourceChecker {
    /** Default constructor. */
    public BasePrefixChecker() {}

    @Override
    protected SourceVisitor<?, ?> createSourceVisitor() {
      return new DummySourceVisitor(this);
    }
  }

  /** Subclass checker adding its own prefixes. */
  @SuppressWarningsPrefix({"derived", "shared"})
  public static class DerivedPrefixChecker extends BasePrefixChecker {
    /** Default constructor. */
    public DerivedPrefixChecker() {}
  }

  /** Subclass checker without any annotation. */
  public static class NoAnnotationDerivedChecker extends BasePrefixChecker {
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

  /** Parent compound checker. */
  @SuppressWarningsPrefix({"parentprefix"})
  public static final class EnclosingParentChecker extends SourceChecker {

    /**
     * Creates an EnclosingParentChecker with given subcheckers.
     *
     * @param subcheckers the subcheckers
     */
    public EnclosingParentChecker(List<SourceChecker> subcheckers) {
      this.subcheckers = subcheckers;
    }

    @Override
    public List<SourceChecker> getSubcheckers() {
      return subcheckers;
    }

    @Override
    protected SourceVisitor<?, ?> createSourceVisitor() {
      return new DummySourceVisitor(this);
    }
  }

  /** Child subchecker with its own prefix. */
  @SuppressWarningsPrefix({"childprefix"})
  public static class EnclosedChildChecker extends SourceChecker {
    /** Default constructor. */
    public EnclosedChildChecker() {}

    @Override
    protected SourceVisitor<?, ?> createSourceVisitor() {
      return new DummySourceVisitor(this);
    }
  }

  /** Grandparent checker with explicit prefix. */
  @SuppressWarningsPrefix({"grandprefix"})
  public static class GrandparentPrefixChecker extends SourceChecker {
    /** Default constructor. */
    public GrandparentPrefixChecker() {}

    @Override
    protected SourceVisitor<?, ?> createSourceVisitor() {
      return new DummySourceVisitor(this);
    }
  }

  /** Unannotated parent checker extending GrandparentPrefixChecker. */
  public static class UnannotatedParentChecker extends GrandparentPrefixChecker {
    /**
     * Creates an UnannotatedParentChecker with given subcheckers.
     *
     * @param subcheckers the subcheckers
     */
    public UnannotatedParentChecker(List<SourceChecker> subcheckers) {
      this.subcheckers = subcheckers;
    }

    @Override
    public List<SourceChecker> getSubcheckers() {
      return subcheckers;
    }
  }

  /** Parent checker that explicitly overrides its superclass prefix. */
  @SuppressWarningsPrefix({"overriddenparent"})
  public static class OverridingParentChecker extends GrandparentPrefixChecker {
    /**
     * Creates an OverridingParentChecker with given subcheckers.
     *
     * @param subcheckers the subcheckers
     */
    public OverridingParentChecker(List<SourceChecker> subcheckers) {
      this.subcheckers = subcheckers;
    }

    @Override
    public List<SourceChecker> getSubcheckers() {
      return subcheckers;
    }
  }

  /** Tests that a subclass inherits prefixes declared on superclasses in the hierarchy. */
  @Test
  public void testSuperclassPrefixInheritance() {
    DerivedPrefixChecker checker = new DerivedPrefixChecker();
    NavigableSet<String> prefixes = checker.getSuppressWarningsPrefixes();
    Assert.assertTrue(prefixes.contains("base"));
    Assert.assertTrue(prefixes.contains("derived"));
    Assert.assertTrue(prefixes.contains("shared"));
    Assert.assertTrue(prefixes.contains(SourceChecker.SUPPRESS_ALL_PREFIX));
  }

  /** Tests that an unannotated subclass inherits prefixes from an annotated superclass. */
  @Test
  public void testUnannotatedSubclassInheritsSuperclassPrefix() {
    NoAnnotationDerivedChecker checker = new NoAnnotationDerivedChecker();
    NavigableSet<String> prefixes = checker.getSuppressWarningsPrefixes();
    Assert.assertTrue(prefixes.contains("base"));
    Assert.assertTrue(prefixes.contains("shared"));
    Assert.assertTrue(prefixes.contains(SourceChecker.SUPPRESS_ALL_PREFIX));
  }

  /** Tests that a checker with no annotation defaults to its class-derived prefix. */
  @Test
  public void testDefaultPrefixWhenNoAnnotation() {
    PlainUnannotatedChecker checker = new PlainUnannotatedChecker();
    NavigableSet<String> prefixes = checker.getSuppressWarningsPrefixes();
    Assert.assertTrue(prefixes.contains("plainunannotated"));
    Assert.assertTrue(prefixes.contains(SourceChecker.SUPPRESS_ALL_PREFIX));
  }

  /** Tests that a subchecker inherits the prefixes of its parent checker. */
  @Test
  public void testParentCheckerPrefixPropagation() {
    EnclosedChildChecker child = new EnclosedChildChecker();
    EnclosingParentChecker parent = new EnclosingParentChecker(Collections.singletonList(child));
    child.setParentChecker(parent);

    NavigableSet<String> childPrefixes = child.getSuppressWarningsPrefixes();
    Assert.assertTrue(childPrefixes.contains("childprefix"));
    Assert.assertTrue(childPrefixes.contains("parentprefix"));
    Assert.assertTrue(childPrefixes.contains(SourceChecker.SUPPRESS_ALL_PREFIX));

    Assert.assertTrue(parent.getSuppressWarningsPrefixesOfSubcheckers().contains("childprefix"));
    Assert.assertTrue(parent.getSuppressWarningsPrefixesOfSubcheckers().contains("parentprefix"));
  }

  /** Tests that setParentChecker invalidates cached prefixes on the subchecker. */
  @Test
  public void testSetParentCheckerInvalidation() {
    EnclosedChildChecker child = new EnclosedChildChecker();
    NavigableSet<String> standalonePrefixes = child.getSuppressWarningsPrefixes();
    Assert.assertTrue(standalonePrefixes.contains("childprefix"));
    Assert.assertFalse(standalonePrefixes.contains("parentprefix"));

    EnclosingParentChecker parent = new EnclosingParentChecker(Collections.singletonList(child));
    child.setParentChecker(parent);
    NavigableSet<String> withParentPrefixes = child.getSuppressWarningsPrefixes();
    Assert.assertTrue(withParentPrefixes.contains("childprefix"));
    Assert.assertTrue(withParentPrefixes.contains("parentprefix"));
  }

  /** Tests that a subchecker inherits prefixes declared on superclasses of its parent checker. */
  @Test
  public void testParentSuperclassPrefixPropagation() {
    EnclosedChildChecker child = new EnclosedChildChecker();
    UnannotatedParentChecker parent =
        new UnannotatedParentChecker(Collections.singletonList(child));
    child.setParentChecker(parent);

    NavigableSet<String> childPrefixes = child.getSuppressWarningsPrefixes();
    Assert.assertTrue(childPrefixes.contains("childprefix"));
    Assert.assertTrue(childPrefixes.contains("grandprefix"));
    Assert.assertTrue(childPrefixes.contains(SourceChecker.SUPPRESS_ALL_PREFIX));

    Assert.assertTrue(parent.getSuppressWarningsPrefixesOfSubcheckers().contains("childprefix"));
    Assert.assertTrue(parent.getSuppressWarningsPrefixesOfSubcheckers().contains("grandprefix"));
  }

  /**
   * Tests that when a parent checker declares its own @SuppressWarningsPrefix, it overrides
   * prefixes from its superclasses for propagation to subcheckers, avoiding prefix bleed.
   */
  @Test
  public void testParentPrefixOverridingSuperclassForSubcheckers() {
    EnclosedChildChecker child = new EnclosedChildChecker();
    OverridingParentChecker parent = new OverridingParentChecker(Collections.singletonList(child));
    child.setParentChecker(parent);

    // Parent checker itself inherits both its own and its superclasses' prefixes
    NavigableSet<String> parentPrefixes = parent.getSuppressWarningsPrefixes();
    Assert.assertTrue(parentPrefixes.contains("overriddenparent"));
    Assert.assertTrue(parentPrefixes.contains("grandprefix"));

    // Subchecker inherits parent's declared prefix, but not the parent's superclass prefix
    NavigableSet<String> childPrefixes = child.getSuppressWarningsPrefixes();
    Assert.assertTrue(childPrefixes.contains("childprefix"));
    Assert.assertTrue(childPrefixes.contains("overriddenparent"));
    Assert.assertFalse(childPrefixes.contains("grandprefix"));
    Assert.assertTrue(childPrefixes.contains(SourceChecker.SUPPRESS_ALL_PREFIX));
  }
}
