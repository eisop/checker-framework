package com.example;

import java.util.Set;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public class Demo {
  void demo(Set<Number> sn, @Nullable Number nn) {
    sn.add(nn);
  }
}
