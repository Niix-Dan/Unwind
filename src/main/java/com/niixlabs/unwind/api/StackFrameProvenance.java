package com.niixlabs.unwind.api;

public record StackFrameProvenance(
        int index,
        String className,
        String methodName,
        String fileName,
        int lineNumber,
        String moduleName,
        String modId,
        String modVersion,
        String jarFileName,
        String classLoaderName
) {}