package com.doctorfuture.destinedtraits.potion;

import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.resources.ResourceLocation;

import dev.shadowsoffire.apothic_attributes.api.ALObjects;
import com.doctorfuture.destinedtraits.DestinedtraitsMod;

public class ForsakenRiteEffectMobEffect extends MobEffect {
	public ForsakenRiteEffectMobEffect() {
		super(MobEffectCategory.BENEFICIAL, -16751002);
		this.addAttributeModifier(Attributes.ATTACK_SPEED, ResourceLocation.fromNamespaceAndPath(DestinedtraitsMod.MODID, "effect.forsaken_rite_effect_0"), 0.2, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
		this.addAttributeModifier(ALObjects.Attributes.DRAW_SPEED, ResourceLocation.fromNamespaceAndPath(DestinedtraitsMod.MODID, "effect.forsaken_rite_effect_1"), 0.2, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
	}
}