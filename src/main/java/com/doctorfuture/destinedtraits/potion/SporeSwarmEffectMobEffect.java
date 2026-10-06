package com.doctorfuture.destinedtraits.potion;

import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.ParticleOptions;

import com.doctorfuture.destinedtraits.procedures.SporeSwarmEffectOnEffectActiveTickProcedure;
import com.doctorfuture.destinedtraits.DestinedtraitsMod;

public class SporeSwarmEffectMobEffect extends MobEffect {
	public SporeSwarmEffectMobEffect() {
		super(MobEffectCategory.HARMFUL, -16751002);
		this.addAttributeModifier(Attributes.KNOCKBACK_RESISTANCE, ResourceLocation.fromNamespaceAndPath(DestinedtraitsMod.MODID, "effect.spore_swarm_effect_0"), 0.12, AttributeModifier.Operation.ADD_VALUE);
	}

	@Override
	public ParticleOptions createParticleOptions(MobEffectInstance mobEffectInstance) {
		return ParticleTypes.SPORE_BLOSSOM_AIR;
	}

	@Override
	public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
		return true;
	}

	@Override
	public boolean applyEffectTick(LivingEntity entity, int amplifier) {
		SporeSwarmEffectOnEffectActiveTickProcedure.execute(entity.level(), entity.getX(), entity.getY(), entity.getZ());
		return super.applyEffectTick(entity, amplifier);
	}
}