package com.doctorfuture.destinedtraits.network;

import com.doctorfuture.destinedtraits.DestinedtraitsMod;
import com.doctorfuture.destinedtraits.procedures.NorthStarProcedure;
import com.doctorfuture.destinedtraits.procedures.RedstoneBatteryProcedure;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;

@EventBusSubscriber(modid = DestinedtraitsMod.MODID)
public record ExoticWeaponInputPayload(boolean held) implements CustomPacketPayload {
    public static final Type<ExoticWeaponInputPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(DestinedtraitsMod.MODID, "exotic_weapon_input"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ExoticWeaponInputPayload> STREAM_CODEC = StreamCodec.of(
            (buffer, payload) -> buffer.writeBoolean(payload.held()),
            buffer -> new ExoticWeaponInputPayload(buffer.readBoolean()));

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    @SubscribeEvent
    public static void registerPayload(FMLCommonSetupEvent event) {
        DestinedtraitsMod.addNetworkMessage(TYPE, STREAM_CODEC, ExoticWeaponInputPayload::handle);
    }

    private static void handle(ExoticWeaponInputPayload payload, IPayloadContext context) {
        if (context.flow() != PacketFlow.SERVERBOUND) return;
        context.enqueueWork(() -> {
            NorthStarProcedure.handleUseInput(context.player(), payload.held());
            RedstoneBatteryProcedure.handleUseInput(context.player(), payload.held());
        })
                .exceptionally(error -> {
                    context.connection().disconnect(Component.literal(error.getMessage()));
                    return null;
                });
    }
}