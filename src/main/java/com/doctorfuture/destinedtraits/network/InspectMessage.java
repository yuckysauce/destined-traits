package com.doctorfuture.destinedtraits.network;

import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.bus.api.SubscribeEvent;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.chat.Component;
import net.minecraft.network.RegistryFriendlyByteBuf;

import com.doctorfuture.destinedtraits.DestinedtraitsMod;

@EventBusSubscriber
public record InspectMessage(int eventType, int pressedms) implements CustomPacketPayload {
	public static final Type<InspectMessage> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(DestinedtraitsMod.MODID, "key_inspect"));
	public static final StreamCodec<RegistryFriendlyByteBuf, InspectMessage> STREAM_CODEC = StreamCodec.of((RegistryFriendlyByteBuf buffer, InspectMessage message) -> {
		buffer.writeInt(message.eventType);
		buffer.writeInt(message.pressedms);
	}, (RegistryFriendlyByteBuf buffer) -> new InspectMessage(buffer.readInt(), buffer.readInt()));

	@Override
	public Type<InspectMessage> type() {
		return TYPE;
	}

	public static void handleData(final InspectMessage message, final IPayloadContext context) {
		if (context.flow() == PacketFlow.SERVERBOUND) {
			context.enqueueWork(() -> {
			}).exceptionally(e -> {
				context.connection().disconnect(Component.literal(e.getMessage()));
				return null;
			});
		}
	}

	@SubscribeEvent
	public static void registerMessage(FMLCommonSetupEvent event) {
		DestinedtraitsMod.addNetworkMessage(InspectMessage.TYPE, InspectMessage.STREAM_CODEC, InspectMessage::handleData);
	}
}