package com.niixlabs.unwind.logger;

import com.niixlabs.unwind.analysis.ModProvenanceResolver;
import com.niixlabs.unwind.analysis.PrivacyScrubber;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.LoggerConfig;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public final class LiveBreakpointAppender extends AbstractAppender {

    private static final StackWalker WALKER = StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE);
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm:ss.SSS");
    private final Path traceLogFile;
    private final Map<String, AtomicInteger> deduplicationCounter = new ConcurrentHashMap<>();

    private LiveBreakpointAppender(Path traceLogFile) {
        super("UnwindLiveBreakpointAppender", null, null, true, null);
        this.traceLogFile = traceLogFile;
    }

    public static void install(Path diagnosticsDir) {
        try {
            Files.createDirectories(diagnosticsDir);
            Path logFile = diagnosticsDir.resolve("live-warn-error-traces.log");
            Files.writeString(logFile, "=== UNWIND LIVE WARN/ERROR CALL STACK PROVENANCE ===\n\n");

            LoggerContext ctx = (LoggerContext) LogManager.getContext(false);
            Configuration config = ctx.getConfiguration();

            LiveBreakpointAppender appender = new LiveBreakpointAppender(logFile);
            appender.start();
            config.addAppender(appender);

            LoggerConfig rootLogger = config.getRootLogger();
            rootLogger.addAppender(appender, Level.WARN, null);
            ctx.updateLoggers();
        } catch (IOException ignored) {}
    }

    @Override
    public void append(LogEvent event) {
        if (event.getLevel().isLessSpecificThan(Level.WARN)) return;

        String rawMessage = event.getMessage().getFormattedMessage();
        String dedupKey = event.getLoggerName() + "|" + rawMessage;
        int count = deduplicationCounter.computeIfAbsent(dedupKey, k -> new AtomicInteger(0)).incrementAndGet();

        if (count > 5) return;

        List<String> frames = WALKER.walk(stream -> stream
                .dropWhile(f -> f.getClassName().startsWith("org.apache.logging")
                        || f.getClassName().startsWith("org.slf4j")
                        || f.getClassName().startsWith("java.lang.reflect")
                        || f.getClassName().startsWith("com.niixlabs.unwind"))
                .limit(8)
                .map(ModProvenanceResolver::formatFrameWithMod)
                .toList()
        );

        StringBuilder sb = new StringBuilder(512);
        sb.append(String.format("[%s] [%s] [Thread: %s] [Logger: %s] [Occurrence #%d]\n",
                LocalTime.now().format(TIME_FMT),
                event.getLevel().name(),
                event.getThreadName(),
                event.getLoggerName(),
                count
        ));
        sb.append("  Message : ").append(PrivacyScrubber.scrub(rawMessage)).append("\n");
        sb.append("  Callers :\n");
        for (int i = 0; i < frames.size(); i++) {
            sb.append("    [").append(i).append("] ").append(frames.get(i)).append("\n");
        }
        sb.append("\n");

        try {
            Files.writeString(traceLogFile, sb.toString(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException ignored) {}
    }
}