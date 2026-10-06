package com.doctorfuture.destinedtraits.potion;

import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.resources.ResourceLocation;

import com.doctorfuture.destinedtraits.DestinedtraitsMod;
import dev.shadowsoffire.apothic_attributes.api.ALObjects;

public class SurroundedEffectMobEffect extends MobEffect {
	public SurroundedEffectMobEffect() {
		super(MobEffectCategory.BENEFICIAL, -16751002);
		this.addAttributeModifier(Attributes.ATTACK_DAMAGE, ResourceLocation.fromNamespaceAndPath(DestinedtraitsMod.MODID, "effect.surrounded_effect_0"), 0.2, AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
		this.addAttributeModifier(ALObjects.Attributes.ARROW_DAMAGE, ResourceLocation.fromNamespaceAndPath(DestinedtraitsMod.MODID, "effect.surrounded_1"), 0.2, AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
	}
}