package com.doctorfuture.destinedtraits.potion;

import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.resources.ResourceLocation;

import com.doctorfuture.destinedtraits.DestinedtraitsMod;

public class PulseMonitorEffectMobEffect extends MobEffect {
	public PulseMonitorEffectMobEffect() {
		super(MobEffectCategory.BENEFICIAL, -16751002);
		this.addAttributeModifier(Attributes.KNOCKBACK_RESISTANCE, ResourceLocation.fromNamespaceAndPath(DestinedtraitsMod.MODID, "effect.pulse_monitor_effect_0"), 0.12, AttributeModifier.Operation.ADD_VALUE);
	}
}