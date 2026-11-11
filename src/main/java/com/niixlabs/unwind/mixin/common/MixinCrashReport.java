package com.niixlabs.unwind.mixin.common;

import com.niixlabs.unwind.api.ActionRecord;
import com.niixlabs.unwind.api.CapturedFrameState;
import com.niixlabs.unwind.core.FlightRecorder;
import com.niixlabs.unwind.core.FrameCaptureContext;
import com.niixlabs.unwind.report.DiagnosticReportWriter;
import net.minecraft.CrashReport;
import net.minecraft.ReportType;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.nio.file.Path;
import java.util.List;

@Mixin(CrashReport.class)
public class MixinCrashReport {

    @Shadow @Final private String title;
    @Shadow @Final private Throwable exception;

    @Unique private Thread unwind$crashingThread;
    @Unique private List<ActionRecord> unwind$capturedScopes;
    @Unique private List<CapturedFrameState> unwind$capturedFrames;
    @Unique private boolean unwind$exported = false;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void unwind$onConstruct(String title, Throwable exception, CallbackInfo ci) {
        this.unwind$crashingThread = Thread.currentThread();
        this.unwind$capturedScopes = FlightRecorder.getActiveScopesForCurrentThread();
        this.unwind$capturedFrames = FrameCaptureContext.snapshotActiveFrames();
    }

    @Inject(method = "saveToFile(Ljava/nio/file/Path;Lnet/minecraft/ReportType;Ljava/util/List;)Z", at = @At("RETURN"))
    private void unwind$onSaveToFile(Path path, ReportType type, List<String> links, CallbackInfoReturnable<Boolean> cir) {
        if (!this.unwind$exported) {
            this.unwind$exported = true;
            DiagnosticReportWriter.writeStandaloneDiagnosticReport(
                    this.title,
                    this.exception,
                    this.unwind$crashingThread != null ? this.unwind$crashingThread : Thread.currentThread(),
                    this.unwind$capturedScopes != null ? this.unwind$capturedScopes : List.of(),
                    this.unwind$capturedFrames != null ? this.unwind$capturedFrames : List.of()
            );
        }
    }
}