package com.niixlabs.unwind;

import com.niixlabs.unwind.core.FlightRecorder;
import com.niixlabs.unwind.core.FrameCaptureContext;
import com.niixlabs.unwind.core.GlobalExceptionInterceptor;
import com.niixlabs.unwind.logger.LiveBreakpointAppender;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerContainerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

import java.nio.file.Path;

@Mod(UnwindMod.MODID)
public class UnwindMod {

    public static final String MODID = "unwind";

    public UnwindMod(IEventBus modEventBus) {
        Path reportsDir = FMLPaths.GAMEDIR.get().resolve("crash-reports/unwind");
        LiveBreakpointAppender.install(reportsDir);
        GlobalExceptionInterceptor.install();
        NeoForge.EVENT_BUS.register(this);
    }

    @SubscribeEvent
    public void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        Level level = event.getLevel();
        BlockState state = level.getBlockState(event.getPos());
        String desc = String.format("Player '%s' RightClickBlock '%s' at [%s] holding '%s'",
                event.getEntity().getName().getString(),
                BuiltInRegistries.BLOCK.getKey(state.getBlock()),
                event.getPos().toShortString(),
                BuiltInRegistries.ITEM.getKey(event.getItemStack().getItem())
        );
        FlightRecorder.pushScope(level.getGameTime(), level.isClientSide(), "EVENT_RIGHT_CLICK_BLOCK", desc);
        FlightRecorder.popScope();
    }

    @SubscribeEvent
    public void onLeftClickBlock(PlayerInteractEvent.LeftClickBlock event) {
        Level level = event.getLevel();
        BlockState state = level.getBlockState(event.getPos());
        String desc = String.format("Player '%s' LeftClickBlock '%s' at [%s] holding '%s'",
                event.getEntity().getName().getString(),
                BuiltInRegistries.BLOCK.getKey(state.getBlock()),
                event.getPos().toShortString(),
                BuiltInRegistries.ITEM.getKey(event.getItemStack().getItem())
        );
        FlightRecorder.pushScope(level.getGameTime(), level.isClientSide(), "EVENT_LEFT_CLICK_BLOCK", desc);
        FlightRecorder.popScope();
    }

    @SubscribeEvent
    public void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        Level level = event.getLevel();
        String desc = String.format("Player '%s' EntityInteract '%s' (id=%d) at [%s]",
                event.getEntity().getName().getString(),
                BuiltInRegistries.ENTITY_TYPE.getKey(event.getTarget().getType()),
                event.getTarget().getId(),
                event.getPos().toShortString()
        );
        FlightRecorder.pushScope(level.getGameTime(), level.isClientSide(), "EVENT_ENTITY_INTERACT", desc);
        FlightRecorder.popScope();
    }

    @SubscribeEvent
    public void onBlockBreak(BlockEvent.BreakEvent event) {
        if (event.getLevel() instanceof Level level) {
            String desc = String.format("Player '%s' broke '%s' at [%s]",
                    event.getPlayer().getName().getString(),
                    BuiltInRegistries.BLOCK.getKey(event.getState().getBlock()),
                    event.getPos().toShortString()
            );
            FlightRecorder.pushScope(level.getGameTime(), level.isClientSide(), "EVENT_BLOCK_BREAK", desc);
            FlightRecorder.popScope();
        }
    }

    @SubscribeEvent
    public void onContainerOpen(PlayerContainerEvent.Open event) {
        Level level = event.getEntity().level();
        String desc = String.format("Player '%s' opened Container '%s' (containerId=%d)",
                event.getEntity().getName().getString(),
                event.getContainer().getClass().getName(),
                event.getContainer().containerId
        );
        FlightRecorder.pushScope(level.getGameTime(), level.isClientSide(), "EVENT_CONTAINER_OPEN", desc);
        FlightRecorder.popScope();
    }

    @SubscribeEvent
    public void onLivingDamage(LivingDamageEvent.Pre event) {
        Level level = event.getEntity().level();
        String desc = String.format("Entity '%s' taking %.2f damage from '%s'",
                BuiltInRegistries.ENTITY_TYPE.getKey(event.getEntity().getType()),
                event.getOriginalDamage(),
                event.getSource().getMsgId()
        );
        FlightRecorder.pushScope(level.getGameTime(), level.isClientSide(), "EVENT_LIVING_DAMAGE", desc);
        FlightRecorder.popScope();
    }

    @SubscribeEvent
    public void onLivingDeath(LivingDeathEvent event) {
        Level level = event.getEntity().level();
        String desc = String.format("Entity '%s' died from '%s' at [%s]",
                BuiltInRegistries.ENTITY_TYPE.getKey(event.getEntity().getType()),
                event.getSource().getMsgId(),
                event.getEntity().blockPosition().toShortString()
        );
        FlightRecorder.pushScope(level.getGameTime(), level.isClientSide(), "EVENT_LIVING_DEATH", desc);
        FlightRecorder.popScope();
    }

    @SubscribeEvent
    public void onLevelTickPost(LevelTickEvent.Post event) {
        FrameCaptureContext.clearThreadFrames();
        FlightRecorder.clearCurrentThreadScopes();
    }
}