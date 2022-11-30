import org.checkerframework.checker.testchecker.ainfer.qual.AinferBottom;
import org.checkerframework.checker.testchecker.ainfer.qual.AinferParent;
import org.checkerframework.checker.testchecker.ainfer.qual.AinferSibling1;
import org.checkerframework.checker.testchecker.ainfer.qual.AinferSibling2;
import org.checkerframework.checker.testchecker.ainfer.qual.AinferTop;

public class PublicFieldTest {
  public static int field1; // parent
  public static int field2; // sib2

  public PublicFieldTest() {
    field1 = getAinferSibling1();
  }

  void testPublicInference() {
    // :: warning: (argument.type.incompatible)
    expectsAinferSibling2(field2);
    // :: warning: (argument.type.incompatible)
    expectsParent(field1);
    // :: warning: (argument.type.incompatible)
    expectsParent(field2);
  }

  void expectsBottom(@AinferBottom int t) {}

  void expectsAinferSibling1(@AinferSibling1 int t) {}

  void expectsAinferSibling2(@AinferSibling2 int t) {}

  void expectsAinferTop(@AinferTop int t) {}

  void expectsParent(@AinferParent int t) {}

  @AinferSibling1
  int getAinferSibling1() {
    return (@AinferSibling1 int) 0;
  }

  class AnotherClass {

    int innerField;

    public AnotherClass() {
      PublicFieldTest.field1 = getAinferSibling2();
      PublicFieldTest.field2 = getAinferSibling2();
      innerField = getAinferSibling2();
    }

    void innerFieldTest() {
      // :: warning: (argument.type.incompatible)
      expectsAinferSibling2(innerField);
    }

    @AinferBottom int getBottom() {
      return (@AinferBottom int) 0;
    }

    @AinferTop
    int getAinferTop() {
      return (@AinferTop int) 0;
    }

    @AinferSibling2
    int getAinferSibling2() {
      return (@AinferSibling2 int) 0;
    }

    void expectsAinferSibling2(@AinferSibling2 int t) {}
  }
}
