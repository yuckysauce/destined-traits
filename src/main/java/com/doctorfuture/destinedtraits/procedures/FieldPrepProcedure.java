package com.doctorfuture.destinedtraits.procedures;

import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.bus.api.Event;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.effect.MobEffectInstance;

import com.doctorfuture.destinedtraits.init.DestinedtraitsModMobEffects;
import com.doctorfuture.destinedtraits.perks.MasterworkUtils;

import javax.annotation.Nullable;

@EventBusSubscriber
public class FieldPrepProcedure {
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
		double amplifier = 0;
		double enhancedbonus = 0;
		double duration = 0;
		if (entity.isShiftKeyDown()) {
			if ((entity instanceof net.minecraft.world.entity.LivingEntity _livEnt ? _livEnt.getMainHandItem() : net.minecraft.world.item.ItemStack.EMPTY)
					.getOrDefault((net.minecraft.core.component.DataComponentType<java.util.List<net.minecraft.resources.ResourceLocation>>) (Object) net.minecraft.core.registries.BuiltInRegistries.DATA_COMPONENT_TYPE
							.get(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("destinedtraits", "perks")), java.util.Collections.emptyList())
					.contains(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("destinedtraits", "field_prep")) == true) {
				if (java.util.Optional.of(entity instanceof net.minecraft.world.entity.LivingEntity _livEnt ? _livEnt.getMainHandItem() : net.minecraft.world.item.ItemStack.EMPTY).map(item -> {
					net.minecraft.core.component.DataComponentType<java.util.List<net.minecraft.resources.ResourceLocation>> comp = (net.minecraft.core.component.DataComponentType<java.util.List<net.minecraft.resources.ResourceLocation>>) (Object) net.minecraft.core.registries.BuiltInRegistries.DATA_COMPONENT_TYPE
							.get(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("destinedtraits", "perks"));
					java.util.List<net.minecraft.resources.ResourceLocation> perks = item.getOrDefault(comp, java.util.Collections.emptyList());
					int index = perks.indexOf(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("destinedtraits", "field_prep"));
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
				MasterworkUtils.Stats masterworks = entity instanceof net.minecraft.world.entity.player.Player player
						? MasterworkUtils.getStats(player) : new MasterworkUtils.Stats(100.0, 100.0, 100.0, 100.0);
				if (entity instanceof LivingEntity _livEnt1 && _livEnt1.hasEffect(DestinedtraitsModMobEffects.FIELD_PREP_EFFECT)) {
					if ((entity instanceof LivingEntity _livEnt && _livEnt.hasEffect(DestinedtraitsModMobEffects.FIELD_PREP_EFFECT) ? _livEnt.getEffect(DestinedtraitsModMobEffects.FIELD_PREP_EFFECT).getDuration() : 0) >= 2) {
						if (entity instanceof LivingEntity _entity && !_entity.level().isClientSide())
							_entity.addEffect(new MobEffectInstance(DestinedtraitsModMobEffects.FIELD_PREP_EFFECT, masterworks.scaleDuration(5), (int) enhancedbonus, true, true));
					}
				} else {
					if (entity instanceof LivingEntity _entity && !_entity.level().isClientSide())
						_entity.addEffect(new MobEffectInstance(DestinedtraitsModMobEffects.FIELD_PREP_EFFECT, masterworks.scaleDuration(5), (int) enhancedbonus, true, true));
				}
			}
		}
	}
}