package com.niixlabs.unwind.report;

import com.niixlabs.unwind.analysis.PrivacyScrubber;
import com.niixlabs.unwind.analysis.StackTraceInspector;
import com.niixlabs.unwind.api.ActionRecord;
import com.niixlabs.unwind.api.CapturedFrameState;
import com.niixlabs.unwind.api.MixinFrameMetadata;
import com.niixlabs.unwind.api.MutationRecord;
import com.niixlabs.unwind.api.StackFrameProvenance;
import com.niixlabs.unwind.core.AsyncCausalityTracker;
import com.niixlabs.unwind.core.FlightRecorder;
import com.niixlabs.unwind.core.MutationLineageTracker;
import net.neoforged.fml.loading.FMLPaths;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

public final class DiagnosticReportWriter {

    private static final DateTimeFormatter FILE_TIMESTAMP = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH.mm.ss.SSS");

    private DiagnosticReportWriter() {}

    public static Path writeStandaloneDiagnosticReport(
            String crashDescription,
            Throwable exception,
            Thread crashingThread,
            List<ActionRecord> activeScopesAtCrash,
            List<CapturedFrameState> capturedFramesAtCrash
    ) {
        try {
            Path outDir = FMLPaths.GAMEDIR.get().resolve("crash-reports/unwind");
            Files.createDirectories(outDir);

            String fileName = "unwind-" + LocalDateTime.now().format(FILE_TIMESTAMP) + ".txt";
            Path reportPath = outDir.resolve(fileName);

            MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
            long heapUsedMb = memoryBean.getHeapMemoryUsage().getUsed() / (1024 * 1024);
            long heapMaxMb = memoryBean.getHeapMemoryUsage().getMax() / (1024 * 1024);

            StringBuilder sb = new StringBuilder(16384);
            sb.append("================================================================================\n");
            sb.append("  UNWIND RUNTIME EXECUTION & STATE PROVENANCE DUMP (MC 1.21.1 / NEOFORGE)\n");
            sb.append("================================================================================\n");
            sb.append("Timestamp        : ").append(LocalDateTime.now()).append("\n");
            sb.append("Report Context   : ").append(crashDescription).append("\n");
            sb.append("Exception Class  : ").append(exception != null ? exception.getClass().getName() : "N/A").append("\n");
            sb.append("Exception Message: ").append(exception != null ? exception.getMessage() : "N/A").append("\n");
            sb.append("Crashing Thread  : ").append(crashingThread.getName())
                    .append(" (ID=").append(crashingThread.threadId())
                    .append(", State=").append(crashingThread.getState()).append(")\n");
            sb.append("Task Causality   : ").append(AsyncCausalityTracker.getCurrentTaskOrigin()).append("\n");
            sb.append("JVM Heap Usage   : ").append(heapUsedMb).append(" MB / ").append(heapMaxMb).append(" MB\n");
            sb.append("================================================================================\n\n");

            sb.append("[1] PRE-UNWIND INSTRUMENTED CALL STACK & LOCAL STATE SNAPSHOT\n");
            sb.append("--------------------------------------------------------------------------------\n");
            if (capturedFramesAtCrash.isEmpty()) {
                sb.append("  (No instrumented frames remained on the unwinding call stack)\n");
            } else {
                for (int i = 0; i < capturedFramesAtCrash.size(); i++) {
                    CapturedFrameState frame = capturedFramesAtCrash.get(i);
                    sb.append(String.format("  UnwindFrame[%d] Phase=%s | Side=%s | Tick=%d\n",
                            i, frame.phase(), frame.logicalSide(), frame.gameTick()));
                    sb.append(String.format("    Target : %s\n", frame.targetMethod()));
                    sb.append(String.format("    Owner  : Mod=%s | Jar=%s\n", frame.modId(), frame.modJar()));
                    for (Map.Entry<String, String> arg : frame.capturedArguments().entrySet()) {
                        sb.append("    ├─ ").append(arg.getKey()).append(" = ").append(arg.getValue()).append("\n");
                    }
                }
            }
            sb.append("\n");

            sb.append("[2] ACTIVE EXECUTION SCOPES ON CRASHING THREAD\n");
            sb.append("--------------------------------------------------------------------------------\n");
            if (activeScopesAtCrash.isEmpty()) {
                sb.append("  (Empty)\n");
            } else {
                for (int i = 0; i < activeScopesAtCrash.size(); i++) {
                    sb.append("  Scope[").append(i).append("] ").append(activeScopesAtCrash.get(i)).append("\n");
                }
            }
            sb.append("\n");

            sb.append("[3] SYNTHETIC MIXIN HANDLER DECOMPOSITION\n");
            sb.append("--------------------------------------------------------------------------------\n");
            List<MixinFrameMetadata> mixinFrames = StackTraceInspector.extractMixinFrames(exception);
            if (mixinFrames.isEmpty()) {
                sb.append("  (No synthetic Mixin frames present in exception stack)\n");
            } else {
                for (MixinFrameMetadata mf : mixinFrames) {
                    sb.append(String.format(
                            "  Frame[%02d] Injector=@%s | ModId=%s | HandlerId=%s | Method=%s | TargetClass=%s (Line %d)\n",
                            mf.frameIndex(), mf.injectorType(), mf.sourceModId(), mf.handlerId(),
                            mf.sourceMethodName(), mf.targetClass(), mf.lineNumber()
                    ));
                }
            }
            sb.append("\n");

            sb.append("[4] STACK FRAME MODULE, JAR & CLASSLOADER PROVENANCE\n");
            sb.append("--------------------------------------------------------------------------------\n");
            List<StackFrameProvenance> provenanceChain = StackTraceInspector.extractProvenanceChain(exception);
            for (StackFrameProvenance p : provenanceChain) {
                sb.append(String.format(
                        "  [%02d] %s.%s(%s:%d)\n       └─ Module=%s | ModId=%s (v%s) | Jar=%s | ClassLoader=%s\n",
                        p.index(), p.className(), p.methodName(), p.fileName(), p.lineNumber(),
                        p.moduleName(), p.modId(), p.modVersion(), p.jarFileName(), p.classLoaderName()
                ));
            }
            sb.append("\n");

            sb.append("[5] WORLD BLOCKPOS MUTATION LINEAGE (LAST 30 MUTATIONS)\n");
            sb.append("--------------------------------------------------------------------------------\n");
            List<MutationRecord> mutations = MutationLineageTracker.getRecentMutations(30);
            if (mutations.isEmpty()) {
                sb.append("  (No block mutations recorded)\n");
            } else {
                for (MutationRecord m : mutations) {
                    sb.append(String.format("  [Tick %d | %s] Pos=[%s] Flags=%d | %s -> %s\n",
                            m.gameTick(), m.dimension(), m.targetPos(), m.flags(), m.previousState(), m.newState()));
                    sb.append("    ├─ Caller : ").append(m.callerFrame()).append("\n");
                    sb.append("    └─ Origin : ").append(m.asyncOrigin()).append("\n");
                }
            }
            sb.append("\n");

            sb.append("[6] FLIGHT RECORDER RING BUFFER (LAST 150 GAMEPLAY & ENGINE EVENTS)\n");
            sb.append("--------------------------------------------------------------------------------\n");
            for (ActionRecord rec : FlightRecorder.getRecentHistorySnapshot()) {
                sb.append("  ").append(rec.toString()).append("\n");
            }
            sb.append("\n");

            sb.append("[7] RAW UNMODIFIED JAVA STACK TRACE\n");
            sb.append("--------------------------------------------------------------------------------\n");
            if (exception != null) {
                StringWriter sw = new StringWriter();
                exception.printStackTrace(new PrintWriter(sw));
                sb.append(sw);
            }

            Files.writeString(reportPath, PrivacyScrubber.scrub(sb.toString()));
            return reportPath;
        } catch (Throwable e) {
            return null;
        }
    }
}