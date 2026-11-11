package com.niixlabs.unwind.mixin.common;

import com.niixlabs.unwind.core.FlightRecorder;
import com.niixlabs.unwind.core.FrameCaptureContext;
import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import javax.annotation.Nullable;
import java.util.LinkedHashMap;
import java.util.Map;

@Mixin(AbstractContainerMenu.class)
public abstract class MixinAbstractContainerMenu {

    @Shadow @Final public NonNullList<Slot> slots;
    @Shadow @Final public int containerId;
    @Shadow @Nullable private MenuType<?> menuType;
    @Shadow public abstract ItemStack getCarried();

    @Inject(method = "clicked", at = @At("HEAD"))
    private void unwind$onMenuClickedHead(int slotId, int button, ClickType clickType, Player player, CallbackInfo ci) {
        String menuRegistryName = "unregistered_menu";
        if (this.menuType != null && BuiltInRegistries.MENU.getKey(this.menuType) != null) {
            menuRegistryName = BuiltInRegistries.MENU.getKey(this.menuType).toString();
        }

        String slotItemDesc = "OUT_OF_BOUNDS_OR_OUTSIDE";
        String slotContainerClass = "N/A";
        if (slotId >= 0 && slotId < this.slots.size()) {
            Slot slot = this.slots.get(slotId);
            if (slot != null) {
                ItemStack inSlot = slot.getItem();
                slotItemDesc = inSlot.isEmpty()
                        ? "EMPTY"
                        : inSlot.getCount() + "x " + BuiltInRegistries.ITEM.getKey(inSlot.getItem()) + " " + inSlot.getComponents();
                if (slot.container != null) {
                    slotContainerClass = slot.container.getClass().getName();
                }
            }
        }

        ItemStack carried = this.getCarried();
        String carriedDesc = carried.isEmpty()
                ? "EMPTY"
                : carried.getCount() + "x " + BuiltInRegistries.ITEM.getKey(carried.getItem()) + " " + carried.getComponents();

        Map<String, String> args = new LinkedHashMap<>();
        args.put("MenuClass", this.getClass().getName());
        args.put("MenuType", menuRegistryName);
        args.put("ContainerId", String.valueOf(this.containerId));
        args.put("SlotId", String.valueOf(slotId));
        args.put("MouseButtonOrKey", String.valueOf(button));
        args.put("ClickType", clickType.name());
        args.put("SlotBackingContainer", slotContainerClass);
        args.put("SlotStack", slotItemDesc);
        args.put("CursorCarriedStack", carriedDesc);
        args.put("Player", player.getName().getString() + " (" + player.getClass().getSimpleName() + ")");

        boolean isClient = player.level().isClientSide();
        long tick = player.level().getGameTime();

        FlightRecorder.pushScope(
                tick,
                isClient,
                "CONTAINER_SLOT_CLICK",
                "Player '" + player.getName().getString() + "' clicked Slot #" + slotId + " (" + clickType.name() + ", btn=" + button + ") in Menu '" + menuRegistryName + "' (" + this.getClass().getSimpleName() + ")"
        );

        FrameCaptureContext.pushFrame(
                tick,
                "CONTAINER_SLOT_CLICK",
                this.getClass().getName() + ".clicked(int, int, ClickType, Player)",
                isClient ? "CLIENT" : "SERVER",
                this.getClass(),
                args
        );
    }

    @Inject(method = "clicked", at = @At("RETURN"))
    private void unwind$onMenuClickedReturn(int slotId, int button, ClickType clickType, Player player, CallbackInfo ci) {
        FrameCaptureContext.popFrame();
        FlightRecorder.popScope();
    }
}