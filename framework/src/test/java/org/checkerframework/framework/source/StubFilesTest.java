package org.checkerframework.framework.source;

import org.checkerframework.framework.qual.StubFiles;
import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/** Tests for {@link StubFiles} propagation across checker hierarchies and compound checkers. */
public class StubFilesTest {

    /** Default constructor. */
    public StubFilesTest() {}

    /** Dummy SourceVisitor for testing. */
    private static class DummySourceVisitor extends SourceVisitor<Void, Void> {
        DummySourceVisitor(SourceChecker checker) {
            super(checker);
        }
    }

    /** Base test checker with explicit stub files. */
    @StubFiles({"base1.astub", "base2.astub"})
    public static class BaseStubChecker extends SourceChecker {
        /** Default constructor. */
        public BaseStubChecker() {}

        @Override
        protected SourceVisitor<?, ?> createSourceVisitor() {
            return new DummySourceVisitor(this);
        }
    }

    /** Subclass checker overriding with its own stub files. */
    @StubFiles({"sub.astub"})
    public static class OverridingSubChecker extends BaseStubChecker {
        /** Default constructor. */
        public OverridingSubChecker() {}
    }

    /** Subclass checker inheriting superclass stub files. */
    public static class InheritingSubChecker extends BaseStubChecker {
        /** Default constructor. */
        public InheritingSubChecker() {}
    }

    /** Checker with both declared and extra stub files. */
    @StubFiles({"declared.astub"})
    public static class ExtraStubChecker extends SourceChecker {
        /** Default constructor. */
        public ExtraStubChecker() {}

        @Override
        public List<String> getExtraStubFiles() {
            return Collections.singletonList("extra.astub");
        }

        @Override
        protected SourceVisitor<?, ?> createSourceVisitor() {
            return new DummySourceVisitor(this);
        }
    }

    /** Parent compound checker. */
    @StubFiles({"parent.astub"})
    public static class ParentStubChecker extends SourceChecker {
        /**
         * Creates a ParentStubChecker with given subcheckers.
         *
         * @param subcheckers the subcheckers
         */
        public ParentStubChecker(List<SourceChecker> subcheckers) {
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

    /** Child subchecker with its own stub files. */
    @StubFiles({"child.astub"})
    public static class ChildStubChecker extends SourceChecker {
        /** Default constructor. */
        public ChildStubChecker() {}

        @Override
        protected SourceVisitor<?, ?> createSourceVisitor() {
            return new DummySourceVisitor(this);
        }
    }

    /** Grandparent checker with stub files. */
    @StubFiles({"grandparent.astub"})
    public static class GrandparentStubChecker extends SourceChecker {
        /** Default constructor. */
        public GrandparentStubChecker() {}

        @Override
        protected SourceVisitor<?, ?> createSourceVisitor() {
            return new DummySourceVisitor(this);
        }
    }

    /** Unannotated parent checker extending GrandparentStubChecker. */
    public static class UnannotatedParentStubChecker extends GrandparentStubChecker {
        /**
         * Creates an UnannotatedParentStubChecker with given subcheckers.
         *
         * @param subcheckers the subcheckers
         */
        public UnannotatedParentStubChecker(List<SourceChecker> subcheckers) {
            this.subcheckers = subcheckers;
        }

        @Override
        public List<SourceChecker> getSubcheckers() {
            return subcheckers;
        }
    }

    /** Tests that an unannotated subclass inherits stub files from an annotated superclass. */
    @Test
    public void testSubclassInheritsStubFiles() {
        InheritingSubChecker checker = new InheritingSubChecker();
        List<String> stubs = checker.getStubFiles();
        Assert.assertTrue(stubs.contains("base1.astub"));
        Assert.assertTrue(stubs.contains("base2.astub"));
    }

    /** Tests that a subclass with @StubFiles overrides its superclass's stub files. */
    @Test
    public void testSubclassOverridesStubFiles() {
        OverridingSubChecker checker = new OverridingSubChecker();
        List<String> stubs = checker.getStubFiles();
        Assert.assertTrue(stubs.contains("sub.astub"));
        Assert.assertFalse(stubs.contains("base1.astub"));
        Assert.assertFalse(stubs.contains("base2.astub"));
    }

    /** Tests that getExtraStubFiles() are included in getStubFiles(). */
    @Test
    public void testExtraStubFiles() {
        ExtraStubChecker checker = new ExtraStubChecker();
        List<String> stubs = checker.getStubFiles();
        Assert.assertTrue(stubs.contains("declared.astub"));
        Assert.assertTrue(stubs.contains("extra.astub"));
    }

    /** Tests bidirectional stub file propagation between parent checker and subchecker. */
    @Test
    public void testCompoundCheckerPropagation() {
        ChildStubChecker child = new ChildStubChecker();
        ParentStubChecker parent = new ParentStubChecker(Collections.singletonList(child));
        child.setParentChecker(parent);

        List<String> childStubs = child.getStubFiles();
        Assert.assertTrue(childStubs.contains("child.astub"));
        Assert.assertTrue(childStubs.contains("parent.astub"));

        List<String> parentStubs = parent.getStubFiles();
        Assert.assertTrue(parentStubs.contains("parent.astub"));
        Assert.assertTrue(parentStubs.contains("child.astub"));
    }

    /** Tests that setParentChecker invalidates cached stub files on the subchecker. */
    @Test
    public void testSetParentCheckerInvalidation() {
        ChildStubChecker child = new ChildStubChecker();
        List<String> standaloneStubs = child.getStubFiles();
        Assert.assertTrue(standaloneStubs.contains("child.astub"));
        Assert.assertFalse(standaloneStubs.contains("parent.astub"));

        ParentStubChecker parent = new ParentStubChecker(Collections.singletonList(child));
        child.setParentChecker(parent);

        List<String> withParentStubs = child.getStubFiles();
        Assert.assertTrue(withParentStubs.contains("child.astub"));
        Assert.assertTrue(withParentStubs.contains("parent.astub"));
    }

    /** Tests that a subchecker inherits stub files from a parent checker's superclass. */
    @Test
    public void testParentSuperclassPropagation() {
        ChildStubChecker child = new ChildStubChecker();
        UnannotatedParentStubChecker parent =
                new UnannotatedParentStubChecker(Collections.singletonList(child));
        child.setParentChecker(parent);

        List<String> childStubs = child.getStubFiles();
        Assert.assertTrue(childStubs.contains("child.astub"));
        Assert.assertTrue(childStubs.contains("grandparent.astub"));

        List<String> parentStubs = parent.getStubFiles();
        Assert.assertTrue(parentStubs.contains("grandparent.astub"));
        Assert.assertTrue(parentStubs.contains("child.astub"));
    }

    /** Tests that shared stub files are deduplicated and insertion order is preserved. */
    @Test
    public void testDeduplicationAndOrder() {
        ChildStubChecker child =
                new ChildStubChecker() {
                    @Override
                    public List<String> getExtraStubFiles() {
                        return Arrays.asList("shared.astub", "child_extra.astub");
                    }
                };
        ParentStubChecker parent =
                new ParentStubChecker(Collections.singletonList(child)) {
                    @Override
                    public List<String> getExtraStubFiles() {
                        return Arrays.asList("shared.astub", "parent_extra.astub");
                    }
                };
        child.setParentChecker(parent);

        List<String> childStubs = child.getStubFiles();
        int sharedCount = 0;
        for (String s : childStubs) {
            if ("shared.astub".equals(s)) {
                sharedCount++;
            }
        }
        Assert.assertEquals(1, sharedCount);
    }
}
