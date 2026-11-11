package com.niixlabs.unwind.mixin.common;

import com.niixlabs.unwind.core.FlightRecorder;
import com.niixlabs.unwind.core.FrameCaptureContext;
import com.niixlabs.unwind.core.MutationLineageTracker;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;

@Mixin(Level.class)
public abstract class MixinLevel {

    @Shadow public abstract BlockState getBlockState(BlockPos pos);
    @Shadow public abstract long getGameTime();
    @Shadow public abstract ResourceKey<Level> dimension();
    @Shadow public abstract boolean isClientSide();

    @Inject(method = "setBlock(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;II)Z", at = @At("HEAD"))
    private void unwind$onSetBlockHead(BlockPos pos, BlockState newState, int flags, int recursionLeft, CallbackInfoReturnable<Boolean> cir) {
        BlockState oldState = this.getBlockState(pos);
        String dim = this.dimension().location().toString();

        if (oldState != newState) {
            MutationLineageTracker.recordBlockMutation(
                    this.getGameTime(),
                    dim,
                    pos,
                    oldState.toString(),
                    newState.toString(),
                    flags
            );
        }

        Map<String, String> args = new LinkedHashMap<>();
        args.put("arg0 (BlockPos)", pos.toShortString());
        args.put("arg1 (NewBlockState)", newState.toString());
        args.put("CurrentBlockState", oldState.toString());
        args.put("arg2 (Flags)", String.valueOf(flags));
        args.put("arg3 (RecursionLeft)", String.valueOf(recursionLeft));
        args.put("Dimension", dim);

        FrameCaptureContext.pushFrame(
                this.getGameTime(),
                "LEVEL_SET_BLOCK",
                "Level.setBlock(BlockPos, BlockState, int, int)",
                this.isClientSide() ? "CLIENT" : "SERVER",
                newState.getBlock().getClass(),
                args
        );
    }

    @Inject(method = "setBlock(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;II)Z", at = @At("RETURN"))
    private void unwind$onSetBlockReturn(BlockPos pos, BlockState newState, int flags, int recursionLeft, CallbackInfoReturnable<Boolean> cir) {
        FrameCaptureContext.popFrame();
    }

    @Inject(method = "guardEntityTick", at = @At("HEAD"))
    private <T extends Entity> void unwind$onEntityTickHead(Consumer<T> consumer, T entity, CallbackInfo ci) {
        String entityType = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).toString();
        Map<String, String> args = new LinkedHashMap<>();
        args.put("EntityClass", entity.getClass().getName());
        args.put("EntityType", entityType);
        args.put("EntityId", String.valueOf(entity.getId()));
        args.put("EntityUUID", entity.getUUID().toString());
        args.put("Position", entity.position().toString());
        args.put("BlockPos", entity.blockPosition().toShortString());
        args.put("DeltaMovement", entity.getDeltaMovement().toString());
        args.put("IsRemoved", String.valueOf(entity.isRemoved()));
        args.put("Dimension", this.dimension().location().toString());

        FlightRecorder.pushTransientScope(
                this.getGameTime(),
                this.isClientSide(),
                "ENTITY_TICK",
                "Ticking Entity " + entityType + " (id=" + entity.getId() + ") at [" + entity.blockPosition().toShortString() + "]"
        );

        FrameCaptureContext.pushFrame(
                this.getGameTime(),
                "ENTITY_TICK",
                entity.getClass().getName() + ".tick()",
                this.isClientSide() ? "CLIENT" : "SERVER",
                entity.getClass(),
                args
        );
    }

    @Inject(method = "guardEntityTick", at = @At("RETURN"))
    private <T extends Entity> void unwind$onEntityTickReturn(Consumer<T> consumer, T entity, CallbackInfo ci) {
        FrameCaptureContext.popFrame();
        FlightRecorder.popScope();
    }
}