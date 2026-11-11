package com.niixlabs.unwind.analysis;

import net.neoforged.fml.loading.FMLPaths;

import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

public final class PrivacyScrubber {

    private static final Map<String, String> EXACT_REPLACEMENTS = new LinkedHashMap<>();

    static {
        registerPath(FMLPaths.GAMEDIR.get(), "<GAME_DIR>");
        registerPath(System.getProperty("user.home"), "<USER_HOME>");
        registerPath(System.getProperty("java.io.tmpdir"), "<TEMP_DIR>");

        registerLiteral(System.getProperty("user.name"), "<OS_USER>");
        registerLiteral(System.getenv("COMPUTERNAME"), "<PC_NAME>");
        registerLiteral(System.getenv("HOSTNAME"), "<PC_NAME>");
    }

    private PrivacyScrubber() {}

    public static String scrub(String rawText) {
        if (rawText == null || rawText.isEmpty()) return "";
        String cleaned = rawText;
        for (Map.Entry<String, String> entry : EXACT_REPLACEMENTS.entrySet()) {
            cleaned = cleaned.replace(entry.getKey(), entry.getValue());
        }
        return cleaned;
    }

    private static void registerPath(Path path, String placeholder) {
        if (path != null) {
            registerPath(path.toAbsolutePath().toString(), placeholder);
        }
    }

    private static void registerPath(String rawPath, String placeholder) {
        if (rawPath == null || rawPath.isBlank()) return;
        String normalized = rawPath.endsWith("\\") || rawPath.endsWith("/")
                ? rawPath.substring(0, rawPath.length() - 1)
                : rawPath;

        EXACT_REPLACEMENTS.put(normalized.replace('/', '\\'), placeholder);
        EXACT_REPLACEMENTS.put(normalized.replace('\\', '/'), placeholder);
    }

    private static void registerLiteral(String secret, String placeholder) {
        if (secret != null && secret.trim().length() >= 3) {
            EXACT_REPLACEMENTS.put(secret.trim(), placeholder);
        }
    }
}