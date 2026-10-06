package com.doctorfuture.destinedtraits.procedures;

import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
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
public class InCombatCheckDamageProcedure {
	@SubscribeEvent
	public static void onEntityAttacked(LivingIncomingDamageEvent event) {
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
		if (!world.isClientSide()) {
			if (sourceentity instanceof Player) {
				if (sourceentity.getData(DestinedtraitsModVariables.PLAYER_VARIABLES).in_combat <= 0) {
					{
						DestinedtraitsModVariables.PlayerVariables _vars = sourceentity.getData(DestinedtraitsModVariables.PLAYER_VARIABLES);
						_vars.in_combat_timer = 100;
						_vars.markSyncDirty();
					}
				}
				{
					DestinedtraitsModVariables.PlayerVariables _vars = sourceentity.getData(DestinedtraitsModVariables.PLAYER_VARIABLES);
					_vars.in_combat = Mth.clamp(sourceentity.getData(DestinedtraitsModVariables.PLAYER_VARIABLES).in_combat + 1, 0, 2);
					_vars.markSyncDirty();
				}
			}
		}
	}
}