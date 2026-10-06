package com.doctorfuture.destinedtraits.procedures;

import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.bus.api.Event;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.util.Mth;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;

import com.doctorfuture.destinedtraits.init.DestinedtraitsModMobEffects;
import com.doctorfuture.destinedtraits.perks.MasterworkUtils;

import javax.annotation.Nullable;

@EventBusSubscriber
public class RampageProcedure {
	@SubscribeEvent
	public static void onRampageDamage(LivingDamageEvent.Pre event) {
		if (!(event.getSource().getEntity() instanceof Player player) || player.level().isClientSide()) return;
		if (!player.hasEffect(DestinedtraitsModMobEffects.RAMPAGE_EFFECT)
				|| !WeaponPerkUtils.hasPerk(player.getMainHandItem(), net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("destinedtraits", "rampage"))) return;

		int stacks = player.getEffect(DestinedtraitsModMobEffects.RAMPAGE_EFFECT).getAmplifier() + 1;
		double strength = MasterworkUtils.getStats(player).strengthMultiplier();
		event.setNewDamage(event.getNewDamage() + (float) (stacks * strength));
	}

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
					.contains(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("destinedtraits", "rampage")) == true) {
				if (java.util.Optional.of(sourceentity instanceof net.minecraft.world.entity.LivingEntity _livEnt ? _livEnt.getMainHandItem() : net.minecraft.world.item.ItemStack.EMPTY).map(item -> {
					net.minecraft.core.component.DataComponentType<java.util.List<net.minecraft.resources.ResourceLocation>> comp = (net.minecraft.core.component.DataComponentType<java.util.List<net.minecraft.resources.ResourceLocation>>) (Object) net.minecraft.core.registries.BuiltInRegistries.DATA_COMPONENT_TYPE
							.get(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("destinedtraits", "perks"));
					java.util.List<net.minecraft.resources.ResourceLocation> perks = item.getOrDefault(comp, java.util.Collections.emptyList());
					int index = perks.indexOf(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("destinedtraits", "rampage"));
					if (index == -1)
						return false; // The weapon does not have the perk at all
					int tier = item.getOrDefault(net.minecraft.core.component.DataComponents.CUSTOM_DATA, net.minecraft.world.item.component.CustomData.EMPTY).copyTag().getInt("tier");
					// Returns true if left perk is tier 3+, or right perk is tier 4
					return WeaponPerkUtils.isEnhancedAtIndex(item, index);
				}).orElse(false)) {
					enhancedbonus = 2;
					duration = 110;
				} else {
					enhancedbonus = 1;
					duration = 80;
				}
				MasterworkUtils.Stats masterworks = MasterworkUtils.getStats((Player) sourceentity);
				duration = masterworks.scaleDuration((int) duration);
				if (sourceentity instanceof LivingEntity _livEnt1 && _livEnt1.hasEffect(DestinedtraitsModMobEffects.RAMPAGE_EFFECT)) {
					amplifier = Mth.clamp((sourceentity instanceof LivingEntity _livEnt && _livEnt.hasEffect(DestinedtraitsModMobEffects.RAMPAGE_EFFECT) ? _livEnt.getEffect(DestinedtraitsModMobEffects.RAMPAGE_EFFECT).getAmplifier() : 0) + 1, 0,
							2 * enhancedbonus);
					if (sourceentity instanceof LivingEntity _entity && !_entity.level().isClientSide())
						_entity.addEffect(new MobEffectInstance(DestinedtraitsModMobEffects.RAMPAGE_EFFECT, (int) duration, (int) amplifier, true, true));
				} else {
					if (sourceentity instanceof LivingEntity _entity && !_entity.level().isClientSide())
						_entity.addEffect(new MobEffectInstance(DestinedtraitsModMobEffects.RAMPAGE_EFFECT, (int) duration, 0, true, true));
				}
			}
		}
	}
}