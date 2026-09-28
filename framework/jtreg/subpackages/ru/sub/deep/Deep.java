package ru.sub.deep;

/**
 * Package ru.sub sets applyToSubpackages=false, which limits its own annotation to ru.sub. It does
 * not block package ru, whose annotation applies to subpackages and so still reaches here, so this
 * class is reported.
 */
public class Deep {}
