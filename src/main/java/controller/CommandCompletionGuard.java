package controller;

import java.util.concurrent.atomic.AtomicInteger;

public class CommandCompletionGuard {
    private static final AtomicInteger commandCount = new AtomicInteger(0);

    public static void incrementCommandCount() {
        commandCount.incrementAndGet();
    }

    public static void decrementCommandCount() {
        commandCount.decrementAndGet();
    }

    public static boolean isCommandInProgress() {
        return commandCount.get() > 0;
    }

    public static void blockUntilNoCommands() {
        while (isCommandInProgress()) {
            try {
                Thread.sleep(250);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}
