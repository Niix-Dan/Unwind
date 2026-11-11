package com.niixlabs.unwind.api;

public record ActionRecord(
        long timestampMs,
        long gameTick,
        String threadName,
        String logicalSide,
        String category,
        String description,
        String callerModId,
        String callerFrame
) {
    @Override
    public String toString() {
        return String.format("[Tick %d | %s | %s] [%s] %s (Caller: %s | Mod: %s)",
                gameTick, logicalSide, threadName, category, description, callerFrame, callerModId);
    }
}