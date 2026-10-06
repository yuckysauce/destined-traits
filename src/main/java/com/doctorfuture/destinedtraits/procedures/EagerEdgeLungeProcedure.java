package com.doctorfuture.destinedtraits.procedures;

import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.bus.api.Event;
import net.neoforged.api.distmarker.Dist;

import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.chat.Component;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.core.SectionPos;

import com.doctorfuture.destinedtraits.init.DestinedtraitsModMobEffects;
import com.doctorfuture.destinedtraits.DestinedtraitsMod;
import com.doctorfuture.destinedtraits.perks.MasterworkUtils;

import javax.annotation.Nullable;

@EventBusSubscriber(Dist.CLIENT)
public class EagerEdgeLungeProcedure {
	@SubscribeEvent
	public static void onLeftClick(PlayerInteractEvent.LeftClickEmpty event) {
		PacketDistributor.sendToServer(new EagerEdgeLungeMessage());
		execute(event.getLevel(), event.getEntity());
	}

	@EventBusSubscriber
	public record EagerEdgeLungeMessage() implements CustomPacketPayload {
		public static final Type<EagerEdgeLungeMessage> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(DestinedtraitsMod.MODID, "procedure_eager_edge_lunge"));
		public static final StreamCodec<RegistryFriendlyByteBuf, EagerEdgeLungeMessage> STREAM_CODEC = StreamCodec.of((RegistryFriendlyByteBuf buffer, EagerEdgeLungeMessage message) -> {
		}, (RegistryFriendlyByteBuf buffer) -> new EagerEdgeLungeMessage());

		@Override
		public Type<EagerEdgeLungeMessage> type() {
			return TYPE;
		}

		public static void handleData(final EagerEdgeLungeMessage message, final IPayloadContext context) {
			if (context.flow() == PacketFlow.SERVERBOUND) {
				context.enqueueWork(() -> {
					if (!context.player().level().getChunkSource().hasChunk(SectionPos.blockToSectionCoord(context.player().getX()), SectionPos.blockToSectionCoord(context.player().getZ())))
						return;
					execute(context.player().level(), context.player());
				}).exceptionally(e -> {
					context.connection().disconnect(Component.literal(e.getMessage()));
					return null;
				});
			}
		}

		@SubscribeEvent
		public static void registerMessage(FMLCommonSetupEvent event) {
			DestinedtraitsMod.addNetworkMessage(EagerEdgeLungeMessage.TYPE, EagerEdgeLungeMessage.STREAM_CODEC, EagerEdgeLungeMessage::handleData);
		}
	}

	public static void execute(LevelAccessor world, Entity entity) {
		execute(null, world, entity);
	}

	private static void execute(@Nullable Event event, LevelAccessor world, Entity entity) {
		if (entity == null)
			return;
		if (entity instanceof LivingEntity _livEnt0 && _livEnt0.hasEffect(DestinedtraitsModMobEffects.EAGER_EDGE_EFFECT)) {
			if (entity instanceof LivingEntity _entity)
				_entity.removeEffect(DestinedtraitsModMobEffects.EAGER_EDGE_EFFECT);
			if (entity instanceof net.minecraft.world.entity.Entity _ent) {
				double _power = 3;
				if (entity instanceof Player player) {
					_power *= MasterworkUtils.getStats(player).strengthMultiplier();
				}
				net.minecraft.world.phys.Vec3 _motion;
				if (true) {
					_motion = _ent.getLookAngle().scale(_power);
				} else {
					double _yaw = Math.toRadians(_ent.getYRot());
					double _dx = -Math.sin(_yaw);
					double _dz = Math.cos(_yaw);
					_motion = new net.minecraft.world.phys.Vec3(_dx * _power, _ent.getDeltaMovement().y, _dz * _power);
				}
				_ent.setDeltaMovement(_motion);
				_ent.hurtMarked = true;
				if (_ent instanceof net.minecraft.server.level.ServerPlayer _player) {
					_player.connection.send(new net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket(_ent));
				}
			}
		}
	}
}