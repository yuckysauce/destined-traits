package com.doctorfuture.destinedtraits.potion;

import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.resources.ResourceLocation;

import dev.shadowsoffire.apothic_attributes.api.ALObjects;
import com.doctorfuture.destinedtraits.DestinedtraitsMod;

public class InTheDeepEffectMobEffect extends MobEffect {
	public InTheDeepEffectMobEffect() {
		super(MobEffectCategory.BENEFICIAL, -16751002);
		this.addAttributeModifier(Attributes.ENTITY_INTERACTION_RANGE, ResourceLocation.fromNamespaceAndPath(DestinedtraitsMod.MODID, "effect.in_the_deep_effect_0"), 0.5, AttributeModifier.Operation.ADD_VALUE);
		this.addAttributeModifier(ALObjects.Attributes.ARROW_VELOCITY, ResourceLocation.fromNamespaceAndPath(DestinedtraitsMod.MODID, "effect.in_the_deep_effect_1"), 0.5, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
	}
}