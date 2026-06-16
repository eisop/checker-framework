package org.checkerframework.checker.nullness;

import org.checkerframework.common.basetype.BaseTypeChecker;

import java.util.NavigableSet;

import javax.annotation.processing.SupportedOptions;

/**
 * A type-checker for determining which values are keys for which maps. Typically used as part of
 * the compound checker for the nullness type system.
 *
 * <p>You can use the following {@link SuppressWarnings} prefixes with this checker:
 *
 * <ul>
 *   <li>{@code @SuppressWarnings("nullness")} suppresses warnings from the Nullness,
 *       Initialization, and KeyFor Checkers
 *   <li>{@code @SuppressWarnings("nullnesskeyfor")} suppresses warnings from the Nullness and
 *       KeyFor Checkers only, warnings from the Initialization Checker are not suppressed
 *       {@code @SuppressWarnings("nullnessnoinit")} has the same effect as
 *       {@code @SuppressWarnings("nullnesskeyfor")}
 *   <li>{@code @SuppressWarnings("keyfor")} suppresses warnings from the KeyFor Checker only,
 *       warnings from the Nullness and Initialization Checkers are not suppressed
 * </ul>
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
        result.add("nullnesskeyfor");
        result.add("nullnessnoinit");
        result.add("nullness");
        return result;
    }
}
