package com.doctorfuture.destinedtraits.potion;

import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.resources.ResourceLocation;

import com.doctorfuture.destinedtraits.DestinedtraitsMod;
import dev.shadowsoffire.apothic_attributes.api.ALObjects;

public class LooseChangeEffectMobEffect extends MobEffect {
	public LooseChangeEffectMobEffect() {
		super(MobEffectCategory.BENEFICIAL, -16751002);
		this.addAttributeModifier(Attributes.ATTACK_SPEED, ResourceLocation.fromNamespaceAndPath(DestinedtraitsMod.MODID, "effect.loose_change_effect_0"), 0.5, AttributeModifier.Operation.ADD_VALUE);
		this.addAttributeModifier(Attributes.MOVEMENT_SPEED, ResourceLocation.fromNamespaceAndPath(DestinedtraitsMod.MODID, "effect.loose_change_effect_1"), 0.05, AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
		this.addAttributeModifier(ALObjects.Attributes.ARROW_VELOCITY, ResourceLocation.fromNamespaceAndPath(DestinedtraitsMod.MODID, "effect.loose_change_effect_2"), 0.5, AttributeModifier.Operation.ADD_VALUE);
	}
}