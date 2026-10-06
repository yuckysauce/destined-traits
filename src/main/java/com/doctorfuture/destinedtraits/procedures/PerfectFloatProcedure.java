package com.doctorfuture.destinedtraits.procedures;

import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.bus.api.Event;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.effect.MobEffectInstance;

import com.doctorfuture.destinedtraits.network.DestinedtraitsModVariables;
import com.doctorfuture.destinedtraits.init.DestinedtraitsModMobEffects;
import com.doctorfuture.destinedtraits.perks.MasterworkUtils;

import javax.annotation.Nullable;

@EventBusSubscriber
public class PerfectFloatProcedure {
	@SubscribeEvent
	public static void onPlayerTick(PlayerTickEvent.Post event) {
		execute(event, event.getEntity());
	}

	public static void execute(Entity entity) {
		execute(null, entity);
	}

	private static void execute(@Nullable Event event, Entity entity) {
		if (entity == null)
			return;
		double enhancedbonus = 0;
		if (entity.isAlive()) {
			if ((entity instanceof net.minecraft.world.entity.LivingEntity _livEnt ? _livEnt.getMainHandItem() : net.minecraft.world.item.ItemStack.EMPTY)
					.getOrDefault((net.minecraft.core.component.DataComponentType<java.util.List<net.minecraft.resources.ResourceLocation>>) (Object) net.minecraft.core.registries.BuiltInRegistries.DATA_COMPONENT_TYPE
							.get(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("destinedtraits", "perks")), java.util.Collections.emptyList())
					.contains(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("destinedtraits", "perfect_float")) == true) {
				if (entity.getData(DestinedtraitsModVariables.PLAYER_VARIABLES).in_combat_bool == true) {
					if (java.util.Optional.of(entity instanceof net.minecraft.world.entity.LivingEntity _livEnt ? _livEnt.getMainHandItem() : net.minecraft.world.item.ItemStack.EMPTY).map(item -> {
						net.minecraft.core.component.DataComponentType<java.util.List<net.minecraft.resources.ResourceLocation>> comp = (net.minecraft.core.component.DataComponentType<java.util.List<net.minecraft.resources.ResourceLocation>>) (Object) net.minecraft.core.registries.BuiltInRegistries.DATA_COMPONENT_TYPE
								.get(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("destinedtraits", "perks"));
						java.util.List<net.minecraft.resources.ResourceLocation> perks = item.getOrDefault(comp, java.util.Collections.emptyList());
						int index = perks.indexOf(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("destinedtraits", "perfect_float"));
						if (index == -1)
							return false; // The weapon does not have the perk at all
						int tier = item.getOrDefault(net.minecraft.core.component.DataComponents.CUSTOM_DATA, net.minecraft.world.item.component.CustomData.EMPTY).copyTag().getInt("tier");
						// Returns true if left perk is tier 3+, or right perk is tier 4
						return WeaponPerkUtils.isEnhancedAtIndex(item, index);
					}).orElse(false)) {
						enhancedbonus = 1;
					} else {
						enhancedbonus = 0;
					}
					if (entity instanceof Player player) {
						MasterworkUtils.Stats masterworks = MasterworkUtils.getStats(player);
						if (entity instanceof LivingEntity _entity && !_entity.level().isClientSide())
							_entity.addEffect(new MobEffectInstance(DestinedtraitsModMobEffects.PERFECT_FLOAT_EFFECT, masterworks.scaleDuration(20), (int) enhancedbonus, true, true));
					}
				}
			}
		}
	}
}