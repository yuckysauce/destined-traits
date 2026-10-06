package com.doctorfuture.destinedtraits.procedures;

import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import com.doctorfuture.destinedtraits.perks.MasterworkUtils;
import com.doctorfuture.destinedtraits.perks.MasterworkTriggeredEffectsHandler;
import com.doctorfuture.destinedtraits.init.MasterworkAuxEffects;

@EventBusSubscriber
public class MagmaSkinProcedure {

    // Helper class to store the results of our equipment scan
    private static class PerkData {
        public int totalPieces = 0;
        public boolean hasEnhanced = false;
    }

    // Helper method to scan armor and offhand for the magma_skin perk
    private static PerkData getPerkData(LivingEntity livingEntity) {
        PerkData data = new PerkData();
        ResourceLocation magmaSkin = ResourceLocation.fromNamespaceAndPath("destinedtraits", "magma_skin");
        data.totalPieces = WeaponPerkUtils.countOriginArmorPieces(livingEntity, magmaSkin);
        data.hasEnhanced = WeaponPerkUtils.hasEnhancedOriginArmor(livingEntity, magmaSkin);
        return data;
    }

    // =================================================================================
    // EVENT: GRANTS REGENERATION WHEN TAKING FIRE DAMAGE
    // =================================================================================
    @SubscribeEvent
    public static void onFireDamageTaken(LivingDamageEvent.Pre event) {
        // event.getEntity() is the entity RECEIVING the damage
        if (event.getEntity() instanceof Player player) {
            
            // Only run on the server side to prevent ghost effects
            if (player.level().isClientSide()) return;
            
            // Check if the damage type is fire-based (fire, lava, magma, etc.)
            if (event.getSource().is(DamageTypeTags.IS_FIRE)) {
                
                PerkData perkData = getPerkData(player);
                
                if (perkData.totalPieces > 0) {
                    MasterworkUtils.Stats masterworks = MasterworkUtils.getStats(player);
                    int effectDuration = Math.max(1, (int) Math.round(60 * (perkData.totalPieces / 2.0)));
                    player.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                        MasterworkAuxEffects.MAGMA_SKIN, masterworks.scaleDuration(effectDuration), perkData.hasEnhanced ? 1 : 0,
                        true, false, false));
                }
            }
        }
    }
}