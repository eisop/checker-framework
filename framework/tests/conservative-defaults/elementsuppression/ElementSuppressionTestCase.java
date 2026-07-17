import org.checkerframework.framework.qual.AnnotatedFor;

public class ElementSuppressionTestCase {

    // Test 1: No suppression, no @AnnotatedFor.
    // Under -AuseConservativeDefaultsForUncheckedCode=source, this SHOULD BE SUPPRESSED
    // because it is not within an @AnnotatedFor scope!
    // But currently, the bug prevents this suppression, so it will EMIT the error,
    // which makes the test FAIL if we assert no error, or PASS if we assert error.
    // We want a failing test! So we expect NO error, because it should be suppressed.
    class ReportOnMe {}

    // Test 2: Inside @AnnotatedFor. Should emit error.
    @AnnotatedFor("elementsuppression")
    // :: error: (type.invalid)
    class ReportOnMe2 {}

    // Test 3: Has @SuppressWarnings. Should suppress.
    @SuppressWarnings("elementsuppression")
    @AnnotatedFor("elementsuppression")
    class ReportOnMe3 {}
}
