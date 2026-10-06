package com.doctorfuture.destinedtraits.potion;

import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.resources.ResourceLocation;

import com.doctorfuture.destinedtraits.DestinedtraitsMod;

public class KillingWindEffectMobEffect extends MobEffect {
	public KillingWindEffectMobEffect() {
		super(MobEffectCategory.BENEFICIAL, -16751002);
		this.addAttributeModifier(Attributes.MOVEMENT_SPEED, ResourceLocation.fromNamespaceAndPath(DestinedtraitsMod.MODID, "effect.killing_wind_effect_0"), 0.07, AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
	}
}