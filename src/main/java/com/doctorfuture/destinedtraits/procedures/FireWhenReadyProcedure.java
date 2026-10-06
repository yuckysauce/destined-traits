package com.doctorfuture.destinedtraits.procedures;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;

@EventBusSubscriber(modid = "destinedtraits")
public final class FireWhenReadyProcedure {
    private static final ResourceLocation PERK = ResourceLocation.fromNamespaceAndPath("destinedtraits", "fire_when_ready");

    private FireWhenReadyProcedure() {}

    @SubscribeEvent
    public static void onExplosionDamageTaken(LivingDamageEvent.Pre event) {
        if (!(event.getEntity() instanceof Player player) || player.level().isClientSide()
                || !isSelfExplosion(event.getSource(), player) || !ExoticEffectUtils.hasArmorPerk(player, PERK)) return;
        event.setNewDamage(event.getNewDamage() * 0.1F);
    }

    @SubscribeEvent
    public static void onExplosionDamageResolved(LivingDamageEvent.Post event) {
        if (!(event.getEntity() instanceof Player player) || player.level().isClientSide()
                || !event.getSource().is(DamageTypeTags.IS_EXPLOSION)
                || (event.getSource().getEntity() != player && event.getSource().getDirectEntity() != player)
                || !ExoticEffectUtils.hasArmorPerk(player, PERK)) return;
        player.getFoodData().eat(Math.max(1, (int) (event.getOriginalDamage() / 8.0F)), 0.2F);
    }

    private static boolean isSelfExplosion(DamageSource source, Player player) {
        return source.is(DamageTypeTags.IS_EXPLOSION)
                && (source.getEntity() == player || source.getDirectEntity() == player);
    }
}