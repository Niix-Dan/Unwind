package com.niixlabs.unwind.mixin.common;

import com.niixlabs.unwind.core.FlightRecorder;
import com.niixlabs.unwind.core.FrameCaptureContext;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.LinkedHashMap;
import java.util.Map;

@Mixin(ItemStack.class)
public abstract class MixinItemStack {

    @Shadow public abstract Item getItem();

    @Inject(method = "use", at = @At("HEAD"))
    private void unwind$onUseHead(Level level, Player player, InteractionHand hand, CallbackInfoReturnable<InteractionResultHolder<ItemStack>> cir) {
        ItemStack self = (ItemStack) (Object) this;
        Item item = this.getItem();
        String itemId = BuiltInRegistries.ITEM.getKey(item).toString();

        Map<String, String> args = new LinkedHashMap<>();
        args.put("arg0 (Level)", level.getClass().getName() + " [dim=" + level.dimension().location() + ", isClientSide=" + level.isClientSide() + "]");
        args.put("arg1 (Player)", player.getClass().getName() + " [name=" + player.getName().getString() + ", pos=" + player.position() + "]");
        args.put("arg2 (InteractionHand)", hand.name());
        args.put("ItemStack", self.getCount() + "x " + itemId + " (ItemClass=" + item.getClass().getName() + ")");
        args.put("DataComponents", self.getComponents().toString());

        FlightRecorder.pushScope(
                level.getGameTime(),
                level.isClientSide(),
                "ITEM_USE",
                "Player '" + player.getName().getString() + "' used '" + itemId + "' (" + hand.name() + ") at [" + player.blockPosition().toShortString() + "]"
        );

        FrameCaptureContext.pushFrame(
                level.getGameTime(),
                "ITEM_USE",
                item.getClass().getName() + ".use(Level, Player, InteractionHand)",
                level.isClientSide() ? "CLIENT" : "SERVER",
                item.getClass(),
                args
        );
    }

    @Inject(method = "use", at = @At("RETURN"))
    private void unwind$onUseReturn(Level level, Player player, InteractionHand hand, CallbackInfoReturnable<InteractionResultHolder<ItemStack>> cir) {
        FrameCaptureContext.popFrame();
        FlightRecorder.popScope();
    }

    @Inject(method = "useOn", at = @At("HEAD"))
    private void unwind$onUseOnHead(UseOnContext context, CallbackInfoReturnable<InteractionResult> cir) {
        ItemStack self = (ItemStack) (Object) this;
        Item item = this.getItem();
        Level level = context.getLevel();
        String itemId = BuiltInRegistries.ITEM.getKey(item).toString();

        Map<String, String> args = new LinkedHashMap<>();
        args.put("ClickedPos", context.getClickedPos().toShortString());
        args.put("ClickedFace", context.getClickedFace().name());
        args.put("ClickedBlockState", level.getBlockState(context.getClickedPos()).toString());
        args.put("Player", context.getPlayer() != null ? context.getPlayer().getName().getString() : "null");
        args.put("Hand", context.getHand().name());
        args.put("ItemStack", self.getCount() + "x " + itemId);
        args.put("DataComponents", self.getComponents().toString());
        args.put("Level", level.getClass().getName() + " [isClientSide=" + level.isClientSide() + "]");

        FlightRecorder.pushScope(
                level.getGameTime(),
                level.isClientSide(),
                "ITEM_USE_ON_BLOCK",
                "Used '" + itemId + "' on block at [" + context.getClickedPos().toShortString() + "]"
        );

        FrameCaptureContext.pushFrame(
                level.getGameTime(),
                "ITEM_USE_ON_BLOCK",
                item.getClass().getName() + ".useOn(UseOnContext)",
                level.isClientSide() ? "CLIENT" : "SERVER",
                item.getClass(),
                args
        );
    }

    @Inject(method = "useOn", at = @At("RETURN"))
    private void unwind$onUseOnReturn(UseOnContext context, CallbackInfoReturnable<InteractionResult> cir) {
        FrameCaptureContext.popFrame();
        FlightRecorder.popScope();
    }

    @Inject(method = "inventoryTick", at = @At("HEAD"))
    private void unwind$onInventoryTickHead(Level level, Entity entity, int inventorySlot, boolean isCurrentItem, CallbackInfo ci) {
        ItemStack self = (ItemStack) (Object) this;
        Item item = this.getItem();
        String itemId = BuiltInRegistries.ITEM.getKey(item).toString();

        Map<String, String> args = new LinkedHashMap<>();
        args.put("ItemStack", self.getCount() + "x " + itemId);
        args.put("DataComponents", self.getComponents().toString());
        args.put("HolderEntity", entity.getClass().getName() + " [name=" + entity.getName().getString() + "]");
        args.put("SlotIndex", String.valueOf(inventorySlot));
        args.put("IsSelected", String.valueOf(isCurrentItem));
        args.put("Level", level.getClass().getName() + " [isClientSide=" + level.isClientSide() + "]");

        FrameCaptureContext.pushFrame(
                level.getGameTime(),
                "ITEM_INVENTORY_TICK",
                item.getClass().getName() + ".inventoryTick",
                level.isClientSide() ? "CLIENT" : "SERVER",
                item.getClass(),
                args
        );
    }

    @Inject(method = "inventoryTick", at = @At("RETURN"))
    private void unwind$onInventoryTickReturn(Level level, Entity entity, int inventorySlot, boolean isCurrentItem, CallbackInfo ci) {
        FrameCaptureContext.popFrame();
    }
}