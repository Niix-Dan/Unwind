package com.niixlabs.unwind.api;

public record MutationRecord(
        long timestampMs,
        long gameTick,
        String dimension,
        String targetPos,
        String previousState,
        String newState,
        int flags,
        String callerModId,
        String callerFrame,
        String asyncOrigin
) {}