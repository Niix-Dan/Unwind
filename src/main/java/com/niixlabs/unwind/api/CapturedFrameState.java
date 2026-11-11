package com.niixlabs.unwind.api;

import java.util.Map;

public record CapturedFrameState(
        long timestampMs,
        long gameTick,
        String phase,
        String targetMethod,
        String logicalSide,
        String modId,
        String modJar,
        Map<String, String> capturedArguments
) {}