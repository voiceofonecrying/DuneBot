package controller;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Per-id serialized work queue. Each id (a Discord game category name, in production) owns a
 * CompletableFuture chain; submitting runs work after any prior work for that id finishes.
 *
 * The chain is self-healing: a throwing submission does not poison subsequent submissions.
 * Cleanup runnables run even when the work throws. This invariant is what {@link #submit} and
 * {@link #submitSync} exist to guarantee — callers must go through these methods rather than
 * building chains manually, otherwise one bad command can lock up an entire game until the
 * JVM restarts.
 */
public class Queue {
    private static final Map<String, CompletableFuture<Void>> queue = new ConcurrentHashMap<>();

    public static CompletableFuture<Void> getFuture(String id) {
        return queue.computeIfAbsent(id, _ -> CompletableFuture.completedFuture(null));
    }

    public static void putFuture(String id, CompletableFuture<Void> future) {
        queue.put(id, future);
    }

    /**
     * Submit async work for the given id. Work runs on the common ForkJoinPool after any
     * previously submitted work for this id finishes. Any exception from a prior submission
     * is absorbed before work runs, and each cleanup runs after work completes regardless of
     * whether work threw. Returns the final stage for callers (typically tests) that need to
     * await completion; production callers may ignore it.
     */
    public static CompletableFuture<Void> submit(String id, Runnable work, Runnable... cleanups) {
        CompletableFuture<Void> chain = getFuture(id)
                .handleAsync((_, _) -> null)
                .thenRunAsync(work);
        for (Runnable cleanup : cleanups) {
            chain = chain.whenCompleteAsync((_, _) -> cleanup.run());
        }
        putFuture(id, chain);
        return chain;
    }

    /**
     * Synchronous variant of {@link #submit} that runs stages on the caller's thread. Used by
     * test infrastructure that forces synchronous execution to avoid thread-local mock issues.
     * Same self-healing + always-cleanup semantics as {@link #submit}.
     */
    public static CompletableFuture<Void> submitSync(String id, Runnable work, Runnable... cleanups) {
        CompletableFuture<Void> chain = getFuture(id)
                .handle((_, _) -> null)
                .thenRun(work);
        for (Runnable cleanup : cleanups) {
            chain = chain.whenComplete((_, _) -> cleanup.run());
        }
        putFuture(id, chain);
        return chain;
    }
}
