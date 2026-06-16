package edu.cmu.cs.glacier.tests;

import org.checkerframework.checker.pico.qual.Immutable;

public @Immutable class StringTest {
    String s; // no error expected here because String should be treated as if it were declared
    // @Immutable.
}
