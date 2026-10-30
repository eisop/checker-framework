// Tests spring.astub, which gives the Nullness Checker the postconditions that Spring
// declares with its own @Contract.  The Spring classes under test are the stand-ins in
// checker/src/testannotations/java/org/springframework/util, so this test needs no
// dependency on Spring itself.

import org.checkerframework.checker.nullness.qual.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

import java.util.Collection;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.Map;

public class SpringStubFile {

    // @Contract("null -> false"): a true result means the argument is non-null.

    int hasLengthString(@Nullable String s) {
        return StringUtils.hasLength(s) ? s.length() : 0;
    }

    int hasLengthCharSequence(@Nullable CharSequence s) {
        return StringUtils.hasLength(s) ? s.length() : 0;
    }

    int hasTextString(@Nullable String s) {
        if (StringUtils.hasText(s)) {
            return s.length();
        }
        return 0;
    }

    int hasTextCharSequence(@Nullable CharSequence s) {
        if (StringUtils.hasText(s)) {
            return s.length();
        }
        return 0;
    }

    int containsWhitespaceString(@Nullable String s) {
        return StringUtils.containsWhitespace(s) ? s.length() : 0;
    }

    int containsWhitespaceCharSequence(@Nullable CharSequence s) {
        return StringUtils.containsWhitespace(s) ? s.length() : 0;
    }

    int isArray(@Nullable Object o) {
        return ObjectUtils.isArray(o) ? o.hashCode() : 0;
    }

    // @Contract("null, _ -> false"): a true result means the first argument is non-null.

    int matchesCharacter(@Nullable String s) {
        return StringUtils.matchesCharacter(s, 'x') ? s.length() : 0;
    }

    int containsElement(@Nullable Object @Nullable [] a, Object e) {
        return ObjectUtils.containsElement(a, e) ? a.length : 0;
    }

    boolean containsIterator(@Nullable Iterator<String> it, Object e) {
        return CollectionUtils.contains(it, e) && it.hasNext();
    }

    boolean containsEnumeration(@Nullable Enumeration<String> en, Object e) {
        return CollectionUtils.contains(en, e) && en.hasMoreElements();
    }

    int containsInstance(@Nullable Collection<String> c, Object e) {
        return CollectionUtils.containsInstance(c, e) ? c.size() : 0;
    }

    // @Contract("null, _ -> false; _, null -> false"): a true result means both are non-null.

    int startsWithIgnoreCase(@Nullable String s, @Nullable String prefix) {
        if (StringUtils.startsWithIgnoreCase(s, prefix)) {
            return s.length() + prefix.length();
        }
        return 0;
    }

    int endsWithIgnoreCase(@Nullable String s, @Nullable String suffix) {
        if (StringUtils.endsWithIgnoreCase(s, suffix)) {
            return s.length() + suffix.length();
        }
        return 0;
    }

    // @Contract("null -> true"): a false result means the argument is non-null.

    int collectionIsEmpty(@Nullable Collection<String> c) {
        if (!CollectionUtils.isEmpty(c)) {
            return c.size();
        }
        return 0;
    }

    int mapIsEmpty(@Nullable Map<String, String> m) {
        return CollectionUtils.isEmpty(m) ? 0 : m.size();
    }

    int objectIsEmpty(@Nullable Object o) {
        if (!ObjectUtils.isEmpty(o)) {
            return o.hashCode();
        }
        return 0;
    }

    int arrayIsEmpty(@Nullable String @Nullable [] a) {
        if (!ObjectUtils.isEmpty(a)) {
            return a.length;
        }
        return 0;
    }

    int stringUtilsIsEmpty(@Nullable Object o) {
        return StringUtils.isEmpty(o) ? 0 : o.hashCode();
    }

    // @Contract("null, _ -> fail"): returning normally means the argument is non-null.

    int assertNotNull(@Nullable Object o) {
        Assert.notNull(o, "must not be null");
        return o.hashCode();
    }

    int assertNotNullSupplier(@Nullable Object o) {
        Assert.notNull(o, () -> "must not be null");
        return o.hashCode();
    }

    int assertHasText(@Nullable String s) {
        Assert.hasText(s, "must have text");
        return s.length();
    }

    int assertHasTextSupplier(@Nullable String s) {
        Assert.hasText(s, () -> "must have text");
        return s.length();
    }

    int assertHasLength(@Nullable String s) {
        Assert.hasLength(s, "must have length");
        return s.length();
    }

    int assertHasLengthSupplier(@Nullable String s) {
        Assert.hasLength(s, () -> "must have length");
        return s.length();
    }

    int assertNotEmptyArray(@Nullable String @Nullable [] a) {
        Assert.notEmpty(a, "must not be empty");
        return a.length;
    }

    int assertNotEmptyCollection(@Nullable Collection<String> c) {
        Assert.notEmpty(c, "must not be empty");
        return c.size();
    }

    int assertNotEmptyMap(@Nullable Map<String, String> m) {
        Assert.notEmpty(m, "must not be empty");
        return m.size();
    }

    // @Contract("_, null, _ -> fail"): returning normally means the second argument is non-null.

    int assertIsInstanceOf(@Nullable Object o) {
        Assert.isInstanceOf(String.class, o, "must be a String");
        return o.hashCode();
    }

    int assertIsInstanceOfSupplier(@Nullable Object o) {
        Assert.isInstanceOf(String.class, o, () -> "must be a String");
        return o.hashCode();
    }

    int assertIsInstanceOfNoMessage(@Nullable Object o) {
        Assert.isInstanceOf(String.class, o);
        return o.hashCode();
    }

    String assertIsAssignable(@Nullable Class<?> c) {
        Assert.isAssignable(Number.class, c, "must be a Number");
        return c.getName();
    }

    String assertIsAssignableSupplier(@Nullable Class<?> c) {
        Assert.isAssignable(Number.class, c, () -> "must be a Number");
        return c.getName();
    }

    String assertIsAssignableNoMessage(@Nullable Class<?> c) {
        Assert.isAssignable(Number.class, c);
        return c.getName();
    }

    // @Contract("false, _ -> fail"): returning normally means the condition is true.

    int assertState(@Nullable Object o) {
        Assert.state(o != null, "must not be null");
        return o.hashCode();
    }

    int assertStateSupplier(@Nullable Object o) {
        Assert.state(o != null, () -> "must not be null");
        return o.hashCode();
    }

    int assertIsTrue(@Nullable Object o) {
        Assert.isTrue(o != null, "must not be null");
        return o.hashCode();
    }

    int assertIsTrueSupplier(@Nullable Object o) {
        Assert.isTrue(o != null, () -> "must not be null");
        return o.hashCode();
    }

    // The postconditions must not be read as unconditional: an unguarded dereference, and a
    // dereference on the branch the postcondition says nothing about, are both still errors.

    int noGuard(@Nullable String s) {
        StringUtils.hasText(s);
        // :: error: (dereference.of.nullable)
        return s.length();
    }

    int wrongBranch(@Nullable String s) {
        if (StringUtils.hasText(s)) {
            return 0;
        }
        // :: error: (dereference.of.nullable)
        return s.length();
    }

    int wrongBranchIsEmpty(@Nullable Collection<String> c) {
        if (CollectionUtils.isEmpty(c)) {
            // :: error: (dereference.of.nullable)
            return c.size();
        }
        return 0;
    }

    int wrongBranchContainsElement(@Nullable Object @Nullable [] a, Object e) {
        if (ObjectUtils.containsElement(a, e)) {
            return 0;
        }
        // :: error: (dereference.of.nullable)
        return a.length;
    }

    int wrongBranchStartsWithIgnoreCase(@Nullable String s, @Nullable String prefix) {
        if (!StringUtils.startsWithIgnoreCase(s, prefix)) {
            // :: error: (dereference.of.nullable)
            return prefix.length();
        }
        return 0;
    }

    int wrongBranchIsArray(@Nullable Object o) {
        // :: error: (dereference.of.nullable)
        return ObjectUtils.isArray(o) ? 0 : o.hashCode();
    }

    int wrongBranchContainsWhitespace(@Nullable String s) {
        // :: error: (dereference.of.nullable)
        return StringUtils.containsWhitespace(s) ? 0 : s.length();
    }

    int wrongBranchMatchesCharacter(@Nullable String s) {
        // :: error: (dereference.of.nullable)
        return StringUtils.matchesCharacter(s, 'x') ? 0 : s.length();
    }

    int wrongBranchContainsInstance(@Nullable Collection<String> c, Object e) {
        // :: error: (dereference.of.nullable)
        return CollectionUtils.containsInstance(c, e) ? 0 : c.size();
    }

    boolean wrongBranchContainsIterator(@Nullable Iterator<String> it, Object e) {
        // :: error: (dereference.of.nullable)
        return CollectionUtils.contains(it, e) || it.hasNext();
    }

    boolean wrongBranchContainsEnumeration(@Nullable Enumeration<String> en, Object e) {
        // :: error: (dereference.of.nullable)
        return CollectionUtils.contains(en, e) || en.hasMoreElements();
    }

    int wrongBranchEndsWithIgnoreCase(@Nullable String s, @Nullable String suffix) {
        if (StringUtils.endsWithIgnoreCase(s, suffix)) {
            return 0;
        }
        // :: error: (dereference.of.nullable)
        return s.length();
    }

    // Assert.isInstanceOf and isAssignable say nothing about their first argument.

    int isInstanceOfFirstArgument(@Nullable Object o) {
        // :: error: (argument.type.incompatible)
        Assert.isInstanceOf(null, o);
        return o.hashCode();
    }

    // Assert.state and isTrue refine by the condition, not by an argument being non-null: the
    // asserted fact about one variable says nothing about another.

    int assertStateOther(@Nullable Object o, @Nullable Object p) {
        Assert.state(o != null, "must not be null");
        // :: error: (dereference.of.nullable)
        return p.hashCode();
    }

    int assertIsTrueNegated(@Nullable Object o) {
        Assert.isTrue(o == null, "must be null");
        // :: error: (dereference.of.nullable)
        return o.hashCode();
    }

    int isInstanceOfNoGuard(@Nullable Object o) {
        if (o instanceof String) {
            Assert.isInstanceOf(String.class, o);
        }
        // :: error: (dereference.of.nullable)
        return o.hashCode();
    }
}
