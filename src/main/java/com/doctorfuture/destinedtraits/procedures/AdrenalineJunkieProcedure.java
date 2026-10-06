package com.doctorfuture.destinedtraits.procedures;

import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.bus.api.Event;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;

import javax.annotation.Nullable;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.tags.DamageTypeTags;
import com.doctorfuture.destinedtraits.perks.MasterworkUtils;

@EventBusSubscriber
public class AdrenalineJunkieProcedure {
    @SubscribeEvent
    public static void onEntityDamaged(LivingDamageEvent.Post event) {
        if (event != null && event.getSource().getEntity() != null) {
            String msgId = event.getSource().type().msgId();
            Entity attacker = event.getSource().getEntity();
            
            // Checks only for explosions and fireworks, ignoring what type of entity was hit
            boolean isExplosion = event.getSource().is(DamageTypeTags.IS_EXPLOSION) || 
                                  "explosion".equals(msgId) || 
                                  "explosion.player".equals(msgId) || 
                                  "player_explosion".equals(msgId) || 
                                  "fireworks".equals(msgId);
                                  
            if (isExplosion) {
                execute(event, attacker);
            }
        }
    }

    public static void execute(Entity entity) {
        execute(null, entity);
    }

    private static void execute(@Nullable Event event, Entity entity) {
        if (entity == null) return;
            
        if (entity instanceof Player _player) {
            ItemStack mainHandItem = _player.getMainHandItem();
            
            boolean hasPerk = mainHandItem
                    .getOrDefault((net.minecraft.core.component.DataComponentType<java.util.List<net.minecraft.resources.ResourceLocation>>) (Object) net.minecraft.core.registries.BuiltInRegistries.DATA_COMPONENT_TYPE
                            .get(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("destinedtraits", "perks")), java.util.Collections.emptyList())
                    .contains(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("destinedtraits", "adrenaline_junkie"));
            
            if (hasPerk) {
                int enhancedbonus = 0;
                MasterworkUtils.Stats masterworks = MasterworkUtils.getStats(_player);
                
                if (java.util.Optional.of(mainHandItem).map(item -> {
                    net.minecraft.core.component.DataComponentType<java.util.List<net.minecraft.resources.ResourceLocation>> comp = (net.minecraft.core.component.DataComponentType<java.util.List<net.minecraft.resources.ResourceLocation>>) (Object) net.minecraft.core.registries.BuiltInRegistries.DATA_COMPONENT_TYPE
                            .get(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("destinedtraits", "perks"));
                    java.util.List<net.minecraft.resources.ResourceLocation> perks = item.getOrDefault(comp, java.util.Collections.emptyList());
                    
                    int index = perks.indexOf(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("destinedtraits", "adrenaline_junkie"));
                    if (index == -1) return false; 
                        
                    int tier = item.getOrDefault(net.minecraft.core.component.DataComponents.CUSTOM_DATA, net.minecraft.world.item.component.CustomData.EMPTY).copyTag().getInt("tier");
                    return WeaponPerkUtils.isEnhancedAtIndex(item, index);
                }).orElse(false)) {
                    enhancedbonus = 380;
                } else {
                    enhancedbonus = 300;
                }
                
                java.util.Optional<net.minecraft.core.Holder.Reference<net.minecraft.world.effect.MobEffect>> effectOpt = 
                    net.minecraft.core.registries.BuiltInRegistries.MOB_EFFECT.getHolder(
                        net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("destinedtraits", "adrenaline_junkie_effect")
                    );
                    
                if (effectOpt.isPresent()) {
                    int newAmplifier = 0;
                    MobEffectInstance currentEffect = _player.getEffect(effectOpt.get());
                    
                    if (currentEffect != null) {
                        newAmplifier = Math.min(currentEffect.getAmplifier() + 1, 4);
                    }
                    
                    _player.addEffect(new MobEffectInstance(effectOpt.get(), masterworks.scaleDuration(enhancedbonus), newAmplifier, true, true));
                }
            }
        }
    }
}