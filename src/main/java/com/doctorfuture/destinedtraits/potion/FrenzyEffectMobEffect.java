package com.doctorfuture.destinedtraits.potion;

import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.resources.ResourceLocation;

import com.doctorfuture.destinedtraits.DestinedtraitsMod;
import dev.shadowsoffire.apothic_attributes.api.ALObjects;

public class FrenzyEffectMobEffect extends MobEffect {
	public FrenzyEffectMobEffect() {
		super(MobEffectCategory.BENEFICIAL, -16751002);
		this.addAttributeModifier(Attributes.ATTACK_DAMAGE, ResourceLocation.fromNamespaceAndPath(DestinedtraitsMod.MODID, "effect.frenzy_effect_0"), 0.5, AttributeModifier.Operation.ADD_VALUE);
		this.addAttributeModifier(ALObjects.Attributes.ARROW_DAMAGE, ResourceLocation.fromNamespaceAndPath(DestinedtraitsMod.MODID, "effect.frenzy_effect_1"), 0.5, AttributeModifier.Operation.ADD_VALUE);
	}
}