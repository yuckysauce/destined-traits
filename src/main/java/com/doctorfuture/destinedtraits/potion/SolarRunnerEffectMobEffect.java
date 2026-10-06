package com.doctorfuture.destinedtraits.potion;

import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.resources.ResourceLocation;

import com.doctorfuture.destinedtraits.DestinedtraitsMod;

public class SolarRunnerEffectMobEffect extends MobEffect {
	public SolarRunnerEffectMobEffect() {
		super(MobEffectCategory.BENEFICIAL, -16751002);
		this.addAttributeModifier(Attributes.MOVEMENT_SPEED, ResourceLocation.fromNamespaceAndPath(DestinedtraitsMod.MODID, "effect.solar_runner_effect_0"), 0.1, AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
	}
}