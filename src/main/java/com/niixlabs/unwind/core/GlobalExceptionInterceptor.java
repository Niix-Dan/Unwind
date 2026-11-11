package com.niixlabs.unwind.core;

import com.niixlabs.unwind.report.DiagnosticReportWriter;

public final class GlobalExceptionInterceptor {

    private static boolean installed = false;

    private GlobalExceptionInterceptor() {}

    public static synchronized void install() {
        if (installed) return;
        installed = true;

        Thread.UncaughtExceptionHandler previousHandler = Thread.getDefaultUncaughtExceptionHandler();
        Thread.setDefaultUncaughtExceptionHandler((thread, throwable) -> {
            try {
                DiagnosticReportWriter.writeStandaloneDiagnosticReport(
                        "Uncaught Thread Exception (" + thread.getName() + ")",
                        throwable,
                        thread,
                        FlightRecorder.getActiveScopesForCurrentThread(),
                        FrameCaptureContext.snapshotActiveFrames()
                );
            } catch (Throwable ignored) {
            } finally {
                if (previousHandler != null) {
                    previousHandler.uncaughtException(thread, throwable);
                }
            }
        });
    }
}