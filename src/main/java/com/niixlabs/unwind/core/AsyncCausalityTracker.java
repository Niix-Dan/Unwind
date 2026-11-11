package com.niixlabs.unwind.core;

import com.niixlabs.unwind.analysis.ModProvenanceResolver;

import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;

public final class AsyncCausalityTracker {

    private static final Map<Runnable, String> TASK_PROVENANCE = Collections.synchronizedMap(new WeakHashMap<>());
    private static final ThreadLocal<String> CURRENT_TASK_ORIGIN = new ThreadLocal<>();

    private AsyncCausalityTracker() {}

    public static void tagRunnableOnSchedule(Runnable task) {
        if (task == null) return;
        StackWalker.StackFrame externalCaller = ModProvenanceResolver.findFirstExternalModFrame();
        StackWalker.StackFrame immediateCaller = ModProvenanceResolver.findImmediateCallerFrame();
        StackWalker.StackFrame target = externalCaller != null ? externalCaller : immediateCaller;

        String origin = String.format("Enqueued on Thread [%s] by %s",
                Thread.currentThread().getName(),
                ModProvenanceResolver.formatFrameWithMod(target)
        );
        TASK_PROVENANCE.put(task, origin);
    }

    public static void onEnterTask(Runnable task) {
        String origin = TASK_PROVENANCE.get(task);
        if (origin != null) {
            CURRENT_TASK_ORIGIN.set(origin);
        }
    }

    public static void onExitTask() {
        CURRENT_TASK_ORIGIN.remove();
    }

    public static String getCurrentTaskOrigin() {
        String origin = CURRENT_TASK_ORIGIN.get();
        return origin != null ? origin : "Direct Synchronous Execution (Same Thread)";
    }
}