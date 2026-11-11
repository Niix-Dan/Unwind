package com.niixlabs.unwind.core;

import com.niixlabs.unwind.analysis.ModProvenanceResolver;
import com.niixlabs.unwind.api.MutationRecord;
import net.minecraft.core.BlockPos;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class MutationLineageTracker {

    private static final int MAX_GLOBAL_MUTATIONS = 200;
    private static final int MAX_PER_POS_HISTORY = 8;

    private static final Map<Long, Deque<MutationRecord>> POS_LINEAGE = new ConcurrentHashMap<>();
    private static final Deque<MutationRecord> GLOBAL_RECENT = new ArrayDeque<>(MAX_GLOBAL_MUTATIONS);

    private MutationLineageTracker() {}

    public static void recordBlockMutation(
            long gameTick,
            String dimension,
            BlockPos pos,
            String previousState,
            String newState,
            int flags
    ) {
        StackWalker.StackFrame externalCaller = ModProvenanceResolver.findFirstExternalModFrame();
        StackWalker.StackFrame immediateCaller = ModProvenanceResolver.findImmediateCallerFrame();
        StackWalker.StackFrame chosenCaller = externalCaller != null ? externalCaller : immediateCaller;

        String modId = chosenCaller != null
                ? ModProvenanceResolver.resolveModId(chosenCaller.getDeclaringClass())
                : "minecraft";
        String formattedFrame = ModProvenanceResolver.formatFrameWithMod(chosenCaller);
        String asyncOrigin = AsyncCausalityTracker.getCurrentTaskOrigin();

        MutationRecord record = new MutationRecord(
                System.currentTimeMillis(),
                gameTick,
                dimension,
                pos.toShortString(),
                previousState,
                newState,
                flags,
                modId,
                formattedFrame,
                asyncOrigin
        );

        long posKey = pos.asLong();
        POS_LINEAGE.compute(posKey, (k, deque) -> {
            if (deque == null) deque = new ArrayDeque<>(MAX_PER_POS_HISTORY);
            if (deque.size() >= MAX_PER_POS_HISTORY) deque.pollFirst();
            deque.addLast(record);
            return deque;
        });

        synchronized (GLOBAL_RECENT) {
            if (GLOBAL_RECENT.size() >= MAX_GLOBAL_MUTATIONS) {
                GLOBAL_RECENT.pollFirst();
            }
            GLOBAL_RECENT.addLast(record);
        }
    }

    public static List<MutationRecord> getLineageForPos(BlockPos pos) {
        Deque<MutationRecord> deque = POS_LINEAGE.get(pos.asLong());
        return deque != null ? new ArrayList<>(deque) : List.of();
    }

    public static List<MutationRecord> getRecentMutations(int limit) {
        synchronized (GLOBAL_RECENT) {
            List<MutationRecord> all = new ArrayList<>(GLOBAL_RECENT);
            int start = Math.max(0, all.size() - limit);
            return all.subList(start, all.size());
        }
    }
}