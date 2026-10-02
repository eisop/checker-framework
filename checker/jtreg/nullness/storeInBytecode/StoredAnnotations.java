/*
 * @test
 * @summary Test which qualifiers are stored in the class file, by reading them back with
 *          reflection.  The two failures of eisop issue 1291 come from qualifiers stored on a
 *          class type parameter (read by Kotlin) and on a field type (rejected by javac 25), and
 *          include Initialized from the Initialization Checker, so a test of downstream
 *          nullness diagnostics alone cannot see them.
 *
 * @compile -processor org.checkerframework.checker.nullness.NullnessChecker ../storeInBytecodeLib/DefaultStorage.java
 * @compile -processor org.checkerframework.checker.nullness.NullnessChecker -AstoreInBytecode=false ../storeInBytecodeLib/ExplicitFalseStorage.java
 * @compile -processor org.checkerframework.checker.nullness.NullnessChecker -Amode=jspecify ../storeInBytecodeLib/ModeStorage.java
 * @compile -processor org.checkerframework.checker.nullness.NullnessChecker -Amode=jspecify -AstoreInBytecode ../storeInBytecodeLib/ModeBareFlag.java
 * @run main StoredAnnotations
 */

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedParameterizedType;
import java.lang.reflect.AnnotatedType;
import java.lang.reflect.TypeVariable;
import java.util.ArrayList;
import java.util.List;

public class StoredAnnotations {
  /** Qualifiers that the library's source does not write, so only storage can put there. */
  private static final String INITIALIZED =
      "org.checkerframework.checker.initialization.qual.Initialized";

  private static final String NONNULL = "org.checkerframework.checker.nullness.qual.NonNull";

  private static final String UNKNOWNKEYFOR =
      "org.checkerframework.checker.nullness.qual.UnknownKeyFor";

  /** Written in the library's source, so present whether or not qualifiers are stored. */
  private static final String NULLABLE = "org.checkerframework.checker.nullness.qual.Nullable";

  public static void main(String[] args) throws Exception {
    // The default stores defaulted qualifiers, including the Initialization Checker's.
    List<String> stored = annotations("storeinbytecodelib.DefaultStorage");
    require(stored.contains(NULLABLE), "DefaultStorage: source @Nullable missing", stored);
    require(stored.contains(INITIALIZED), "DefaultStorage: no @Initialized stored", stored);
    require(stored.contains(NONNULL), "DefaultStorage: no @NonNull stored", stored);

    // -AstoreInBytecode=false, without a mode, omits them for every checker too.
    List<String> off = annotations("storeinbytecodelib.ExplicitFalseStorage");
    require(off.contains(NULLABLE), "ExplicitFalseStorage: source @Nullable missing", off);
    require(!off.contains(INITIALIZED), "ExplicitFalseStorage: @Initialized stored", off);
    require(!off.contains(NONNULL), "ExplicitFalseStorage: @NonNull stored", off);
    require(!off.contains(UNKNOWNKEYFOR), "ExplicitFalseStorage: @UnknownKeyFor stored", off);

    // -Amode=jspecify implies -AstoreInBytecode=false for every checker it runs.
    List<String> mode = annotations("storeinbytecodelib.ModeStorage");
    require(mode.contains(NULLABLE), "ModeStorage: source @Nullable missing", mode);
    require(!mode.contains(INITIALIZED), "ModeStorage: @Initialized stored", mode);
    require(!mode.contains(NONNULL), "ModeStorage: @NonNull stored", mode);
    require(!mode.contains(UNKNOWNKEYFOR), "ModeStorage: @UnknownKeyFor stored", mode);

    // A bare -AstoreInBytecode means true, so it keeps storage on under the mode.
    List<String> bare = annotations("storeinbytecodelib.ModeBareFlag");
    require(bare.contains(INITIALIZED), "ModeBareFlag: no @Initialized stored", bare);
    require(bare.contains(NONNULL), "ModeBareFlag: no @NonNull stored", bare);
  }

  /**
   * Returns the names of the type annotations on the class's type parameters, their bounds, and its
   * fields' types and type arguments.
   */
  private static List<String> annotations(String className) throws Exception {
    Class<?> c = Class.forName(className);
    List<String> names = new ArrayList<>();
    for (TypeVariable<?> tv : c.getTypeParameters()) {
      add(tv.getAnnotations(), names);
      for (AnnotatedType bound : tv.getAnnotatedBounds()) {
        add(bound.getAnnotations(), names);
      }
    }
    for (java.lang.reflect.Field f : c.getDeclaredFields()) {
      AnnotatedType type = f.getAnnotatedType();
      add(type.getAnnotations(), names);
      if (type instanceof AnnotatedParameterizedType) {
        for (AnnotatedType arg :
            ((AnnotatedParameterizedType) type).getAnnotatedActualTypeArguments()) {
          add(arg.getAnnotations(), names);
        }
      }
    }
    return names;
  }

  private static void add(Annotation[] annos, List<String> names) {
    for (Annotation a : annos) {
      names.add(a.annotationType().getName());
    }
  }

  private static void require(boolean condition, String message, List<String> found) {
    if (!condition) {
      throw new AssertionError(message + "; found " + found);
    }
  }
}
