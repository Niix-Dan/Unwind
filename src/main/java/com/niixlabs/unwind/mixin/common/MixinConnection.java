package com.niixlabs.unwind.mixin.common;

import com.niixlabs.unwind.analysis.ModProvenanceResolver;
import com.niixlabs.unwind.core.FlightRecorder;
import com.niixlabs.unwind.core.FrameCaptureContext;
import net.minecraft.network.Connection;
import net.minecraft.network.PacketListener;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket;
import net.minecraft.network.protocol.common.ServerboundCustomPayloadPacket;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

@Mixin(Connection.class)
public class MixinConnection {

    @Unique
    private static final Set<String> UNWIND$NOISY_PACKETS = Set.of(
            "minecraft:move_player_pos",
            "minecraft:move_player_pos_rot",
            "minecraft:move_player_rot",
            "minecraft:move_player_status_only",
            "minecraft:move_entity_pos",
            "minecraft:move_entity_pos_rot",
            "minecraft:move_entity_rot",
            "minecraft:rotate_head",
            "minecraft:set_entity_motion",
            "minecraft:teleport_entity",
            "minecraft:keep_alive",
            "minecraft:ping",
            "minecraft:pong",
            "minecraft:set_time",
            "minecraft:level_chunk_with_light",
            "minecraft:chunk_batch_start",
            "minecraft:chunk_batch_finished",
            "minecraft:chunk_batch_received",
            "minecraft:light_update",
            "minecraft:bundle",
            "minecraft:bundle_delimiter",
            "minecraft:entity_event",
            "minecraft:set_entity_data",
            "minecraft:update_attributes"
    );

    @Inject(method = "genericsFtw", at = @At("HEAD"))
    private static <T extends PacketListener> void unwind$onPacketHandleHead(Packet<T> packet, PacketListener listener, CallbackInfo ci) {
        String packetId = packet.type() != null ? packet.type().id().toString() : packet.getClass().getName();
        String flow = packet.type() != null ? packet.type().flow().name() : "UNKNOWN";
        Class<?> ownerClass = packet.getClass();
        String payloadDump = null;

        CustomPacketPayload customPayload = unwind$extractCustomPayload(packet);
        if (customPayload != null) {
            if (customPayload.type() != null && customPayload.type().id() != null) {
                packetId = "custom_payload(" + customPayload.type().id() + ")";
            }
            ownerClass = customPayload.getClass();
            payloadDump = customPayload.toString();
        }

        String ownerModId = ModProvenanceResolver.resolveModId(ownerClass);

        Map<String, String> args = new LinkedHashMap<>();
        args.put("PacketTypeId", packetId);
        args.put("PacketFlow", flow);
        args.put("PayloadClass", ownerClass.getName());
        args.put("PayloadModId", ownerModId);
        if (payloadDump != null) {
            args.put("PayloadData", payloadDump);
        }
        args.put("PacketListenerClass", listener.getClass().getName());
        args.put("Protocol", listener.protocol().name());

        boolean isClient = "CLIENTBOUND".equals(flow);
        String desc = String.format("Packet '%s' (%s) via %s [PayloadClass: %s | Mod: %s]",
                packetId, flow, listener.getClass().getSimpleName(), ownerClass.getSimpleName(), ownerModId);

        if (UNWIND$NOISY_PACKETS.contains(packetId)) {
            FlightRecorder.pushTransientScope(-1L, isClient, "NETWORK_PACKET", desc);
        } else {
            FlightRecorder.pushScope(-1L, isClient, "NETWORK_PACKET", desc);
        }

        FrameCaptureContext.pushFrame(
                -1L,
                "NETWORK_PACKET",
                ownerClass.getName() + ".handle(" + listener.getClass().getSimpleName() + ")",
                isClient ? "CLIENT" : "SERVER",
                ownerClass,
                args
        );
    }

    @Inject(method = "genericsFtw", at = @At("RETURN"))
    private static <T extends PacketListener> void unwind$onPacketHandleReturn(Packet<T> packet, PacketListener listener, CallbackInfo ci) {
        FrameCaptureContext.popFrame();
        FlightRecorder.popScope();
    }

    @Unique
    private static CustomPacketPayload unwind$extractCustomPayload(Packet<?> packet) {
        if (packet instanceof ClientboundCustomPayloadPacket clientCustom) {
            return clientCustom.payload();
        }
        if (packet instanceof ServerboundCustomPayloadPacket serverCustom) {
            return serverCustom.payload();
        }
        return null;
    }
}