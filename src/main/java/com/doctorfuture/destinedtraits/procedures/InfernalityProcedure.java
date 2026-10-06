package com.doctorfuture.destinedtraits.procedures;

import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.bus.api.Event;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.Entity;

import javax.annotation.Nullable;
import com.doctorfuture.destinedtraits.perks.MasterworkUtils;

@EventBusSubscriber
public class InfernalityProcedure {
	@SubscribeEvent
	public static void onEntityAttacked(LivingIncomingDamageEvent event) {
		if (event.getEntity() != null) {
			execute(event, event.getEntity(), event.getSource().getEntity(), event.getAmount());
		}
	}

	public static void execute(Entity entity, Entity sourceentity, double amount) {
		execute(null, entity, sourceentity, amount);
	}

	private static void execute(@Nullable Event event, Entity entity, Entity sourceentity, double amount) {
		if (entity == null || sourceentity == null)
			return;
		double amplifier = 0;
		double enhancedbonus = 0;
		double duration = 0;
		if (sourceentity instanceof Player) {
			if ((sourceentity instanceof net.minecraft.world.entity.LivingEntity _livEnt ? _livEnt.getMainHandItem() : net.minecraft.world.item.ItemStack.EMPTY)
					.getOrDefault((net.minecraft.core.component.DataComponentType<java.util.List<net.minecraft.resources.ResourceLocation>>) (Object) net.minecraft.core.registries.BuiltInRegistries.DATA_COMPONENT_TYPE
							.get(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("destinedtraits", "perks")), java.util.Collections.emptyList())
					.contains(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("destinedtraits", "infernality")) == true) {
				if (java.util.Optional.of(sourceentity instanceof net.minecraft.world.entity.LivingEntity _livEnt ? _livEnt.getMainHandItem() : net.minecraft.world.item.ItemStack.EMPTY).map(item -> {
					net.minecraft.core.component.DataComponentType<java.util.List<net.minecraft.resources.ResourceLocation>> comp = (net.minecraft.core.component.DataComponentType<java.util.List<net.minecraft.resources.ResourceLocation>>) (Object) net.minecraft.core.registries.BuiltInRegistries.DATA_COMPONENT_TYPE
							.get(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("destinedtraits", "perks"));
					java.util.List<net.minecraft.resources.ResourceLocation> perks = item.getOrDefault(comp, java.util.Collections.emptyList());
					int index = perks.indexOf(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("destinedtraits", "infernality"));
					if (index == -1)
						return false; // The weapon does not have the perk at all
					int tier = item.getOrDefault(net.minecraft.core.component.DataComponents.CUSTOM_DATA, net.minecraft.world.item.component.CustomData.EMPTY).copyTag().getInt("tier");
					return WeaponPerkUtils.isOriginEnhanced(item, net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("destinedtraits", "infernality"));
				}).orElse(false)) {
					enhancedbonus = 3;
				} else {
					enhancedbonus = 2;
				}
				if (entity.getRemainingFireTicks() > 0) {
					MasterworkUtils.Stats masterworks = MasterworkUtils.getStats((Player) sourceentity);
					if (event instanceof LivingIncomingDamageEvent _event) {
						_event.setAmount((float) (amount + enhancedbonus * masterworks.strengthMultiplier()));
					}
				}
			}
		}
	}
}