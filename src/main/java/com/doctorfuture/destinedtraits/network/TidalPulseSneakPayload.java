package com.doctorfuture.destinedtraits.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import com.doctorfuture.destinedtraits.DestinedtraitsMod;
import com.doctorfuture.destinedtraits.procedures.TidalPulseProcedure;

@EventBusSubscriber(modid = DestinedtraitsMod.MODID)
public record TidalPulseSneakPayload() implements CustomPacketPayload {
	public static final Type<TidalPulseSneakPayload> TYPE = new Type<>(
			ResourceLocation.fromNamespaceAndPath(DestinedtraitsMod.MODID, "tidal_pulse_sneak"));
	public static final StreamCodec<RegistryFriendlyByteBuf, TidalPulseSneakPayload> STREAM_CODEC =
			StreamCodec.of((buffer, payload) -> {}, buffer -> new TidalPulseSneakPayload());

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}

	@SubscribeEvent
	public static void registerPayload(FMLCommonSetupEvent event) {
		DestinedtraitsMod.addNetworkMessage(TYPE, STREAM_CODEC, TidalPulseSneakPayload::handle);
	}

	private static void handle(TidalPulseSneakPayload payload, IPayloadContext context) {
		if (context.flow() != PacketFlow.SERVERBOUND) return;
		context.enqueueWork(() -> TidalPulseProcedure.onConfiguredSneakPressed(context.player()))
				.exceptionally(error -> {
					context.connection().disconnect(Component.literal(error.getMessage()));
					return null;
				});
	}
}