package org.checkerframework.framework.source;

import org.checkerframework.checker.compilermsgs.qual.CompilerMessageKey;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.dataflow.qual.Pure;
import org.checkerframework.dataflow.qual.SideEffectFree;
import org.checkerframework.framework.qual.AnnotatedFor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.tools.Diagnostic;

/**
 * A {@code DiagMessage} is a kind, a message key, and arguments. The message key will be expanded
 * according to the user locale. Any arguments will then be interpolated into the localized message.
 *
 * <p>By contrast, {@code javax.tools.Diagnostic} has just a string message.
 */
@AnnotatedFor("nullness")
public class DiagMessage {
    /** The kind of message. */
    private final Diagnostic.Kind kind;

    /** The message key. */
    private final @CompilerMessageKey String messageKey;

    /** The arguments that will be interpolated into the localized message. */
    private final Object[] args;

    /**
     * Cached hash code. Lazily computed on first call to {@link #hashCode()} and gated by {@link
     * #hashCodeComputed}.
     */
    private transient int hashCodeCache = 0;

    /** True if {@link #hashCodeCache} contains the current hash code. */
    private transient boolean hashCodeComputed = false;

    /** Shared empty args array, returned for no-arg DiagMessages instead of allocating. */
    private static final Object[] EMPTY_ARGS = new Object[0];

    /**
     * Create a DiagMessage.
     *
     * @param kind the kind of message
     * @param messageKey the message key
     * @param args the arguments that will be interpolated into the localized message
     */
    public DiagMessage(
            Diagnostic.Kind kind, @CompilerMessageKey String messageKey, Object... args) {
        this.kind = kind;
        this.messageKey = messageKey;
        if (args == null || args.length == 0) {
            // Share a single empty array instead of allocating one per no-arg DiagMessage.
            this.args = EMPTY_ARGS; /*null->nn*/
        } else {
            this.args = Arrays.copyOf(args, args.length);
        }
    }

    /**
     * Create a DiagMessage with kind ERROR.
     *
     * @param messageKey the message key
     * @param args the arguments that will be interpolated into the localized message
     * @return a new DiagMessage
     */
    public static DiagMessage error(@CompilerMessageKey String messageKey, Object... args) {
        return new DiagMessage(Diagnostic.Kind.ERROR, messageKey, args);
    }

    /**
     * Returns the kind of this DiagMessage.
     *
     * @return the kind of this DiagMessage
     */
    public Diagnostic.Kind getKind() {
        return this.kind;
    }

    /**
     * Returns the message key of this DiagMessage.
     *
     * @return the message key of this DiagMessage
     */
    public @CompilerMessageKey String getMessageKey() {
        return this.messageKey;
    }

    /**
     * Returns the customized optional arguments for the message.
     *
     * @return the customized optional arguments for the message
     */
    public Object[] getArgs() {
        return this.args;
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof DiagMessage)) {
            return false;
        }

        DiagMessage other = (DiagMessage) obj;

        return (kind == other.kind
                && messageKey.equals(other.messageKey)
                && Arrays.equals(args, other.args));
    }

    @Pure
    @Override
    public int hashCode() {
        if (!hashCodeComputed) {
            int h = (kind == null ? 0 : kind.hashCode());
            h = 31 * h + (messageKey == null ? 0 : messageKey.hashCode());
            h = 31 * h + Arrays.hashCode(args);
            hashCodeCache = h;
            hashCodeComputed = true;
        }
        return hashCodeCache;
    }

    @SideEffectFree
    @Override
    public String toString() {
        if (args.length == 0) {
            return messageKey;
        }

        return kind + messageKey + " : " + Arrays.toString(args);
    }

    /**
     * Returns the concatenation of the lists.
     *
     * @param list1 a list of DiagMessage, or null
     * @param list2 a list of DiagMessage, or null
     * @return the concatenation of the lists
     */
    public static @Nullable List<DiagMessage> mergeLists(
            @Nullable List<DiagMessage> list1, @Nullable List<DiagMessage> list2) {
        if (list1 == null || list1.isEmpty()) {
            return list2;
        } else if (list2 == null || list2.isEmpty()) {
            return list1;
        } else {
            List<DiagMessage> result = new ArrayList<>(list1.size() + list2.size());
            result.addAll(list1);
            result.addAll(list2);
            return result;
        }
    }
}
