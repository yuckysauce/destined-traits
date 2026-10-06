package com.doctorfuture.destinedtraits.procedures;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

@EventBusSubscriber(modid = "destinedtraits")
public final class SecondChanceProcedure {
    private static final ResourceLocation PERK = ResourceLocation.fromNamespaceAndPath("destinedtraits", "second_chance");
    private static final String COOLDOWN = "destinedtraits_armadan_cooldown";
    private static final int COOLDOWN_TICKS = 200;

    private SecondChanceProcedure() {}

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (player.level().isClientSide() || !ExoticEffectUtils.hasArmorPerk(player, PERK)) return;
        var data = player.getPersistentData();
        int cooldown = data.getInt(COOLDOWN);
        if (cooldown > 0) {
            data.putInt(COOLDOWN, cooldown - 1);
            return;
        }
        boolean highFall = player.fallDistance >= 12.0F;
        boolean overVoid = player.getY() < player.level().getMinBuildHeight() + 8.0;
        if (player.onGround() || player.getDeltaMovement().y >= 0.0 || (!highFall && !overVoid)) return;
        player.addEffect(new MobEffectInstance(MobEffects.LEVITATION, 40, 0, true, true, true));
        player.fallDistance = 0.0F;
        data.putInt(COOLDOWN, COOLDOWN_TICKS);
        player.level().playSound(null, player.blockPosition(), SoundEvents.SHULKER_SHOOT,
                SoundSource.PLAYERS, 0.7F, 1.3F);
    }
}