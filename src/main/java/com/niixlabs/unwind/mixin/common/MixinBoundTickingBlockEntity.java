package com.niixlabs.unwind.mixin.common;

import com.niixlabs.unwind.core.FlightRecorder;
import com.niixlabs.unwind.core.FrameCaptureContext;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.LinkedHashMap;
import java.util.Map;

@Mixin(targets = "net.minecraft.world.level.chunk.LevelChunk$BoundTickingBlockEntity")
public class MixinBoundTickingBlockEntity<T extends BlockEntity> {

    @Shadow @Final private T blockEntity;

    @Inject(method = "tick", at = @At("HEAD"))
    private void unwind$onBlockEntityTickHead(CallbackInfo ci) {
        if (this.blockEntity == null) return;
        Level level = this.blockEntity.getLevel();
        long gameTick = level != null ? level.getGameTime() : -1L;
        boolean isClient = level != null && level.isClientSide();
        BlockPos pos = this.blockEntity.getBlockPos();
        BlockState cachedState = this.blockEntity.getBlockState();
        BlockState liveWorldState = level != null ? level.getBlockState(pos) : null;
        String beType = BuiltInRegistries.BLOCK_ENTITY_TYPE.getKey(this.blockEntity.getType()) != null
                ? BuiltInRegistries.BLOCK_ENTITY_TYPE.getKey(this.blockEntity.getType()).toString()
                : "unregistered";

        Map<String, String> args = new LinkedHashMap<>();
        args.put("BlockEntityClass", this.blockEntity.getClass().getName());
        args.put("BlockEntityType", beType);
        args.put("BlockPos", pos.toShortString());
        args.put("CachedBlockState", String.valueOf(cachedState));
        args.put("LiveWorldBlockState", String.valueOf(liveWorldState));
        args.put("StateMismatch", String.valueOf(liveWorldState != null && cachedState != liveWorldState));
        args.put("IsRemoved", String.valueOf(this.blockEntity.isRemoved()));
        args.put("Dimension", level != null ? level.dimension().location().toString() : "null");

        FlightRecorder.pushTransientScope(
                gameTick,
                isClient,
                "BLOCK_ENTITY_TICK",
                "Ticking BlockEntity " + beType + " at [" + pos.toShortString() + "]"
        );

        FrameCaptureContext.pushFrame(
                gameTick,
                "BLOCK_ENTITY_TICK",
                this.blockEntity.getClass().getName() + ".tick",
                isClient ? "CLIENT" : "SERVER",
                this.blockEntity.getClass(),
                args
        );
    }

    @Inject(method = "tick", at = @At("RETURN"))
    private void unwind$onBlockEntityTickReturn(CallbackInfo ci) {
        if (this.blockEntity == null) return;
        FrameCaptureContext.popFrame();
        FlightRecorder.popScope();
    }
}