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
import net.minecraft.tags.DamageTypeTags;
import com.doctorfuture.destinedtraits.perks.MasterworkUtils;

@EventBusSubscriber
public class DemolitionistProcedure {
    @SubscribeEvent
    public static void onEntityDamaged(LivingDamageEvent.Post event) {
        if (event != null && event.getSource().getEntity() != null) {
            // Get the raw internal name of the damage source to bypass strict tag failures
            String msgId = event.getSource().type().msgId();
            
            // Check the tag OR the raw string IDs for explosions
            if (event.getSource().is(DamageTypeTags.IS_EXPLOSION) || "explosion".equals(msgId) || "player_explosion".equals(msgId)) {
                execute(event, event.getSource().getEntity());
            }
        }
    }

    public static void execute(Entity entity) {
        execute(null, entity);
    }

    private static void execute(@Nullable Event event, Entity entity) {
        if (entity == null)
            return;
            
        if (entity instanceof Player) {
            double enhancedbonus = 0;
            ItemStack weapon = (entity instanceof LivingEntity _livEnt ? _livEnt.getMainHandItem() : ItemStack.EMPTY);
            
            if (weapon.getOrDefault((net.minecraft.core.component.DataComponentType<java.util.List<net.minecraft.resources.ResourceLocation>>) (Object) net.minecraft.core.registries.BuiltInRegistries.DATA_COMPONENT_TYPE
                            .get(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("destinedtraits", "perks")), java.util.Collections.emptyList())
                    .contains(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("destinedtraits", "demolitionist"))) {
                
                if (java.util.Optional.of(weapon).map(item -> {
                    net.minecraft.core.component.DataComponentType<java.util.List<net.minecraft.resources.ResourceLocation>> comp = (net.minecraft.core.component.DataComponentType<java.util.List<net.minecraft.resources.ResourceLocation>>) (Object) net.minecraft.core.registries.BuiltInRegistries.DATA_COMPONENT_TYPE
                            .get(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("destinedtraits", "perks"));
                    java.util.List<net.minecraft.resources.ResourceLocation> perks = item.getOrDefault(comp, java.util.Collections.emptyList());
                    int index = perks.indexOf(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("destinedtraits", "demolitionist"));
                    if (index == -1)
                        return false; 
                    int tier = item.getOrDefault(net.minecraft.core.component.DataComponents.CUSTOM_DATA, net.minecraft.world.item.component.CustomData.EMPTY).copyTag().getInt("tier");
                    return WeaponPerkUtils.isEnhancedAtIndex(item, index);
                }).orElse(false)) {
                    enhancedbonus = 7;
                } else {
                    enhancedbonus = 4;
                }
                
                // Safely clamp the repair math so durability never drops below 0
                if (!weapon.isEmpty()) {
                    int repairAmount = MasterworkUtils.getStats((Player) entity).scaleStrength((int) enhancedbonus);
                    int currentDamage = weapon.getDamageValue();
                    weapon.setDamageValue(Math.max(0, currentDamage - repairAmount));
                }
            }
        }
    }
}