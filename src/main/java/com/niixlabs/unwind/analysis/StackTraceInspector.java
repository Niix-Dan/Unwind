package com.niixlabs.unwind.analysis;

import com.niixlabs.unwind.api.MixinFrameMetadata;
import com.niixlabs.unwind.api.StackFrameProvenance;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class StackTraceInspector {

    private static final Pattern MIXIN_SYNTHETIC_PATTERN = Pattern.compile(
            "^(handler|redirect|modify\\w*|wrapOperation|md)\\$([a-zA-Z0-9]+)\\$([a-zA-Z0-9_\\-]+)\\$(.+)$"
    );

    private StackTraceInspector() {}

    public static List<StackFrameProvenance> extractProvenanceChain(Throwable throwable) {
        List<StackFrameProvenance> list = new ArrayList<>();
        if (throwable == null) return list;

        StackTraceElement[] elements = throwable.getStackTrace();
        ClassLoader contextLoader = Thread.currentThread().getContextClassLoader();

        for (int i = 0; i < elements.length; i++) {
            StackTraceElement el = elements[i];
            String moduleName = el.getModuleName() != null ? el.getModuleName() : "unnamed";
            String modId = moduleName;
            String modVersion = "N/A";
            String jarName = "unknown";
            String classLoaderName = "bootstrap";

            try {
                Class<?> clazz = Class.forName(el.getClassName(), false, contextLoader);
                modId = ModProvenanceResolver.resolveModId(clazz);
                modVersion = ModProvenanceResolver.resolveModVersion(modId);
                jarName = ModProvenanceResolver.resolveModJar(modId);
                if (clazz.getClassLoader() != null) {
                    classLoaderName = clazz.getClassLoader().getName() != null
                            ? clazz.getClassLoader().getName()
                            : clazz.getClassLoader().getClass().getSimpleName();
                }
            } catch (Throwable ignored) {
                modVersion = ModProvenanceResolver.resolveModVersion(modId);
                jarName = ModProvenanceResolver.resolveModJar(modId);
            }

            list.add(new StackFrameProvenance(
                    i,
                    el.getClassName(),
                    el.getMethodName(),
                    el.getFileName() != null ? el.getFileName() : "UnknownSource",
                    el.getLineNumber(),
                    moduleName,
                    modId,
                    modVersion,
                    jarName,
                    classLoaderName
            ));
        }
        return list;
    }

    public static List<MixinFrameMetadata> extractMixinFrames(Throwable throwable) {
        List<MixinFrameMetadata> results = new ArrayList<>();
        if (throwable == null) return results;

        for (Throwable current = throwable; current != null; current = current.getCause()) {
            StackTraceElement[] stack = current.getStackTrace();
            for (int i = 0; i < stack.length; i++) {
                StackTraceElement el = stack[i];
                Matcher matcher = MIXIN_SYNTHETIC_PATTERN.matcher(el.getMethodName());
                if (matcher.matches()) {
                    results.add(new MixinFrameMetadata(
                            i,
                            el.getClassName(),
                            el.getMethodName(),
                            matcher.group(1),
                            matcher.group(2),
                            matcher.group(3),
                            matcher.group(4),
                            el.getLineNumber()
                    ));
                }
            }
        }
        return results;
    }
}