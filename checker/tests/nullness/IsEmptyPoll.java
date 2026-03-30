// Test case for Issue 399:
// https://github.com/typetools/checker-framework/issues/399

import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.ArrayList;
import java.util.Queue;

public final class IsEmptyPoll extends ArrayList<String> {

    void mNonNull(Queue<String> q) {
        while (!q.isEmpty()) {
            @NonNull String firstNode = q.poll();
        }
    }

    void noSideEffectMethod(Queue<String> q) {
        while (!q.isEmpty()) {
            q.size();
            @NonNull String firstNode = q.poll();
        }
    }

    void mNullable(Queue<@Nullable String> q) {
        while (!q.isEmpty()) {
            // :: error: (assignment.type.incompatible)
            @NonNull String firstNode = q.poll();
        }
    }

    void mNoCheck(Queue<@Nullable String> q) {
        // :: error: (assignment.type.incompatible)
        @NonNull String firstNode = q.poll();
    }

    void secondPoll(Queue<String> q) {
        while (!q.isEmpty()) {
            @NonNull String firstNode = q.poll();
            // :: error: (assignment.type.incompatible)
            @NonNull String secondNode = q.poll();
        }
    }

    void replaceQueue(Queue<String> q1, Queue<String> q2) {
        while (!q1.isEmpty()) {
            q1 = q2;
            // :: error: (assignment.type.incompatible)
            @NonNull String firstNode = q1.poll();
        }
    }

    void removeBeforePoll(Queue<String> q) {
        while (!q.isEmpty()) {
            q.remove();
            // :: error: (assignment.type.incompatible)
            @NonNull String firstNode = q.poll();
        }
    }

    void clearBeforePoll(Queue<String> q) {
        while (!q.isEmpty()) {
            q.clear();
            // :: error: (assignment.type.incompatible)
            @NonNull String firstNode = q.poll();
        }
    }

    void conditionalClearBeforePoll(Queue<String> q, boolean bool) {
        while (!q.isEmpty()) {
            if (bool) {
                q.clear();
            }
            // :: error: (assignment.type.incompatible)
            @NonNull String firstNode = q.poll();
        }
    }

    void aliasClearBeforePoll(Queue<String> q) {
        Queue<String> a = q;
        while (!q.isEmpty()) {
            a.clear();
            // :: error: (assignment.type.incompatible)
            @NonNull String firstNode = q.poll();
        }
    }

    void potentiallyRelatedMutation(Queue<String> q1, Queue<String> q2) {
        while (!q1.isEmpty()) {
            q2.clear();
            // :: error: (assignment.type.incompatible)
            @NonNull String firstNode = q1.poll();
        }
    }

    void indexPoll(Queue<String>[] arr, int i) {
        while (!arr[i].isEmpty()) {
            i++;
            // :: error: (assignment.type.incompatible)
            @NonNull String firstNode = arr[i].poll();
        }
    }

    void clearViaArg(Queue<String> q) {
        q.clear();
    }

    void argMutate(Queue<String> q) {
        while (!q.isEmpty()) {
            clearViaArg(q);
            // :: error: (assignment.type.incompatible)
            @NonNull String firstNode = q.poll();
        }
    }
}
