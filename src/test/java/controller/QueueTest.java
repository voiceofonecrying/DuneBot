package controller;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Regresses the "whisper storm breaks a game until restart" bug. {@link Queue#submit} and
 * {@link Queue#submitSync} must be self-healing so one throwing submission cannot block
 * every subsequent command for the same id. Cleanup runnables must run even when the work
 * throws, so counters like CommandCompletionGuard and lookup sets like buttonMessageIds
 * cannot leak.
 */
class QueueTest {

    @Test
    void submitSyncRunsWorkOnceAndThenSecondSubmissionAfterFirstThrows() {
        String id = "submitSyncRunsWorkOnceAndThenSecondSubmissionAfterFirstThrows";
        AtomicInteger first = new AtomicInteger();
        AtomicInteger second = new AtomicInteger();

        Queue.submitSync(id, () -> {
            first.incrementAndGet();
            throw new RuntimeException("first submission blew up");
        });
        Queue.submitSync(id, second::incrementAndGet);

        assertEquals(1, first.get(), "first submission should have executed (and thrown)");
        assertEquals(1, second.get(), "second submission must still run despite the first failing");
    }

    @Test
    void submitSyncCleanupsRunEvenWhenWorkThrows() {
        String id = "submitSyncCleanupsRunEvenWhenWorkThrows";
        AtomicInteger cleanupA = new AtomicInteger();
        AtomicInteger cleanupB = new AtomicInteger();

        Queue.submitSync(id,
                () -> { throw new RuntimeException("boom"); },
                cleanupA::incrementAndGet,
                cleanupB::incrementAndGet);

        assertEquals(1, cleanupA.get(), "first cleanup should run after failing work");
        assertEquals(1, cleanupB.get(), "second cleanup should run after failing work");
    }

    @Test
    void submitSyncCleanupsRunAfterSuccessfulWork() {
        String id = "submitSyncCleanupsRunAfterSuccessfulWork";
        AtomicInteger cleanup = new AtomicInteger();

        Queue.submitSync(id, () -> {}, cleanup::incrementAndGet);

        assertEquals(1, cleanup.get());
    }

    @Test
    void submitSyncSerializesWorkInSubmissionOrder() {
        String id = "submitSyncSerializesWorkInSubmissionOrder";
        List<Integer> observed = new ArrayList<>();

        Queue.submitSync(id, () -> observed.add(1));
        Queue.submitSync(id, () -> observed.add(2));
        Queue.submitSync(id, () -> observed.add(3));

        assertEquals(List.of(1, 2, 3), observed);
    }

    @Test
    void submitAsyncRunsWorkAndCleanupAfterPriorFailure() throws Exception {
        String id = "submitAsyncRunsWorkAndCleanupAfterPriorFailure";
        AtomicInteger first = new AtomicInteger();
        AtomicInteger second = new AtomicInteger();
        AtomicInteger cleanup = new AtomicInteger();

        Queue.submit(id, () -> {
            first.incrementAndGet();
            throw new RuntimeException("async first blew up");
        });
        // Must wait for the second submission's chain to finish so we can assert on it.
        Queue.submit(id, second::incrementAndGet, cleanup::incrementAndGet).get();

        assertEquals(1, first.get());
        assertEquals(1, second.get(), "second async submission must run despite the first failing");
        assertEquals(1, cleanup.get(), "cleanup must run after successful work");
    }

    @Test
    void separateIdsDoNotBlockEachOther() {
        String idA = "separateIdsDoNotBlockEachOther_A";
        String idB = "separateIdsDoNotBlockEachOther_B";
        AtomicInteger b = new AtomicInteger();

        Queue.submitSync(idA, () -> { throw new RuntimeException("A blew up"); });
        Queue.submitSync(idB, b::incrementAndGet);

        assertTrue(b.get() == 1, "failure on one id must not affect another id");
    }
}
