package com.doctorfuture.destinedtraits.init;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import com.doctorfuture.destinedtraits.DestinedtraitsMod;

public final class MasterworkAuxEffects {
    public static final DeferredRegister<MobEffect> REGISTRY = DeferredRegister.create(Registries.MOB_EFFECT, DestinedtraitsMod.MODID);
    public static final DeferredHolder<MobEffect, MobEffect> MAGMA_SKIN = REGISTRY.register(
            "magma_skin_masterwork_effect", () -> new MobEffect(MobEffectCategory.BENEFICIAL, 0xD96B26) {});
    public static final DeferredHolder<MobEffect, MobEffect> REPULSOR_BRACE = REGISTRY.register(
            "repulsor_brace_masterwork_effect", () -> new MobEffect(MobEffectCategory.BENEFICIAL, 0xD9D957) {});

    private MasterworkAuxEffects() {}

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(DestinedtraitsMod.MODID, path);
    }
}
