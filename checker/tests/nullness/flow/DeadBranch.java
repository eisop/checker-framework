import org.checkerframework.checker.nullness.qual.Nullable;

class DeadBranch {

    Object foo(@Nullable Object myObj, boolean x) {
        if (true) {
            myObj = new Object();
        }
        return myObj;
    }
}
