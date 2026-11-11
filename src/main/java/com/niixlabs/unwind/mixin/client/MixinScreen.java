package com.niixlabs.unwind.mixin.client;

import com.niixlabs.unwind.core.FrameCaptureContext;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.LinkedHashMap;
import java.util.Map;

@Mixin(Screen.class)
public class MixinScreen {

    @Shadow @Final protected Component title;
    @Shadow public int width;
    @Shadow public int height;

    @Inject(method = "renderWithTooltip", at = @At("HEAD"))
    private void unwind$onRenderScreenHead(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
        Map<String, String> args = new LinkedHashMap<>();
        args.put("ScreenClass", this.getClass().getName());
        args.put("ScreenTitle", this.title != null ? this.title.getString() : "null");
        args.put("Dimensions", this.width + "x" + this.height);
        args.put("MousePos", "(" + mouseX + ", " + mouseY + ")");
        args.put("PartialTick", String.valueOf(partialTick));

        FrameCaptureContext.pushFrame(
                -1L,
                "CLIENT_SCREEN_RENDER",
                this.getClass().getName() + ".renderWithTooltip(GuiGraphics, int, int, float)",
                "CLIENT",
                this.getClass(),
                args
        );
    }

    @Inject(method = "renderWithTooltip", at = @At("RETURN"))
    private void unwind$onRenderScreenReturn(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
        FrameCaptureContext.popFrame();
    }
}