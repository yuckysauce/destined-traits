package com.doctorfuture.destinedtraits.procedures;

import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.bus.api.Event;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import com.doctorfuture.destinedtraits.init.DestinedtraitsModMobEffects;
import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;
import com.doctorfuture.destinedtraits.perks.MasterworkUtils;

@EventBusSubscriber
public class HydrostaticPressureProcedure {
    
    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        execute(event, event.getEntity());
    }

    public static void execute(Entity entity) {
        execute(null, entity);
    }

    private static void execute(@Nullable Event event, Entity entity) {
        if (entity instanceof Player player && player.isAlive() && !player.level().isClientSide()) {
            
            int maxAir = player.getMaxAirSupply();
            int currentAir = player.getAirSupply();
            ResourceLocation hydrostaticPressure = ResourceLocation.fromNamespaceAndPath("destinedtraits", "hydrostatic_pressure");
            int armorPieces = WeaponPerkUtils.countOriginArmorPieces(player, hydrostaticPressure);
            
            // Triggers when oxygen drops below max
            if (currentAir < maxAir && armorPieces > 0) {
                boolean isEnhanced = WeaponPerkUtils.hasEnhancedOriginArmor(player, hydrostaticPressure);
                int missingAir = maxAir - Math.max(0, currentAir);
                int baseAmplifier = (int) (((float) missingAir / maxAir) * 7);
                baseAmplifier = Math.max(0, Math.min(7, baseAmplifier));
                double traitMultiplier = armorPieces / 2.0;
                int finalAmplifier = (int) Math.round((baseAmplifier + (isEnhanced ? 2 : 0)) * traitMultiplier);

                MobEffectInstance currentEffect = player.getEffect(DestinedtraitsModMobEffects.HYDROSTATIC_PRESSURE_EFFECT);
                int effectDuration = MasterworkUtils.getStats(player).scaleDuration(40);
                if (currentEffect == null || currentEffect.getDuration() <= 10 || currentEffect.getAmplifier() != finalAmplifier) {
                    player.addEffect(new MobEffectInstance(DestinedtraitsModMobEffects.HYDROSTATIC_PRESSURE_EFFECT, effectDuration, finalAmplifier, true, true));
                }
            }
        }
    }
}