package com.doctorfuture.destinedtraits.network;

import net.neoforged.neoforge.registries.NeoForgeRegistries;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.common.util.INBTSerializable;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.bus.api.SubscribeEvent;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.chat.Component;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.core.HolderLookup;

import java.util.function.Supplier;

import com.doctorfuture.destinedtraits.DestinedtraitsMod;

@EventBusSubscriber
public class DestinedtraitsModVariables {
	public static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES = DeferredRegister.create(NeoForgeRegistries.Keys.ATTACHMENT_TYPES, DestinedtraitsMod.MODID);
	public static final Supplier<AttachmentType<PlayerVariables>> PLAYER_VARIABLES = ATTACHMENT_TYPES.register("player_variables", () -> AttachmentType.serializable(PlayerVariables::new).build());

	@SubscribeEvent
	public static void init(FMLCommonSetupEvent event) {
		DestinedtraitsMod.addNetworkMessage(PlayerVariablesSyncMessage.TYPE, PlayerVariablesSyncMessage.STREAM_CODEC, PlayerVariablesSyncMessage::handleData);
	}

	@SubscribeEvent
	public static void onPlayerLoggedInSyncPlayerVariables(PlayerEvent.PlayerLoggedInEvent event) {
		if (event.getEntity() instanceof ServerPlayer player)
			PacketDistributor.sendToPlayer(player, new PlayerVariablesSyncMessage(player.getData(PLAYER_VARIABLES)));
	}

	@SubscribeEvent
	public static void onPlayerRespawnedSyncPlayerVariables(PlayerEvent.PlayerRespawnEvent event) {
		if (event.getEntity() instanceof ServerPlayer player)
			PacketDistributor.sendToPlayer(player, new PlayerVariablesSyncMessage(player.getData(PLAYER_VARIABLES)));
	}

	@SubscribeEvent
	public static void onPlayerChangedDimensionSyncPlayerVariables(PlayerEvent.PlayerChangedDimensionEvent event) {
		if (event.getEntity() instanceof ServerPlayer player)
			PacketDistributor.sendToPlayer(player, new PlayerVariablesSyncMessage(player.getData(PLAYER_VARIABLES)));
	}

	@SubscribeEvent
	public static void onPlayerTickUpdateSyncPlayerVariables(PlayerTickEvent.Post event) {
		if (event.getEntity() instanceof ServerPlayer player && player.getData(PLAYER_VARIABLES)._syncDirty) {
			PacketDistributor.sendToPlayer(player, new PlayerVariablesSyncMessage(player.getData(PLAYER_VARIABLES)));
			player.getData(PLAYER_VARIABLES)._syncDirty = false;
		}
	}

	@SubscribeEvent
	public static void clonePlayer(PlayerEvent.Clone event) {
		PlayerVariables original = event.getOriginal().getData(PLAYER_VARIABLES);
		PlayerVariables clone = new PlayerVariables();
		if (!event.isWasDeath()) {
			clone.recuperation_progress = original.recuperation_progress;
			clone.betteralready_progress = original.betteralready_progress;
			clone.breatheeasy_progress = original.breatheeasy_progress;
			clone.eageredge_timer = original.eageredge_timer;
			clone.last_hit_uuid = original.last_hit_uuid;
			clone.in_combat = original.in_combat;
			clone.in_combat_timer = original.in_combat_timer;
			clone.in_combat_bool = original.in_combat_bool;
			clone.harmony_ready = original.harmony_ready;
			clone.fieldforged_timer = original.fieldforged_timer;
			clone.fieldforged_progress = original.fieldforged_progress;
			clone.rapidkill_timer = original.rapidkill_timer;
			clone.rapidkill_progress = original.rapidkill_progress;
			clone.perpetualmotion_timer = original.perpetualmotion_timer;
			clone.argent_ordnance_progress = original.argent_ordnance_progress;
			clone.lucent_blades_progress = original.lucent_blades_progress;
			clone.rapidhit_timer = original.rapidhit_timer;
			clone.rapidhit_progress = original.rapidhit_progress;
			clone.enderartillery_progress = original.enderartillery_progress;
			clone.enderartillery_timer = original.enderartillery_timer;
		}
		event.getEntity().setData(PLAYER_VARIABLES, clone);
	}

	public static class PlayerVariables implements INBTSerializable<CompoundTag> {
		boolean _syncDirty = false;
		public double recuperation_progress = 0.0;
		public double betteralready_progress = 0.0;
		public double breatheeasy_progress = 0.0;
		public double eageredge_timer = 0.0;
		public String last_hit_uuid = "";
		public double in_combat = 0;
		public double in_combat_timer = 0;
		public boolean in_combat_bool = false;
		public boolean harmony_ready = false;
		public double fieldforged_timer = 0;
		public double fieldforged_progress = 0;
		public double rapidkill_timer = 0.0;
		public double rapidkill_progress = 0.0;
		public double perpetualmotion_timer = 0;
		public double argent_ordnance_progress = 0;
		public double lucent_blades_progress = 0;
		public double rapidhit_timer = 0;
		public double rapidhit_progress = 0;
		public double enderartillery_progress = 0;
		public double enderartillery_timer = 0;

		@Override
		public CompoundTag serializeNBT(HolderLookup.Provider lookupProvider) {
			CompoundTag nbt = new CompoundTag();
			nbt.putDouble("recuperation_progress", recuperation_progress);
			nbt.putDouble("betteralready_progress", betteralready_progress);
			nbt.putDouble("breatheeasy_progress", breatheeasy_progress);
			nbt.putDouble("eageredge_timer", eageredge_timer);
			nbt.putString("last_hit_uuid", last_hit_uuid);
			nbt.putDouble("in_combat", in_combat);
			nbt.putDouble("in_combat_timer", in_combat_timer);
			nbt.putBoolean("in_combat_bool", in_combat_bool);
			nbt.putBoolean("harmony_ready", harmony_ready);
			nbt.putDouble("fieldforged_timer", fieldforged_timer);
			nbt.putDouble("fieldforged_progress", fieldforged_progress);
			nbt.putDouble("rapidkill_timer", rapidkill_timer);
			nbt.putDouble("rapidkill_progress", rapidkill_progress);
			nbt.putDouble("perpetualmotion_timer", perpetualmotion_timer);
			nbt.putDouble("argent_ordnance_progress", argent_ordnance_progress);
			nbt.putDouble("lucent_blades_progress", lucent_blades_progress);
			nbt.putDouble("rapidhit_timer", rapidhit_timer);
			nbt.putDouble("rapidhit_progress", rapidhit_progress);
			nbt.putDouble("enderartillery_progress", enderartillery_progress);
			nbt.putDouble("enderartillery_timer", enderartillery_timer);
			return nbt;
		}

		@Override
		public void deserializeNBT(HolderLookup.Provider lookupProvider, CompoundTag nbt) {
			recuperation_progress = nbt.getDouble("recuperation_progress");
			betteralready_progress = nbt.getDouble("betteralready_progress");
			breatheeasy_progress = nbt.getDouble("breatheeasy_progress");
			eageredge_timer = nbt.getDouble("eageredge_timer");
			last_hit_uuid = nbt.getString("last_hit_uuid");
			in_combat = nbt.getDouble("in_combat");
			in_combat_timer = nbt.getDouble("in_combat_timer");
			in_combat_bool = nbt.getBoolean("in_combat_bool");
			harmony_ready = nbt.getBoolean("harmony_ready");
			fieldforged_timer = nbt.getDouble("fieldforged_timer");
			fieldforged_progress = nbt.getDouble("fieldforged_progress");
			rapidkill_timer = nbt.getDouble("rapidkill_timer");
			rapidkill_progress = nbt.getDouble("rapidkill_progress");
			perpetualmotion_timer = nbt.getDouble("perpetualmotion_timer");
			argent_ordnance_progress = nbt.getDouble("argent_ordnance_progress");
			lucent_blades_progress = nbt.getDouble("lucent_blades_progress");
			rapidhit_timer = nbt.getDouble("rapidhit_timer");
			rapidhit_progress = nbt.getDouble("rapidhit_progress");
			enderartillery_progress = nbt.getDouble("enderartillery_progress");
			enderartillery_timer = nbt.getDouble("enderartillery_timer");
		}

		public void markSyncDirty() {
			_syncDirty = true;
		}
	}

	public record PlayerVariablesSyncMessage(PlayerVariables data) implements CustomPacketPayload {
		public static final Type<PlayerVariablesSyncMessage> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(DestinedtraitsMod.MODID, "player_variables_sync"));
		public static final StreamCodec<RegistryFriendlyByteBuf, PlayerVariablesSyncMessage> STREAM_CODEC = StreamCodec
				.of((RegistryFriendlyByteBuf buffer, PlayerVariablesSyncMessage message) -> buffer.writeNbt(message.data().serializeNBT(buffer.registryAccess())), (RegistryFriendlyByteBuf buffer) -> {
					PlayerVariablesSyncMessage message = new PlayerVariablesSyncMessage(new PlayerVariables());
					message.data.deserializeNBT(buffer.registryAccess(), buffer.readNbt());
					return message;
				});

		@Override
		public Type<PlayerVariablesSyncMessage> type() {
			return TYPE;
		}

		public static void handleData(final PlayerVariablesSyncMessage message, final IPayloadContext context) {
			if (context.flow() == PacketFlow.CLIENTBOUND && message.data != null) {
				context.enqueueWork(() -> context.player().getData(PLAYER_VARIABLES).deserializeNBT(context.player().registryAccess(), message.data.serializeNBT(context.player().registryAccess()))).exceptionally(e -> {
					context.connection().disconnect(Component.literal(e.getMessage()));
					return null;
				});
			}
		}
	}
}