package com.doctorfuture.destinedtraits.procedures;

import net.neoforged.neoforge.event.entity.living.LivingEquipmentChangeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.bus.api.Event;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.effect.MobEffectInstance;

import com.doctorfuture.destinedtraits.network.DestinedtraitsModVariables;
import com.doctorfuture.destinedtraits.init.DestinedtraitsModMobEffects;
import com.doctorfuture.destinedtraits.perks.MasterworkUtils;

import javax.annotation.Nullable;

@EventBusSubscriber
public class HarmonyEffectGiverProcedure {
	@SubscribeEvent
	public static void whenEntityChangeEquipment(LivingEquipmentChangeEvent event) {
		execute(event, event.getEntity(), event.getSlot().getFilterFlag());
	}

	public static void execute(Entity entity, double equipmentslot) {
		execute(null, entity, equipmentslot);
	}

	private static void execute(@Nullable Event event, Entity entity, double equipmentslot) {
		if (entity == null)
			return;
		double amplifier = 0;
		double enhancedbonus = 0;
		double duration = 0;
		if (entity instanceof Player) {
			if (equipmentslot == 0) {
				if ((entity instanceof net.minecraft.world.entity.LivingEntity _livEnt ? _livEnt.getMainHandItem() : net.minecraft.world.item.ItemStack.EMPTY)
						.getOrDefault((net.minecraft.core.component.DataComponentType<java.util.List<net.minecraft.resources.ResourceLocation>>) (Object) net.minecraft.core.registries.BuiltInRegistries.DATA_COMPONENT_TYPE
								.get(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("destinedtraits", "perks")), java.util.Collections.emptyList())
						.contains(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("destinedtraits", "harmony")) == true) {
					if (entity.getData(DestinedtraitsModVariables.PLAYER_VARIABLES).harmony_ready) {
						if (entity instanceof net.minecraft.world.entity.player.Player _player && java.util.stream.Stream.concat(_player.getInventory().items.stream(), _player.getInventory().offhand.stream()).anyMatch(item -> {
							// Skips empty slots and skips the weapon currently held in the main hand
							if (item.isEmpty() || item == _player.getMainHandItem())
								return false;
							net.minecraft.core.component.DataComponentType<java.util.List<net.minecraft.resources.ResourceLocation>> comp = (net.minecraft.core.component.DataComponentType<java.util.List<net.minecraft.resources.ResourceLocation>>) (Object) net.minecraft.core.registries.BuiltInRegistries.DATA_COMPONENT_TYPE
									.get(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("destinedtraits", "perks"));
							java.util.List<net.minecraft.resources.ResourceLocation> perks = item.getOrDefault(comp, java.util.Collections.emptyList());
							int index = perks.indexOf(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("destinedtraits", "harmony"));
							if (index == -1)
								return false; // The weapon does not have the perk at all
							int tier = item.getOrDefault(net.minecraft.core.component.DataComponents.CUSTOM_DATA, net.minecraft.world.item.component.CustomData.EMPTY).copyTag().getInt("tier");
							// Returns true if left perk is tier 3+, or right perk is tier 4
							return WeaponPerkUtils.isEnhancedAtIndex(item, index);
						})) {
							enhancedbonus = 90;
						} else {
							enhancedbonus = 60;
						}
						MasterworkUtils.Stats masterworks = MasterworkUtils.getStats((Player) entity);
						if (entity instanceof LivingEntity _entity && !_entity.level().isClientSide())
							_entity.addEffect(new MobEffectInstance(DestinedtraitsModMobEffects.HARMONY_EFFECT, masterworks.scaleDuration((int) enhancedbonus), 0));
						{
							DestinedtraitsModVariables.PlayerVariables _vars = entity.getData(DestinedtraitsModVariables.PLAYER_VARIABLES);
							_vars.harmony_ready = false;
							_vars.markSyncDirty();
						}
					}
				}
			}
		}
	}
}