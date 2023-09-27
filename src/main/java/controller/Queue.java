package controller;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class Queue {
    private static final Map<String, CompletableFuture<Void>> queue = new HashMap<>();

    public static CompletableFuture<Void> getFuture(String id) {
        if (queue.containsKey(id)) {
            return queue.get(id);
        } else {
            CompletableFuture<Void> future = CompletableFuture.completedFuture(null);
            queue.put(id, future);
            return future;
        }
    }

    public static void putFuture(String id, CompletableFuture<Void> future) {
        queue.put(id, future);
    }
}
