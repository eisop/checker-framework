package org.checkerframework.checker.optional.util;

import org.checkerframework.checker.optional.qual.Present;
import org.junit.Assert;
import org.junit.Test;

import java.util.Optional;

public final class OptionalUtilTest {

    @Test
    public void test_castPresent() {

        Optional<String> nonEmptyOpt = Optional.of("non-empty");
        Optional<String> emptyOpt = Optional.empty();

        Assert.assertTrue(nonEmptyOpt.isPresent());
        @Present Optional<String> foo = OptionalUtil.castPresent(nonEmptyOpt);
        Assert.assertEquals(foo.get(), "non-empty");

        Assert.assertFalse(emptyOpt.isPresent());
        Assert.assertThrows(Error.class, () -> OptionalUtil.castPresent(emptyOpt));
    }
}
