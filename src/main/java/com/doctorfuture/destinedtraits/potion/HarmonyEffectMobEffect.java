package com.doctorfuture.destinedtraits.potion;

import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.resources.ResourceLocation;

import com.doctorfuture.destinedtraits.DestinedtraitsMod;
import dev.shadowsoffire.apothic_attributes.api.ALObjects;

public class HarmonyEffectMobEffect extends MobEffect {
	public HarmonyEffectMobEffect() {
		super(MobEffectCategory.BENEFICIAL, -16751002);
		this.addAttributeModifier(Attributes.ATTACK_SPEED, ResourceLocation.fromNamespaceAndPath(DestinedtraitsMod.MODID, "effect.harmony_effect_0"), 0.2, AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
		this.addAttributeModifier(Attributes.ENTITY_INTERACTION_RANGE, ResourceLocation.fromNamespaceAndPath(DestinedtraitsMod.MODID, "effect.harmony_effect_1"), 0.4, AttributeModifier.Operation.ADD_VALUE);
		this.addAttributeModifier(ALObjects.Attributes.ARROW_DAMAGE, ResourceLocation.fromNamespaceAndPath(DestinedtraitsMod.MODID, "effect.harmony_effect_2"), 0.2, AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
		this.addAttributeModifier(ALObjects.Attributes.ARROW_VELOCITY, ResourceLocation.fromNamespaceAndPath(DestinedtraitsMod.MODID, "effect.harmony_effect_3"), 0.4, AttributeModifier.Operation.ADD_VALUE);
	}
}