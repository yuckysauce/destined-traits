package com.doctorfuture.destinedtraits.procedures;

import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.bus.api.Event;

import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.Entity;
import net.minecraft.server.level.ServerLevel;

import com.doctorfuture.destinedtraits.init.DestinedtraitsModMobEffects;
import com.doctorfuture.destinedtraits.perks.MasterworkUtils;

import javax.annotation.Nullable;

@EventBusSubscriber
public class ReaperSpawnorbProcedure {
	@SubscribeEvent
	public static void onEntityDeath(LivingDeathEvent event) {
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
			if (sourceentity instanceof LivingEntity _livEnt1 && _livEnt1.hasEffect(DestinedtraitsModMobEffects.REAPER_EFFECT)) {
				MasterworkUtils.Stats masterworks = MasterworkUtils.getStats((Player) sourceentity);
				int stacks = (sourceentity instanceof LivingEntity _livEnt && _livEnt.hasEffect(DestinedtraitsModMobEffects.REAPER_EFFECT)
						? _livEnt.getEffect(DestinedtraitsModMobEffects.REAPER_EFFECT).getAmplifier() : 0) + 1;
				int xpAmount = Math.max(1, (int) Math.round(5 * stacks * masterworks.strengthMultiplier()));
				if (world instanceof ServerLevel _level)
					_level.addFreshEntity(new ExperienceOrb(_level, (entity.getX()), (entity.getY()), (entity.getZ()), xpAmount));
				if (sourceentity instanceof LivingEntity _entity)
					_entity.removeEffect(DestinedtraitsModMobEffects.REAPER_EFFECT);
			}
		}
	}
}