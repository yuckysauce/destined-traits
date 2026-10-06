package com.doctorfuture.destinedtraits.procedures;

import net.neoforged.neoforge.event.entity.player.CriticalHitEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.bus.api.Event;

import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.Entity;

import javax.annotation.Nullable;

import java.util.Comparator;
import com.doctorfuture.destinedtraits.perks.MasterworkUtils;

@EventBusSubscriber
public class FiringLineProcedure {
	@SubscribeEvent
	public static void onPlayerCriticalHit(CriticalHitEvent event) {
		execute(event, event.getEntity().level(), event.getEntity().getX(), event.getEntity().getY(), event.getEntity().getZ(), event.getEntity(), event.isVanillaCritical(), event.getDamageMultiplier());
	}

	public static void execute(LevelAccessor world, double x, double y, double z, Entity sourceentity, boolean isvanillacritical, double damagemodifier) {
		execute(null, world, x, y, z, sourceentity, isvanillacritical, damagemodifier);
	}

	private static void execute(@Nullable Event event, LevelAccessor world, double x, double y, double z, Entity sourceentity, boolean isvanillacritical, double damagemodifier) {
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
						.contains(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("destinedtraits", "firing_line")) == true) {
					if (java.util.Optional.of(sourceentity instanceof net.minecraft.world.entity.LivingEntity _livEnt ? _livEnt.getMainHandItem() : net.minecraft.world.item.ItemStack.EMPTY).map(item -> {
						net.minecraft.core.component.DataComponentType<java.util.List<net.minecraft.resources.ResourceLocation>> comp = (net.minecraft.core.component.DataComponentType<java.util.List<net.minecraft.resources.ResourceLocation>>) (Object) net.minecraft.core.registries.BuiltInRegistries.DATA_COMPONENT_TYPE
								.get(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("destinedtraits", "perks"));
						java.util.List<net.minecraft.resources.ResourceLocation> perks = item.getOrDefault(comp, java.util.Collections.emptyList());
						int index = perks.indexOf(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("destinedtraits", "firing_line"));
						if (index == -1)
							return false; // The weapon does not have the perk at all
						int tier = item.getOrDefault(net.minecraft.core.component.DataComponents.CUSTOM_DATA, net.minecraft.world.item.component.CustomData.EMPTY).copyTag().getInt("tier");
						// Returns true if left perk is tier 3+, or right perk is tier 4
						return WeaponPerkUtils.isEnhancedAtIndex(item, index);
					}).orElse(false)) {
						enhancedbonus = 0.35;
					} else {
						enhancedbonus = 0.25;
					}
					amplifier = 0;
					MasterworkUtils.Stats masterworks = MasterworkUtils.getStats((Player) sourceentity);
					{
						final Vec3 _center = new Vec3(x, y, z);
						for (Entity entityiterator : world.getEntitiesOfClass(Entity.class, new AABB(_center, _center).inflate(3 * masterworks.rangeMultiplier()), e -> true).stream().sorted(Comparator.comparingDouble(_entcnd -> _entcnd.distanceToSqr(_center))).toList()) {
							if (!(entityiterator == sourceentity) && entityiterator instanceof Player) {
								amplifier = amplifier + 1;
							}
						}
					}
					if (amplifier >= 0) {
						if (event instanceof CriticalHitEvent _event) {
							_event.setDamageMultiplier((float) (damagemodifier + enhancedbonus * masterworks.strengthMultiplier()));
						}
					}
				}
			}
		}
	}
}