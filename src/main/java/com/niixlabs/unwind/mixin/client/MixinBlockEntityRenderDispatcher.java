package com.niixlabs.unwind.mixin.client;

import com.niixlabs.unwind.core.FrameCaptureContext;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.LinkedHashMap;
import java.util.Map;

@Mixin(BlockEntityRenderDispatcher.class)
public class MixinBlockEntityRenderDispatcher {

    @Inject(method = "tryRender", at = @At("HEAD"))
    private static void unwind$onTryRenderHead(BlockEntity blockEntity, Runnable rendererCall, CallbackInfo ci) {
        String typeId = BuiltInRegistries.BLOCK_ENTITY_TYPE.getKey(blockEntity.getType()) != null
                ? BuiltInRegistries.BLOCK_ENTITY_TYPE.getKey(blockEntity.getType()).toString()
                : "unregistered";

        Map<String, String> args = new LinkedHashMap<>();
        args.put("BlockEntityType", typeId);
        args.put("BlockEntityClass", blockEntity.getClass().getName());
        args.put("BlockPos", blockEntity.getBlockPos().toShortString());
        args.put("BlockState", blockEntity.getBlockState().toString());
        args.put("IsRemoved", String.valueOf(blockEntity.isRemoved()));

        FrameCaptureContext.pushFrame(
                blockEntity.getLevel() != null ? blockEntity.getLevel().getGameTime() : -1L,
                "CLIENT_BLOCK_ENTITY_RENDER",
                blockEntity.getClass().getName() + ".render",
                "CLIENT",
                blockEntity.getClass(),
                args
        );
    }

    @Inject(method = "tryRender", at = @At("RETURN"))
    private static void unwind$onTryRenderReturn(BlockEntity blockEntity, Runnable rendererCall, CallbackInfo ci) {
        FrameCaptureContext.popFrame();
    }
}