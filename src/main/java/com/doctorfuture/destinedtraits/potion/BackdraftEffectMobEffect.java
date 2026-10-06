package com.doctorfuture.destinedtraits.potion;

import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.resources.ResourceLocation;

import com.doctorfuture.destinedtraits.DestinedtraitsMod;

public class BackdraftEffectMobEffect extends MobEffect {
	public BackdraftEffectMobEffect() {
		super(MobEffectCategory.BENEFICIAL, -16751002);
		this.addAttributeModifier(Attributes.ARMOR_TOUGHNESS, ResourceLocation.fromNamespaceAndPath(DestinedtraitsMod.MODID, "effect.backdraft_effect_0"), 2.5, AttributeModifier.Operation.ADD_VALUE);
	}
}