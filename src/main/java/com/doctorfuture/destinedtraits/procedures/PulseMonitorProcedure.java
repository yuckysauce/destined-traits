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
import com.doctorfuture.destinedtraits.perks.MasterworkUtils;
import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;

@EventBusSubscriber
public class PulseMonitorProcedure {
    
    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        execute(event, event.getEntity());
    }

    public static void execute(Entity entity) {
        execute(null, entity);
    }

    private static void execute(@Nullable Event event, Entity entity) {
        if (entity instanceof Player player && player.isAlive() && !player.level().isClientSide()) {
            
            float halfHealth = player.getMaxHealth() / 2.0f;
            
            // Triggers when health is at or below 50%
            if (player.getHealth() <= halfHealth) {
                
                ItemStack mainHandItem = player.getMainHandItem();
                
                @SuppressWarnings("unchecked")
                DataComponentType<List<ResourceLocation>> perkComponent = 
                    (DataComponentType<List<ResourceLocation>>) (Object) BuiltInRegistries.DATA_COMPONENT_TYPE.get(ResourceLocation.fromNamespaceAndPath("destinedtraits", "perks"));
                
                if (perkComponent != null) {
                    List<ResourceLocation> perks = mainHandItem.getOrDefault(perkComponent, Collections.emptyList());
                    ResourceLocation pulseMonitor = ResourceLocation.fromNamespaceAndPath("destinedtraits", "pulse_monitor");

                    if (perks.contains(pulseMonitor)) {
                        
                        int index = perks.indexOf(pulseMonitor);
                        int tier = mainHandItem.getOrDefault(net.minecraft.core.component.DataComponents.CUSTOM_DATA, net.minecraft.world.item.component.CustomData.EMPTY).copyTag().getInt("tier");
                        
                        boolean isEnhanced = WeaponPerkUtils.isEnhancedAtIndex(mainHandItem, index);
                        
                        // 1. Calculate points of health lost past the half-health mark
                        int lostHealth = (int) (halfHealth - player.getHealth());
                        
                        // 2. Cap the base amplifier at 7
                        int baseAmplifier = Math.max(0, Math.min(7, lostHealth));
                        
                        // 3. Add the flat +2 if the weapon is enhanced
                        int finalAmplifier = baseAmplifier + (isEnhanced ? 2 : 0);

                        MobEffectInstance currentEffect = player.getEffect(DestinedtraitsModMobEffects.PULSE_MONITOR_EFFECT);
                        int effectDuration = MasterworkUtils.getStats(player).scaleDuration(40);
                        
                        // Apply if missing, expiring, or if the amplifier needs to update due to taking damage or healing
                        if (currentEffect == null || currentEffect.getDuration() <= 10 || currentEffect.getAmplifier() != finalAmplifier) {
                            player.addEffect(new MobEffectInstance(DestinedtraitsModMobEffects.PULSE_MONITOR_EFFECT, effectDuration, finalAmplifier, true, true));
                        }
                    }
                }
            }
        }
    }
}