package org.checkerframework.checker.nullness;

import org.checkerframework.common.basetype.BaseTypeChecker;

import java.util.NavigableSet;

import javax.annotation.processing.SupportedOptions;

/**
 * A type-checker for determining which values are keys for which maps. Typically used as part of
 * the compound checker for the nullness type system.
 *
 * @checker_framework.manual #map-key-checker Map Key Checker
 * @checker_framework.manual #nullness-checker Nullness Checker
 */
@SupportedOptions({"assumeKeyFor"})
public class KeyForSubchecker extends BaseTypeChecker {
    /** Default constructor for KeyForSubchecker. */
    public KeyForSubchecker() {}

    @Override
    public NavigableSet<String> getSuppressWarningsPrefixes() {
        NavigableSet<String> result = super.getSuppressWarningsPrefixes();
        result.add("nullnessnoinit");
        result.add("nullness");
        return result;
    }
}
