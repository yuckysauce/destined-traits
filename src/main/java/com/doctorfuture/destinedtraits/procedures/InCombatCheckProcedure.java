package com.doctorfuture.destinedtraits.procedures;

import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.bus.api.Event;

import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.Entity;
import net.minecraft.util.Mth;

import javax.annotation.Nullable;

import com.doctorfuture.destinedtraits.network.DestinedtraitsModVariables;

@EventBusSubscriber
public class InCombatCheckProcedure {
	@SubscribeEvent
	public static void onEntityAttacked(LivingDamageEvent.Pre event) {
		if (event.getEntity() != null) {
			execute(event, event.getEntity().level(), event.getEntity());
		}
	}

	public static void execute(LevelAccessor world, Entity entity) {
		execute(null, world, entity);
	}

	private static void execute(@Nullable Event event, LevelAccessor world, Entity entity) {
		if (entity == null)
			return;
		if (!world.isClientSide()) {
			if (entity instanceof Player) {
				if (entity.getData(DestinedtraitsModVariables.PLAYER_VARIABLES).in_combat <= 0) {
					{
						DestinedtraitsModVariables.PlayerVariables _vars = entity.getData(DestinedtraitsModVariables.PLAYER_VARIABLES);
						_vars.in_combat_timer = 100;
						_vars.markSyncDirty();
					}
				}
				{
					DestinedtraitsModVariables.PlayerVariables _vars = entity.getData(DestinedtraitsModVariables.PLAYER_VARIABLES);
					_vars.in_combat = Mth.clamp(entity.getData(DestinedtraitsModVariables.PLAYER_VARIABLES).in_combat + 1, 0, 2);
					_vars.markSyncDirty();
				}
			}
		}
	}
}