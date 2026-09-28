package com.example;

import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Demonstrates the EISOP Checker Framework running as an Error Prone plugin (the {@code eisopcf}
 * check). Compiling this class with the plugin enabled and the Nullness Checker selected produces
 * an {@code eisopcf} diagnostic on the {@code toString()} call below.
 */
public class Demo {

    /** A field that may be null. */
    private @Nullable Object field;

    /**
     * Dereferences a possibly-null field, which the Nullness Checker reports when run via the
     * {@code eisopcf} Error Prone plugin.
     *
     * @return the field's string form
     */
    public String describe() {
        // Error: 'field' may be null here.
        return field.toString();
    }
}
