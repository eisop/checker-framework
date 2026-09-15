// A record component type is a recognized location, so it is not reported even under
// -AjspecifyUnrecognizedLocations.  Records require Java 16.
// @below-java16-jdk-skip-test

import org.jspecify.annotations.Nullable;

public record RecognizedRecordComponent(@Nullable String component) {}
