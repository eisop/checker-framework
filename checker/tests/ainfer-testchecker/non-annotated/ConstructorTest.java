import org.checkerframework.checker.testchecker.ainfer.qual.Top;

public class ConstructorTest {

  public ConstructorTest(int top) {}

  void test() {
    @Top int top = (@Top int) 0;
    // :: warning: (argument.type.incompatible)
    new ConstructorTest(top);
  }
}
