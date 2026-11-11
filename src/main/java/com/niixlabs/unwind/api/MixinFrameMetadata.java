package com.niixlabs.unwind.api;

public record MixinFrameMetadata(
        int frameIndex,
        String targetClass,
        String rawMethodName,
        String injectorType,
        String handlerId,
        String sourceModId,
        String sourceMethodName,
        int lineNumber
) {}