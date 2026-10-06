package com.doctorfuture.destinedtraits.perks;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.resources.ResourceLocation;
import com.doctorfuture.destinedtraits.init.MasterworkAuxEffects;
import com.doctorfuture.destinedtraits.procedures.WeaponPerkUtils;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

@EventBusSubscriber(modid = "destinedtraits")
public final class MasterworkTriggeredEffectsHandler {
    public static final String MAGMA_PULSE = "destinedtraits_magma_skin_pulse";
    private static final String REPULSOR_BONUS = "destinedtraits_repulsor_bonus";

    private MasterworkTriggeredEffectsHandler() {}

    public static void grantRepulsorAbsorption(ServerPlayer player, int amplifier, MasterworkUtils.Stats stats) {
        CompoundTag data = player.getPersistentData();
        double previousBonus = data.getDouble(REPULSOR_BONUS);
        if (previousBonus > 0.0) {
            player.setAbsorptionAmount((float) Math.max(0.0, player.getAbsorptionAmount() - previousBonus));
        }

        double bonus = 4.0 * (amplifier + 1) * stats.strengthMultiplier();
        player.setAbsorptionAmount((float) (player.getAbsorptionAmount() + bonus));
        data.putDouble(REPULSOR_BONUS, bonus);
        player.addEffect(new MobEffectInstance(MasterworkAuxEffects.REPULSOR_BRACE, stats.scaleDuration(180), amplifier, true, false, false));
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        CompoundTag data = player.getPersistentData();
        tickMagmaSkin(player, data);
        tickRepulsor(player, data);
    }

    private static void tickMagmaSkin(ServerPlayer player, CompoundTag data) {
        MobEffectInstance effect = player.getEffect(MasterworkAuxEffects.MAGMA_SKIN);
        if (effect == null) {
            data.remove(MAGMA_PULSE);
            return;
        }

        boolean enhanced = effect.getAmplifier() > 0;
        int pulse = data.getInt(MAGMA_PULSE) + 1;
        int pulseInterval = MasterworkUtils.getStats(player).scaleEfficiencyCooldown(enhanced ? 25 : 50);
        if (pulse >= pulseInterval) {
            double traitMultiplier = WeaponPerkUtils.getOriginArmorMultiplier(player,
                    ResourceLocation.fromNamespaceAndPath("destinedtraits", "magma_skin"));
            player.heal((float) (MasterworkUtils.getStats(player).strengthMultiplier() * traitMultiplier));
            pulse = 0;
        }

        data.putInt(MAGMA_PULSE, pulse);
    }

    private static void tickRepulsor(ServerPlayer player, CompoundTag data) {
        if (!player.hasEffect(MasterworkAuxEffects.REPULSOR_BRACE)) {
            double bonus = data.getDouble(REPULSOR_BONUS);
            player.setAbsorptionAmount((float) Math.max(0.0, player.getAbsorptionAmount() - bonus));
            data.remove(REPULSOR_BONUS);
        }
    }
}
