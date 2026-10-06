package com.doctorfuture.destinedtraits.procedures;

import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.bus.api.Event;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Entity;

import javax.annotation.Nullable;

@EventBusSubscriber
public class SubsistenceProcedure {
	@SubscribeEvent
	public static void onEntityDeath(LivingDeathEvent event) {
		if (event.getEntity() != null) {
			execute(event, event.getSource().getEntity());
		}
	}

	public static void execute(Entity sourceentity) {
		execute(null, sourceentity);
	}

	private static void execute(@Nullable Event event, Entity sourceentity) {
		if (sourceentity == null)
			return;
		double amplifier = 0;
		double enhancedbonus = 0;
		double duration = 0;
		if (sourceentity instanceof Player) {
			if ((sourceentity instanceof net.minecraft.world.entity.LivingEntity _livEnt ? _livEnt.getMainHandItem() : net.minecraft.world.item.ItemStack.EMPTY)
					.getOrDefault((net.minecraft.core.component.DataComponentType<java.util.List<net.minecraft.resources.ResourceLocation>>) (Object) net.minecraft.core.registries.BuiltInRegistries.DATA_COMPONENT_TYPE
							.get(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("destinedtraits", "perks")), java.util.Collections.emptyList())
					.contains(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("destinedtraits", "subsistence")) == true) {
				if (java.util.Optional.of(sourceentity instanceof net.minecraft.world.entity.LivingEntity _livEnt ? _livEnt.getMainHandItem() : net.minecraft.world.item.ItemStack.EMPTY).map(item -> {
					net.minecraft.core.component.DataComponentType<java.util.List<net.minecraft.resources.ResourceLocation>> comp = (net.minecraft.core.component.DataComponentType<java.util.List<net.minecraft.resources.ResourceLocation>>) (Object) net.minecraft.core.registries.BuiltInRegistries.DATA_COMPONENT_TYPE
							.get(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("destinedtraits", "perks"));
					java.util.List<net.minecraft.resources.ResourceLocation> perks = item.getOrDefault(comp, java.util.Collections.emptyList());
					int index = perks.indexOf(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("destinedtraits", "subsistence"));
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
				(sourceentity instanceof LivingEntity _livEnt ? _livEnt.getMainHandItem() : ItemStack.EMPTY)
						.setDamageValue((int) ((sourceentity instanceof LivingEntity _livEnt ? _livEnt.getMainHandItem() : ItemStack.EMPTY).getDamageValue() - enhancedbonus));
			}
		}
	}
}