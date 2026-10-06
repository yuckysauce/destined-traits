package com.doctorfuture.destinedtraits.procedures;

import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.bus.api.Event;

import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.entity.Entity;

import com.doctorfuture.destinedtraits.network.DestinedtraitsModVariables;

import javax.annotation.Nullable;

@EventBusSubscriber
public class TimersProcedure {
	@SubscribeEvent
	public static void onPlayerTick(PlayerTickEvent.Post event) {
		execute(event, event.getEntity().level(), event.getEntity());
	}

	public static void execute(LevelAccessor world, Entity entity) {
		execute(null, world, entity);
	}

	private static void execute(@Nullable Event event, LevelAccessor world, Entity entity) {
		if (entity == null)
			return;
		if (!world.isClientSide()) {
			DestinedtraitsModVariables.PlayerVariables variables = entity.getData(DestinedtraitsModVariables.PLAYER_VARIABLES);
			if (variables.rapidkill_timer > 0) {
				variables.rapidkill_timer--;
				variables.markSyncDirty();
				if (variables.rapidkill_timer <= 0) {
					variables.rapidkill_timer = 0;
					variables.rapidkill_progress = 0;
					variables.markSyncDirty();
				}
			}
			if (variables.rapidhit_timer > 0) {
				variables.rapidhit_timer--;
				variables.markSyncDirty();
				if (variables.rapidhit_timer <= 0) {
					variables.rapidhit_timer = 0;
					variables.rapidhit_progress = 0;
					variables.markSyncDirty();
				}
			}
			if (variables.enderartillery_timer > 0) {
				variables.enderartillery_timer--;
				variables.markSyncDirty();
				if (variables.enderartillery_timer <= 0) {
					variables.enderartillery_timer = 0;
					variables.enderartillery_progress = 0;
					variables.markSyncDirty();
				}
			}
			if (variables.fieldforged_timer > 0) {
				variables.fieldforged_timer--;
				variables.markSyncDirty();
				if (variables.fieldforged_timer <= 0) {
					variables.fieldforged_timer = 0;
					variables.markSyncDirty();
				}
			}
			if (variables.eageredge_timer > 0) {
				if ((entity instanceof net.minecraft.world.entity.LivingEntity _livEnt ? _livEnt.getMainHandItem() : net.minecraft.world.item.ItemStack.EMPTY)
						.getOrDefault((net.minecraft.core.component.DataComponentType<java.util.List<net.minecraft.resources.ResourceLocation>>) (Object) net.minecraft.core.registries.BuiltInRegistries.DATA_COMPONENT_TYPE
								.get(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("destinedtraits", "perks")), java.util.Collections.emptyList())
						.contains(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("destinedtraits", "eager_edge")) == false) {
					{
						variables.eageredge_timer--;
						variables.markSyncDirty();
					}
					if (variables.eageredge_timer <= 0) {
						variables.eageredge_timer = 0;
						variables.markSyncDirty();
					}
				}
			}
			if (variables.in_combat > 0) {
				variables.in_combat_timer--;
				variables.markSyncDirty();
				if (variables.in_combat_timer <= 0) {
					if (variables.in_combat >= 1) {
						if (variables.in_combat == 2) {
							variables.in_combat_bool = true;
							variables.markSyncDirty();
						}
						variables.in_combat_timer = 100;
						variables.in_combat--;
						variables.markSyncDirty();
						if (variables.in_combat == 0) {
							variables.in_combat_bool = false;
							variables.markSyncDirty();
						}
					}
				}
			}
		}
	}
}