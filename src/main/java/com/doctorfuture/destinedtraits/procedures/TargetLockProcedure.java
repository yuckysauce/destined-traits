package com.doctorfuture.destinedtraits.procedures;

import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.bus.api.Event;

import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.util.Mth;
import net.minecraft.server.level.ServerLevel;

import com.doctorfuture.destinedtraits.network.DestinedtraitsModVariables;
import com.doctorfuture.destinedtraits.init.DestinedtraitsModMobEffects;

import javax.annotation.Nullable;

import java.util.UUID;
import com.doctorfuture.destinedtraits.perks.MasterworkUtils;

@EventBusSubscriber
public class TargetLockProcedure {
	@SubscribeEvent
	public static void onEntityAttacked(LivingIncomingDamageEvent event) {
		if (event.getEntity() != null) {
			execute(event, event.getEntity().level(), event.getEntity(), event.getSource().getEntity());
		}
	}

	public static void execute(LevelAccessor world, Entity entity, Entity sourceentity) {
		execute(null, world, entity, sourceentity);
	}

	private static void execute(@Nullable Event event, LevelAccessor world, Entity entity, Entity sourceentity) {
		if (entity == null || sourceentity == null)
			return;
		double amplifier = 0;
		double enhancedbonus = 0;
		double duration = 0;
		if (sourceentity instanceof Player) {
			if ((sourceentity instanceof net.minecraft.world.entity.LivingEntity _livEnt ? _livEnt.getMainHandItem() : net.minecraft.world.item.ItemStack.EMPTY)
					.getOrDefault((net.minecraft.core.component.DataComponentType<java.util.List<net.minecraft.resources.ResourceLocation>>) (Object) net.minecraft.core.registries.BuiltInRegistries.DATA_COMPONENT_TYPE
							.get(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("destinedtraits", "perks")), java.util.Collections.emptyList())
					.contains(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("destinedtraits", "target_lock")) == true) {
				if (entity == entityFromStringUUID(sourceentity.getData(DestinedtraitsModVariables.PLAYER_VARIABLES).last_hit_uuid, world)) {
					if (java.util.Optional.of(sourceentity instanceof net.minecraft.world.entity.LivingEntity _livEnt ? _livEnt.getMainHandItem() : net.minecraft.world.item.ItemStack.EMPTY).map(item -> {
						net.minecraft.core.component.DataComponentType<java.util.List<net.minecraft.resources.ResourceLocation>> comp = (net.minecraft.core.component.DataComponentType<java.util.List<net.minecraft.resources.ResourceLocation>>) (Object) net.minecraft.core.registries.BuiltInRegistries.DATA_COMPONENT_TYPE
								.get(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("destinedtraits", "perks"));
						java.util.List<net.minecraft.resources.ResourceLocation> perks = item.getOrDefault(comp, java.util.Collections.emptyList());
						int index = perks.indexOf(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("destinedtraits", "target_lock"));
						if (index == -1)
							return false; // The weapon does not have the perk at all
						int tier = item.getOrDefault(net.minecraft.core.component.DataComponents.CUSTOM_DATA, net.minecraft.world.item.component.CustomData.EMPTY).copyTag().getInt("tier");
						// Returns true if left perk is tier 3+, or right perk is tier 4
						return WeaponPerkUtils.isEnhancedAtIndex(item, index);
					}).orElse(false)) {
						enhancedbonus = 2;
					} else {
						enhancedbonus = 1;
					}
					MasterworkUtils.Stats masterworks = MasterworkUtils.getStats((Player) sourceentity);
					if (sourceentity instanceof LivingEntity _entity && !_entity.level().isClientSide())
						_entity.addEffect(new MobEffectInstance(DestinedtraitsModMobEffects.TARGET_LOCK_EFFECT, masterworks.scaleDuration(60),
								(int) (Mth.clamp(
										-1 + (sourceentity instanceof LivingEntity _livEnt && _livEnt.hasEffect(DestinedtraitsModMobEffects.TARGET_LOCK_EFFECT) ? _livEnt.getEffect(DestinedtraitsModMobEffects.TARGET_LOCK_EFFECT).getAmplifier() : 0)
												+ enhancedbonus,
										0, 10)),
								true, true));
				} else {
					if (sourceentity instanceof LivingEntity _entity)
						_entity.removeEffect(DestinedtraitsModMobEffects.TARGET_LOCK_EFFECT);
					{
						DestinedtraitsModVariables.PlayerVariables _vars = sourceentity.getData(DestinedtraitsModVariables.PLAYER_VARIABLES);
						_vars.last_hit_uuid = entity.getStringUUID();
						_vars.markSyncDirty();
					}
				}
			}
		}
	}

	private static Entity entityFromStringUUID(String uuid, LevelAccessor world) {
		if (world instanceof ServerLevel _server) {
			try {
				return _server.getEntity(UUID.fromString(uuid));
			} catch (Exception e) {
			}
		}
		return null;
	}
}