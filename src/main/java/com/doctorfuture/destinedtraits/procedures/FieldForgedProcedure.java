package com.doctorfuture.destinedtraits.procedures;

import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.bus.api.Event;

import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.Entity;

import com.doctorfuture.destinedtraits.network.DestinedtraitsModVariables;
import com.doctorfuture.destinedtraits.perks.MasterworkUtils;

import javax.annotation.Nullable;

@EventBusSubscriber
public class FieldForgedProcedure {
	@SubscribeEvent
	public static void onEntityDeath(LivingDeathEvent event) {
		if (event.getEntity() != null) {
			execute(event, event.getEntity().level(), event.getSource().getEntity());
		}
	}

	public static void execute(LevelAccessor world, Entity sourceentity) {
		execute(null, world, sourceentity);
	}

	private static void execute(@Nullable Event event, LevelAccessor world, Entity sourceentity) {
		if (sourceentity == null)
			return;
		double amplifier = 0;
		double enhancedbonus = 0;
		if (!world.isClientSide()) {
			if (sourceentity instanceof Player) {
				if ((sourceentity instanceof net.minecraft.world.entity.LivingEntity _livEnt ? _livEnt.getMainHandItem() : net.minecraft.world.item.ItemStack.EMPTY)
						.getOrDefault((net.minecraft.core.component.DataComponentType<java.util.List<net.minecraft.resources.ResourceLocation>>) (Object) net.minecraft.core.registries.BuiltInRegistries.DATA_COMPONENT_TYPE
								.get(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("destinedtraits", "perks")), java.util.Collections.emptyList())
						.contains(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("destinedtraits", "field_forged")) == true) {
					if (java.util.Optional.of(sourceentity instanceof net.minecraft.world.entity.LivingEntity _livEnt ? _livEnt.getMainHandItem() : net.minecraft.world.item.ItemStack.EMPTY).map(item -> {
						net.minecraft.core.component.DataComponentType<java.util.List<net.minecraft.resources.ResourceLocation>> comp = (net.minecraft.core.component.DataComponentType<java.util.List<net.minecraft.resources.ResourceLocation>>) (Object) net.minecraft.core.registries.BuiltInRegistries.DATA_COMPONENT_TYPE
								.get(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("destinedtraits", "perks"));
						java.util.List<net.minecraft.resources.ResourceLocation> perks = item.getOrDefault(comp, java.util.Collections.emptyList());
						int index = perks.indexOf(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("destinedtraits", "field_forged"));
						if (index == -1)
							return false; // The weapon does not have the perk at all
						int tier = item.getOrDefault(net.minecraft.core.component.DataComponents.CUSTOM_DATA, net.minecraft.world.item.component.CustomData.EMPTY).copyTag().getInt("tier");
						return WeaponPerkUtils.isOriginEnhanced(item, net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("destinedtraits", "field_forged"));
					}).orElse(false)) {
						enhancedbonus = 2;
					} else {
						enhancedbonus = 1;
					}
					MasterworkUtils.Stats masterworks = MasterworkUtils.getStats((Player) sourceentity);
					{
						DestinedtraitsModVariables.PlayerVariables _vars = sourceentity.getData(DestinedtraitsModVariables.PLAYER_VARIABLES);
						_vars.fieldforged_progress = sourceentity.getData(DestinedtraitsModVariables.PLAYER_VARIABLES).fieldforged_progress + 35 * masterworks.efficiencyMultiplier();
						_vars.fieldforged_timer = masterworks.scaleDuration(100);
						_vars.markSyncDirty();
					}
					if (sourceentity.getData(DestinedtraitsModVariables.PLAYER_VARIABLES).fieldforged_progress >= masterworks.scaleEfficiencyCost(100)) {
						if (sourceentity instanceof Player _player)
							_player.getFoodData().setFoodLevel((int) ((sourceentity instanceof Player _plr ? _plr.getFoodData().getFoodLevel() : 0)
									+ Math.max(1, Math.round((float) (enhancedbonus * masterworks.strengthMultiplier())))));
						{
							DestinedtraitsModVariables.PlayerVariables _vars = sourceentity.getData(DestinedtraitsModVariables.PLAYER_VARIABLES);
							_vars.fieldforged_progress = 0;
							_vars.fieldforged_timer = 0;
							_vars.markSyncDirty();
						}
					}
				}
			}
		}
	}
}