package com.example;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.util.Set;

@NullMarked
public class Demo {
    void demo(Set<Number> sn, @Nullable Number nn) {
        sn.add(nn);
    }
}
