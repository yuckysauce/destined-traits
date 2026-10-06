package com.doctorfuture.destinedtraits.procedures;

import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.resources.ResourceKey;
import net.minecraft.core.registries.Registries;
import com.doctorfuture.destinedtraits.perks.MasterworkUtils;

public class FlowingVitreumEffectDamageProcedure {
	public static void execute(LevelAccessor world, Entity entity, double amplifier) {
		execute(world, entity, amplifier, null);
	}

	public static void execute(LevelAccessor world, Entity entity, double amplifier, Entity source) {
		if (entity == null)
			return;
		if (!(entity instanceof Player)) {
			double strength = source instanceof Player player ? MasterworkUtils.getStats(player).strengthMultiplier() : 1.0;
			entity.hurt(new DamageSource(world.holderOrThrow(ResourceKey.create(Registries.DAMAGE_TYPE, ResourceLocation.parse("destinedtraits:glass")))), (float) (2 * amplifier * strength));
		}
	}
}