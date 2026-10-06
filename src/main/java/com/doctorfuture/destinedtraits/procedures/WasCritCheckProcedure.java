package com.doctorfuture.destinedtraits.procedures;

import net.neoforged.neoforge.event.entity.player.CriticalHitEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.bus.api.Event;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.util.Mth;

import com.doctorfuture.destinedtraits.init.DestinedtraitsModMobEffects;
import com.doctorfuture.destinedtraits.perks.MasterworkUtils;

import javax.annotation.Nullable;

@EventBusSubscriber
public class WasCritCheckProcedure {
	@SubscribeEvent
	public static void onPlayerCriticalHit(CriticalHitEvent event) {
		execute(event, event.getEntity(), event.isVanillaCritical());
	}

	public static void execute(Entity sourceentity, boolean isvanillacritical) {
		execute(null, sourceentity, isvanillacritical);
	}

	private static void execute(@Nullable Event event, Entity sourceentity, boolean isvanillacritical) {
		if (sourceentity == null)
			return;
		double amplifier = 0;
		double enhancedbonus = 0;
		double duration = 0;
		if (sourceentity.isAlive()) {
			if (isvanillacritical == true) {
				if ((sourceentity instanceof net.minecraft.world.entity.LivingEntity _livEnt ? _livEnt.getMainHandItem() : net.minecraft.world.item.ItemStack.EMPTY)
						.getOrDefault((net.minecraft.core.component.DataComponentType<java.util.List<net.minecraft.resources.ResourceLocation>>) (Object) net.minecraft.core.registries.BuiltInRegistries.DATA_COMPONENT_TYPE
								.get(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("destinedtraits", "perks")), java.util.Collections.emptyList())
						.contains(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("destinedtraits", "rapid_hit")) == true) {
					if (java.util.Optional.of(sourceentity instanceof net.minecraft.world.entity.LivingEntity _livEnt ? _livEnt.getMainHandItem() : net.minecraft.world.item.ItemStack.EMPTY).map(item -> {
						net.minecraft.core.component.DataComponentType<java.util.List<net.minecraft.resources.ResourceLocation>> comp = (net.minecraft.core.component.DataComponentType<java.util.List<net.minecraft.resources.ResourceLocation>>) (Object) net.minecraft.core.registries.BuiltInRegistries.DATA_COMPONENT_TYPE
								.get(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("destinedtraits", "perks"));
						java.util.List<net.minecraft.resources.ResourceLocation> perks = item.getOrDefault(comp, java.util.Collections.emptyList());
						int index = perks.indexOf(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("destinedtraits", "rapid_hit"));
						if (index == -1)
							return false; // The weapon does not have the perk at all
						int tier = item.getOrDefault(net.minecraft.core.component.DataComponents.CUSTOM_DATA, net.minecraft.world.item.component.CustomData.EMPTY).copyTag().getInt("tier");
						// Returns true if left perk is tier 3+, or right perk is tier 4
						return WeaponPerkUtils.isEnhancedAtIndex(item, index);
					}).orElse(false)) {
						enhancedbonus = 2;
						duration = 50;
					} else {
						enhancedbonus = 1;
						duration = 35;
					}
					duration = MasterworkUtils.getStats((net.minecraft.world.entity.player.Player) sourceentity).scaleDuration((int) duration);
					if (sourceentity instanceof LivingEntity _livEnt1 && _livEnt1.hasEffect(DestinedtraitsModMobEffects.RAPID_HIT_EFFECT)) {
						amplifier = Mth.clamp((sourceentity instanceof LivingEntity _livEnt && _livEnt.hasEffect(DestinedtraitsModMobEffects.RAPID_HIT_EFFECT) ? _livEnt.getEffect(DestinedtraitsModMobEffects.RAPID_HIT_EFFECT).getAmplifier() : 0)
								+ 1 * enhancedbonus, 0, 4);
						if (sourceentity instanceof LivingEntity _entity && !_entity.level().isClientSide())
							_entity.addEffect(new MobEffectInstance(DestinedtraitsModMobEffects.RAPID_HIT_EFFECT, (int) duration, (int) amplifier, true, true));
					} else {
						if (sourceentity instanceof LivingEntity _entity && !_entity.level().isClientSide())
							_entity.addEffect(new MobEffectInstance(DestinedtraitsModMobEffects.RAPID_HIT_EFFECT, (int) duration, 0, true, true));
					}
				}
			}
		}
	}
}