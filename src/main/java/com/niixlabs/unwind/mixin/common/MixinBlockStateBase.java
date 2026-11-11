package com.niixlabs.unwind.mixin.common;

import com.niixlabs.unwind.core.FlightRecorder;
import com.niixlabs.unwind.core.FrameCaptureContext;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.LinkedHashMap;
import java.util.Map;

@Mixin(BlockBehaviour.BlockStateBase.class)
public abstract class MixinBlockStateBase {

    @Shadow public abstract Block getBlock();

    @Inject(method = "useItemOn", at = @At("HEAD"))
    private void unwind$onUseItemOnHead(ItemStack stack, Level level, Player player, InteractionHand hand, BlockHitResult hitResult, CallbackInfoReturnable<ItemInteractionResult> cir) {
        Block block = this.getBlock();
        String blockId = BuiltInRegistries.BLOCK.getKey(block).toString();
        String itemId = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
        BlockPos pos = hitResult.getBlockPos();

        Map<String, String> args = new LinkedHashMap<>();
        args.put("TargetBlock", blockId + " (" + block.getClass().getName() + ")");
        args.put("BlockState", this.toString());
        args.put("BlockPos", pos.toShortString());
        args.put("HitDirection", hitResult.getDirection().name());
        args.put("Player", player.getName().getString() + " (" + player.getClass().getSimpleName() + ")");
        args.put("Hand", hand.name());
        args.put("HeldStack", stack.getCount() + "x " + itemId);
        args.put("HeldComponents", stack.getComponents().toString());
        args.put("Level", level.getClass().getSimpleName() + " [" + level.dimension().location() + "]");

        FlightRecorder.pushScope(
                level.getGameTime(),
                level.isClientSide(),
                "BLOCK_USE_ITEM_ON",
                "Player '" + player.getName().getString() + "' used '" + itemId + "' on block '" + blockId + "' at [" + pos.toShortString() + "]"
        );

        FrameCaptureContext.pushFrame(
                level.getGameTime(),
                "BLOCK_USE_ITEM_ON",
                block.getClass().getName() + ".useItemOn",
                level.isClientSide() ? "CLIENT" : "SERVER",
                block.getClass(),
                args
        );
    }

    @Inject(method = "useItemOn", at = @At("RETURN"))
    private void unwind$onUseItemOnReturn(ItemStack stack, Level level, Player player, InteractionHand hand, BlockHitResult hitResult, CallbackInfoReturnable<ItemInteractionResult> cir) {
        FrameCaptureContext.popFrame();
        FlightRecorder.popScope();
    }

    @Inject(method = "useWithoutItem", at = @At("HEAD"))
    private void unwind$onUseWithoutItemHead(Level level, Player player, BlockHitResult hitResult, CallbackInfoReturnable<InteractionResult> cir) {
        Block block = this.getBlock();
        String blockId = BuiltInRegistries.BLOCK.getKey(block).toString();
        BlockPos pos = hitResult.getBlockPos();

        Map<String, String> args = new LinkedHashMap<>();
        args.put("TargetBlock", blockId + " (" + block.getClass().getName() + ")");
        args.put("BlockState", this.toString());
        args.put("BlockPos", pos.toShortString());
        args.put("HitDirection", hitResult.getDirection().name());
        args.put("Player", player.getName().getString() + " (" + player.getClass().getSimpleName() + ")");
        args.put("Level", level.getClass().getSimpleName() + " [" + level.dimension().location() + "]");

        FlightRecorder.pushScope(
                level.getGameTime(),
                level.isClientSide(),
                "BLOCK_USE_EMPTY",
                "Player '" + player.getName().getString() + "' interacted with block '" + blockId + "' at [" + pos.toShortString() + "]"
        );

        FrameCaptureContext.pushFrame(
                level.getGameTime(),
                "BLOCK_USE_EMPTY",
                block.getClass().getName() + ".useWithoutItem",
                level.isClientSide() ? "CLIENT" : "SERVER",
                block.getClass(),
                args
        );
    }

    @Inject(method = "useWithoutItem", at = @At("RETURN"))
    private void unwind$onUseWithoutItemReturn(Level level, Player player, BlockHitResult hitResult, CallbackInfoReturnable<InteractionResult> cir) {
        FrameCaptureContext.popFrame();
        FlightRecorder.popScope();
    }

    @Inject(method = "handleNeighborChanged", at = @At("HEAD"))
    private void unwind$onNeighborChangedHead(Level level, BlockPos pos, Block neighborBlock, BlockPos neighborPos, boolean movedByPiston, CallbackInfo ci) {
        Block block = this.getBlock();
        Map<String, String> args = new LinkedHashMap<>();
        args.put("TargetBlock", BuiltInRegistries.BLOCK.getKey(block) + " at [" + pos.toShortString() + "]");
        args.put("TargetBlockState", this.toString());
        args.put("SourceNeighborBlock", BuiltInRegistries.BLOCK.getKey(neighborBlock) + " at [" + neighborPos.toShortString() + "]");
        args.put("MovedByPiston", String.valueOf(movedByPiston));
        args.put("Dimension", level.dimension().location().toString());

        FrameCaptureContext.pushFrame(
                level.getGameTime(),
                "NEIGHBOR_CHANGED",
                block.getClass().getName() + ".neighborChanged",
                level.isClientSide() ? "CLIENT" : "SERVER",
                block.getClass(),
                args
        );
    }

    @Inject(method = "handleNeighborChanged", at = @At("RETURN"))
    private void unwind$onNeighborChangedReturn(Level level, BlockPos pos, Block neighborBlock, BlockPos neighborPos, boolean movedByPiston, CallbackInfo ci) {
        FrameCaptureContext.popFrame();
    }

    @Inject(method = "tick", at = @At("HEAD"))
    private void unwind$onScheduledTickHead(ServerLevel level, BlockPos pos, RandomSource random, CallbackInfo ci) {
        Block block = this.getBlock();
        Map<String, String> args = new LinkedHashMap<>();
        args.put("Block", BuiltInRegistries.BLOCK.getKey(block).toString());
        args.put("BlockState", this.toString());
        args.put("BlockPos", pos.toShortString());
        args.put("Dimension", level.dimension().location().toString());

        FrameCaptureContext.pushFrame(
                level.getGameTime(),
                "BLOCK_SCHEDULED_TICK",
                block.getClass().getName() + ".tick",
                "SERVER",
                block.getClass(),
                args
        );
    }

    @Inject(method = "tick", at = @At("RETURN"))
    private void unwind$onScheduledTickReturn(ServerLevel level, BlockPos pos, RandomSource random, CallbackInfo ci) {
        FrameCaptureContext.popFrame();
    }
}