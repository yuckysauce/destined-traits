package com.doctorfuture.destinedtraits.procedures;

import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.bus.api.Event;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.effect.MobEffectInstance;

import com.doctorfuture.destinedtraits.init.DestinedtraitsModMobEffects;
import com.doctorfuture.destinedtraits.perks.MasterworkUtils;

import javax.annotation.Nullable;

@EventBusSubscriber
public class LooseChangeProcedure {
	@SubscribeEvent
	public static void onEntityAttacked(LivingIncomingDamageEvent event) {
		if (event.getEntity() != null) {
			execute(event, event.getEntity(), event.getSource().getEntity());
		}
	}

	public static void execute(Entity entity, Entity sourceentity) {
		execute(null, entity, sourceentity);
	}

	private static void execute(@Nullable Event event, Entity entity, Entity sourceentity) {
		if (entity == null || sourceentity == null)
			return;
		double amplifier = 0;
		double enhancedbonus = 0;
		double duration = 0;
		if (entity.isAlive()) {
			if (sourceentity instanceof Player) {
				if ((sourceentity instanceof net.minecraft.world.entity.LivingEntity _livEnt ? _livEnt.getMainHandItem() : net.minecraft.world.item.ItemStack.EMPTY)
						.getOrDefault((net.minecraft.core.component.DataComponentType<java.util.List<net.minecraft.resources.ResourceLocation>>) (Object) net.minecraft.core.registries.BuiltInRegistries.DATA_COMPONENT_TYPE
								.get(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("destinedtraits", "perks")), java.util.Collections.emptyList())
						.contains(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("destinedtraits", "loose_change")) == true) {
					if (java.util.Optional.of(sourceentity instanceof net.minecraft.world.entity.LivingEntity _livEnt ? _livEnt.getMainHandItem() : net.minecraft.world.item.ItemStack.EMPTY).map(item -> {
						net.minecraft.core.component.DataComponentType<java.util.List<net.minecraft.resources.ResourceLocation>> comp = (net.minecraft.core.component.DataComponentType<java.util.List<net.minecraft.resources.ResourceLocation>>) (Object) net.minecraft.core.registries.BuiltInRegistries.DATA_COMPONENT_TYPE
								.get(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("destinedtraits", "perks"));
						java.util.List<net.minecraft.resources.ResourceLocation> perks = item.getOrDefault(comp, java.util.Collections.emptyList());
						int index = perks.indexOf(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("destinedtraits", "loose_change"));
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
					if (!((net.minecraft.world.entity.LivingEntity) entity).getActiveEffects().isEmpty() == true) {
						MasterworkUtils.Stats masterworks = MasterworkUtils.getStats((Player) sourceentity);
						if (sourceentity instanceof LivingEntity _entity && !_entity.level().isClientSide())
							_entity.addEffect(new MobEffectInstance(DestinedtraitsModMobEffects.LOOSE_CHANGE_EFFECT, masterworks.scaleDuration(120), (int) enhancedbonus, true, true));
					}
				}
			}
		}
	}
}