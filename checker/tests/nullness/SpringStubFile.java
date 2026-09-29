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
}
