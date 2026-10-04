// Test case for Issue 399:
// https://github.com/typetools/checker-framework/issues/399

import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.checker.nullness.qual.PolyNull;

import java.util.ArrayList;
import java.util.Deque;
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

    @SuppressWarnings("rawtypes")
    void rawQueue(Queue q) {
        while (!q.isEmpty()) {
            // :: error: (assignment.type.incompatible)
            @NonNull Object firstNode = q.poll();
        }
    }

    void mPolyNull(Queue<@PolyNull String> q) {
        while (!q.isEmpty()) {
            // :: error: (assignment.type.incompatible)
            @NonNull String firstNode = q.poll();
        }
    }

    void mPeekNonNull(Queue<String> q) {
        while (!q.isEmpty()) {
            @NonNull String firstNode = q.peek();
            @NonNull String secondNode = q.peek();
        }
    }

    void mPeekThenPoll(Queue<String> q) {
        while (!q.isEmpty()) {
            @NonNull String peekedNode = q.peek();
            @NonNull String polledNode = q.poll();
        }
    }

    void mPeekNullable(Queue<@Nullable String> q) {
        while (!q.isEmpty()) {
            // :: error: (assignment.type.incompatible)
            @NonNull String firstNode = q.peek();
        }
    }

    void mPeekNoCheck(Queue<@Nullable String> q) {
        // :: error: (assignment.type.incompatible)
        @NonNull String firstNode = q.peek();
    }

    void mPeekAfterPoll(Queue<String> q) {
        while (!q.isEmpty()) {
            @NonNull String firstNode = q.poll();
            // :: error: (assignment.type.incompatible)
            @NonNull String peekedNode = q.peek();
        }
    }

    void mClearBeforePeek(Queue<String> q) {
        while (!q.isEmpty()) {
            q.clear();
            // :: error: (assignment.type.incompatible)
            @NonNull String firstNode = q.peek();
        }
    }

    void mDequePollFirst(Deque<String> d) {
        while (!d.isEmpty()) {
            @NonNull String firstNode = d.pollFirst();
        }
    }

    void mDequePollLast(Deque<String> d) {
        while (!d.isEmpty()) {
            @NonNull String lastNode = d.pollLast();
        }
    }

    void mDequePeekFirstAndLast(Deque<String> d) {
        while (!d.isEmpty()) {
            @NonNull String firstNode = d.peekFirst();
            @NonNull String lastNode = d.peekLast();
        }
    }

    void mDequePeekThenPoll(Deque<String> d) {
        while (!d.isEmpty()) {
            @NonNull String peekedNode = d.peekFirst();
            @NonNull String polledNode = d.pollLast();
        }
    }

    void mDequeSecondPoll(Deque<String> d) {
        while (!d.isEmpty()) {
            @NonNull String firstNode = d.pollFirst();
            // :: error: (assignment.type.incompatible)
            @NonNull String secondNode = d.pollLast();
        }
    }

    void mDequeNullable(Deque<@Nullable String> d) {
        while (!d.isEmpty()) {
            // :: error: (assignment.type.incompatible)
            @NonNull String firstNode = d.pollFirst();
            // :: error: (assignment.type.incompatible)
            @NonNull String lastNode = d.peekLast();
        }
    }

    <T extends @NonNull Object> void mQueueTypeVarNonNull(Queue<T> q) {
        while (!q.isEmpty()) {
            @NonNull T peeked = q.peek();
            @NonNull T polled = q.poll();
        }
    }

    <T extends @Nullable Object> void mQueueTypeVarNullable(Queue<T> q) {
        while (!q.isEmpty()) {
            // :: error: (assignment.type.incompatible)
            @NonNull T peeked = q.peek();
            // :: error: (assignment.type.incompatible)
            @NonNull T polled = q.poll();
        }
    }

    <T extends @NonNull Object> void mQueueTypeVarPollThenPeek(Queue<T> q) {
        while (!q.isEmpty()) {
            @NonNull T polled = q.poll();
            // :: error: (assignment.type.incompatible)
            @NonNull T peeked = q.peek();
        }
    }
}
