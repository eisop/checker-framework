import java.util.Optional;

/**
 * Test case for rule #3: "Prefer alternative APIs over Optional.isPresent() and Optional.get()."
 */
@SuppressWarnings("optional.parameter")
public class Marks3b {

    class Task {}

    class Executor {
        void runTask(Task t) {}
    }

    Executor executor = new Executor();

    void bad(Optional<Task> oTask) {
        // Missing `else`: the pattern applies.
        // :: warning: (prefer.ifpresent)
        if (oTask.isPresent()) {
            executor.runTask(oTask.get());
        }
    }

    void badEmptyElse(Optional<Task> oTask) {
        // Empty `else` block: equivalent to a missing `else`, so the pattern still applies.
        // :: warning: (prefer.ifpresent)
        if (oTask.isPresent()) {
            executor.runTask(oTask.get());
        } else {
        }
    }

    void okNonEmptyElse(Optional<Task> oTask) {
        // Non-empty `else` block: `ifPresent` cannot express the `else` branch's behavior, so no
        // warning is issued.
        if (oTask.isPresent()) {
            executor.runTask(oTask.get());
        } else {
            System.out.println("no task");
        }
    }

    void better(Optional<Task> oTask) {
        // no warning; better code is possible but has nothing to do with Optional
        oTask.ifPresent(task -> executor.runTask(task));
    }

    void best(Optional<Task> oTask) {
        oTask.ifPresent(executor::runTask);
    }
}
