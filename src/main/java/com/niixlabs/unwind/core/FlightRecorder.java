package com.niixlabs.unwind.core;

import com.niixlabs.unwind.analysis.ModProvenanceResolver;
import com.niixlabs.unwind.api.ActionRecord;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public final class FlightRecorder {

    private static final int MAX_HISTORY = 150;
    private static final Deque<ActionRecord> RING_BUFFER = new ArrayDeque<>(MAX_HISTORY);
    private static final ReentrantReadWriteLock LOCK = new ReentrantReadWriteLock();
    private static final ThreadLocal<Deque<ActionRecord>> ACTIVE_SCOPE = ThreadLocal.withInitial(ArrayDeque::new);

    private FlightRecorder() {}

    public static void pushScope(long gameTick, boolean isClientSide, String category, String description) {
        pushScope(gameTick, isClientSide, category, description, true);
    }

    public static void pushTransientScope(long gameTick, boolean isClientSide, String category, String description) {
        pushScope(gameTick, isClientSide, category, description, false);
    }

    private static void pushScope(long gameTick, boolean isClientSide, String category, String description, boolean archiveToHistory) {
        StackWalker.StackFrame externalFrame = ModProvenanceResolver.findFirstExternalModFrame();
        StackWalker.StackFrame immediateFrame = ModProvenanceResolver.findImmediateCallerFrame();

        String modId = externalFrame != null
                ? ModProvenanceResolver.resolveModId(externalFrame.getDeclaringClass())
                : (immediateFrame != null ? ModProvenanceResolver.resolveModId(immediateFrame.getDeclaringClass()) : "minecraft");

        String formattedCaller = ModProvenanceResolver.formatFrameWithMod(
                externalFrame != null ? externalFrame : immediateFrame
        );

        ActionRecord record = new ActionRecord(
                System.currentTimeMillis(),
                gameTick,
                Thread.currentThread().getName(),
                isClientSide ? "CLIENT" : "SERVER",
                category,
                description,
                modId,
                formattedCaller
        );

        Deque<ActionRecord> scopeStack = ACTIVE_SCOPE.get();
        if (scopeStack.size() >= 32) {
            scopeStack.pollLast();
        }
        scopeStack.push(record);

        if (archiveToHistory) {
            recordEvent(record);
        }
    }

    public static void popScope() {
        Deque<ActionRecord> stack = ACTIVE_SCOPE.get();
        if (!stack.isEmpty()) {
            stack.pop();
        }
    }

    public static void clearCurrentThreadScopes() {
        ACTIVE_SCOPE.get().clear();
    }

    public static void recordEvent(ActionRecord record) {
        LOCK.writeLock().lock();
        try {
            ActionRecord last = RING_BUFFER.peekLast();
            if (last != null
                    && last.category().equals(record.category())
                    && last.description().equals(record.description())
                    && last.logicalSide().equals(record.logicalSide())) {
                return;
            }
            if (RING_BUFFER.size() >= MAX_HISTORY) {
                RING_BUFFER.pollFirst();
            }
            RING_BUFFER.addLast(record);
        } finally {
            LOCK.writeLock().unlock();
        }
    }

    public static List<ActionRecord> getActiveScopesForCurrentThread() {
        return new ArrayList<>(ACTIVE_SCOPE.get());
    }

    public static List<ActionRecord> getRecentHistorySnapshot() {
        LOCK.readLock().lock();
        try {
            return new ArrayList<>(RING_BUFFER);
        } finally {
            LOCK.readLock().unlock();
        }
    }
}