package com.niixlabs.unwind.analysis;

import net.neoforged.fml.ModList;
import net.neoforged.neoforgespi.language.IModInfo;

import java.nio.file.Path;
import java.util.Optional;

public final class ModProvenanceResolver {

    private static final StackWalker STACK_WALKER = StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE);

    private ModProvenanceResolver() {}

    public static String resolveModId(Class<?> clazz) {
        if (clazz == null) return "unknown";
        Module module = clazz.getModule();
        if (module != null && module.isNamed()) {
            String moduleName = module.getName();
            if ("minecraft".equals(moduleName) || moduleName.startsWith("java.") || moduleName.startsWith("jdk.")) {
                return moduleName;
            }
            if (ModList.get() != null && ModList.get().isLoaded(moduleName)) {
                return moduleName;
            }
            return moduleName;
        }
        return "unnamed_or_asm";
    }

    public static String resolveModVersion(String modId) {
        if (ModList.get() == null) return "N/A";
        return ModList.get().getModContainerById(modId)
                .map(c -> c.getModInfo().getVersion().toString())
                .orElse("N/A");
    }

    public static String resolveModJar(String modId) {
        if (ModList.get() == null) return "unknown.jar";
        return ModList.get().getModContainerById(modId)
                .map(container -> {
                    IModInfo info = container.getModInfo();
                    Path jarPath = info.getOwningFile().getFile().getFilePath();
                    return jarPath != null ? jarPath.getFileName().toString() : info.getModId();
                })
                .orElse("runtime/builtin");
    }

    public static StackWalker.StackFrame findFirstExternalModFrame() {
        Optional<StackWalker.StackFrame> frame = STACK_WALKER.walk(stream -> stream
                .filter(f -> !isInternalOrEngineFrame(f))
                .filter(f -> !f.getClassName().startsWith("net.minecraft."))
                .filter(f -> !f.getClassName().startsWith("com.mojang."))
                .findFirst()
        );
        return frame.orElse(null);
    }

    public static StackWalker.StackFrame findImmediateCallerFrame() {
        Optional<StackWalker.StackFrame> frame = STACK_WALKER.walk(stream -> stream
                .filter(f -> !isInternalOrEngineFrame(f))
                .findFirst()
        );
        return frame.orElse(null);
    }

    private static boolean isInternalOrEngineFrame(StackWalker.StackFrame f) {
        String cn = f.getClassName();
        String mn = f.getMethodName();
        return cn.startsWith("com.niixlabs.unwind")
                || mn.contains("$unwind$")
                || cn.startsWith("java.")
                || cn.startsWith("jdk.")
                || cn.startsWith("sun.")
                || cn.startsWith("net.neoforged.")
                || cn.startsWith("cpw.mods.")
                || cn.startsWith("org.spongepowered.")
                || cn.startsWith("io.netty.")
                || cn.startsWith("com.google.")
                || cn.startsWith("it.unimi.dsi.");
    }

    public static String formatFrameWithMod(StackWalker.StackFrame frame) {
        if (frame == null) return "vanilla_direct";
        String modId = resolveModId(frame.getDeclaringClass());
        String jar = resolveModJar(modId);
        return String.format("%s.%s:%d [%s | %s]",
                frame.getClassName(), frame.getMethodName(), frame.getLineNumber(), modId, jar);
    }
}