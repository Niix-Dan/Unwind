package com.niixlabs.unwind.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.niixlabs.unwind.core.FrameCaptureContext;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.LinkedHashMap;
import java.util.Map;

@Mixin(EntityRenderDispatcher.class)
public abstract class MixinEntityRenderDispatcher {

    @Shadow public abstract <T extends Entity> EntityRenderer<? super T> getRenderer(T entity);

    @Inject(method = "render", at = @At("HEAD"))
    private <E extends Entity> void unwind$onRenderEntityHead(
            E entity, double x, double y, double z, float rotationYaw, float partialTicks,
            PoseStack poseStack, MultiBufferSource buffer, int packedLight, CallbackInfo ci
    ) {
        EntityRenderer<? super E> renderer = this.getRenderer(entity);
        Class<?> ownerClass = renderer != null ? renderer.getClass() : entity.getClass();

        Map<String, String> args = new LinkedHashMap<>();
        args.put("EntityType", BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).toString());
        args.put("EntityClass", entity.getClass().getName());
        args.put("RendererClass", renderer != null ? renderer.getClass().getName() : "null");
        args.put("EntityPos", entity.position().toString());
        args.put("RenderOffset", String.format("(%.2f, %.2f, %.2f)", x, y, z));
        args.put("PartialTicks", String.valueOf(partialTicks));
        args.put("PackedLight", String.valueOf(packedLight));

        FrameCaptureContext.pushFrame(
                entity.level().getGameTime(),
                "CLIENT_ENTITY_RENDER",
                ownerClass.getName() + ".render",
                "CLIENT",
                ownerClass,
                args
        );
    }

    @Inject(method = "render", at = @At("RETURN"))
    private <E extends Entity> void unwind$onRenderEntityReturn(
            E entity, double x, double y, double z, float rotationYaw, float partialTicks,
            PoseStack poseStack, MultiBufferSource buffer, int packedLight, CallbackInfo ci
    ) {
        FrameCaptureContext.popFrame();
    }
}