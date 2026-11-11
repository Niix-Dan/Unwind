package com.niixlabs.unwind.core;

import com.niixlabs.unwind.analysis.ModProvenanceResolver;
import com.niixlabs.unwind.api.CapturedFrameState;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Map;

public final class FrameCaptureContext {

    private static final int MAX_STACK_DEPTH = 32;
    private static final ThreadLocal<Deque<CapturedFrameState>> ACTIVE_FRAME_STACK =
            ThreadLocal.withInitial(ArrayDeque::new);

    private FrameCaptureContext() {}

    public static void pushFrame(
            long gameTick,
            String phase,
            String targetMethod,
            String logicalSide,
            Class<?> targetOwnerClass,
            Map<String, String> capturedArguments
    ) {
        String modId = ModProvenanceResolver.resolveModId(targetOwnerClass);
        String modJar = ModProvenanceResolver.resolveModJar(modId);

        CapturedFrameState state = new CapturedFrameState(
                System.currentTimeMillis(),
                gameTick,
                phase,
                targetMethod,
                logicalSide,
                modId,
                modJar,
                capturedArguments
        );

        Deque<CapturedFrameState> stack = ACTIVE_FRAME_STACK.get();
        if (stack.size() >= MAX_STACK_DEPTH) {
            stack.pollLast();
        }
        stack.push(state);
    }

    public static void popFrame() {
        Deque<CapturedFrameState> stack = ACTIVE_FRAME_STACK.get();
        if (!stack.isEmpty()) {
            stack.pop();
        }
    }

    public static List<CapturedFrameState> snapshotActiveFrames() {
        return new ArrayList<>(ACTIVE_FRAME_STACK.get());
    }

    public static void clearThreadFrames() {
        ACTIVE_FRAME_STACK.get().clear();
    }
}