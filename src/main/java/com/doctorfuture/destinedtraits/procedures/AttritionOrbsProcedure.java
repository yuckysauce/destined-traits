package com.doctorfuture.destinedtraits.procedures;

import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.bus.api.Event;

import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.Entity;
import net.minecraft.util.RandomSource;
import net.minecraft.util.Mth;
import net.minecraft.server.level.ServerLevel;

import com.doctorfuture.destinedtraits.DestinedtraitsMod;

import javax.annotation.Nullable;

import java.util.function.Function;

@EventBusSubscriber
public class AttritionOrbsProcedure {
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
					.contains(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("destinedtraits", "attrition_orbs")) == true) {
				if (java.util.Optional.of(sourceentity instanceof net.minecraft.world.entity.LivingEntity _livEnt ? _livEnt.getMainHandItem() : net.minecraft.world.item.ItemStack.EMPTY).map(item -> {
					net.minecraft.core.component.DataComponentType<java.util.List<net.minecraft.resources.ResourceLocation>> comp = (net.minecraft.core.component.DataComponentType<java.util.List<net.minecraft.resources.ResourceLocation>>) (Object) net.minecraft.core.registries.BuiltInRegistries.DATA_COMPONENT_TYPE
							.get(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("destinedtraits", "perks"));
					java.util.List<net.minecraft.resources.ResourceLocation> perks = item.getOrDefault(comp, java.util.Collections.emptyList());
					int index = perks.indexOf(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("destinedtraits", "attrition_orbs"));
					if (index == -1)
						return false; // The weapon does not have the perk at all
					int tier = item.getOrDefault(net.minecraft.core.component.DataComponents.CUSTOM_DATA, net.minecraft.world.item.component.CustomData.EMPTY).copyTag().getInt("tier");
					// Returns true if left perk is tier 3+, or right perk is tier 4
					return WeaponPerkUtils.isEnhancedAtIndex(item, index);
				}).orElse(false)) {
					enhancedbonus = 3;
				} else {
					enhancedbonus = 2;
				}
				if (Mth.nextInt(RandomSource.create(), 1, 100) <= enhancedbonus * 10) {
					createTimedLoop(Mth.nextInt(RandomSource.create(), 1, (int) enhancedbonus), 1, _timedLoop -> {
						if (world instanceof ServerLevel _level)
							_level.addFreshEntity(new ExperienceOrb(_level, (entity.getX()), (entity.getY()), (entity.getZ()), 1));
						return true;
					});
				}
			}
		}
	}

	private static class DestinedtraitsModTimedLoop {
		private int iterator = 0;
		private int ticks;
		private final int total;
		private final Function<DestinedtraitsModTimedLoop, Boolean> forEach;

		private DestinedtraitsModTimedLoop(int total, int ticks, Function<DestinedtraitsModTimedLoop, Boolean> forEach) {
			this.total = total;
			this.ticks = ticks;
			this.forEach = forEach;
			run();
		}

		private void run() {
			if (forEach.apply(this))
				DestinedtraitsMod.queueServerWork(ticks, this::next);
		}

		private void next() {
			if (total > ++iterator)
				run();
		}
	}

	private static DestinedtraitsModTimedLoop createTimedLoop(int total, int ticks, Function<DestinedtraitsModTimedLoop, Boolean> forEach) {
		return new DestinedtraitsModTimedLoop(total, ticks, forEach);
	}
}