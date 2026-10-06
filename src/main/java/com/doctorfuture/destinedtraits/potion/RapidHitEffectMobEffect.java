package com.doctorfuture.destinedtraits.potion;

import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.resources.ResourceLocation;

import com.doctorfuture.destinedtraits.DestinedtraitsMod;
import dev.shadowsoffire.apothic_attributes.api.ALObjects;

public class RapidHitEffectMobEffect extends MobEffect {
    public RapidHitEffectMobEffect() {
        super(MobEffectCategory.BENEFICIAL, -16751002);
        this.addAttributeModifier(Attributes.ATTACK_SPEED, ResourceLocation.fromNamespaceAndPath(DestinedtraitsMod.MODID, "effect.rapid_hit_effect_0"), 0.07, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
        this.addAttributeModifier(ALObjects.Attributes.DRAW_SPEED, ResourceLocation.fromNamespaceAndPath(DestinedtraitsMod.MODID, "effect.rapid_hit_effect_1"), 0.07, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
    }
}