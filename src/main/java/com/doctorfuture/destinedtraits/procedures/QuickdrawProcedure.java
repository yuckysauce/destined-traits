package com.doctorfuture.destinedtraits.procedures;

import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.living.LivingEquipmentChangeEvent;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;

import java.util.Collections;
import java.util.List;

@EventBusSubscriber
public class QuickdrawProcedure {

    // A unique identifier for the temporary attack speed boost
    private static final ResourceLocation QUICKDRAW_MODIFIER_ID = ResourceLocation.fromNamespaceAndPath("destinedtraits", "quickdraw_boost");

    /**
     * STEP 1: Apply the massive attack speed buff instantly upon drawing the weapon.
     * This also automatically removes the buff if the player switches to a non-quickdraw weapon.
     */
    @SubscribeEvent
    public static void onEquipmentChange(LivingEquipmentChangeEvent event) {
        if (event.getEntity() instanceof Player player && event.getSlot() == EquipmentSlot.MAINHAND) {
            
            AttributeInstance attackSpeed = player.getAttribute(Attributes.ATTACK_SPEED);
            if (attackSpeed == null) return;

            ItemStack newItem = event.getTo();
            
            @SuppressWarnings("unchecked")
            DataComponentType<List<ResourceLocation>> perkComponent = 
                    (DataComponentType<List<ResourceLocation>>) (Object) BuiltInRegistries.DATA_COMPONENT_TYPE.get(ResourceLocation.fromNamespaceAndPath("destinedtraits", "perks"));
            
            boolean hasQuickdraw = false;
            if (perkComponent != null) {
                List<ResourceLocation> perks = newItem.getOrDefault(perkComponent, Collections.emptyList());
                hasQuickdraw = perks.contains(ResourceLocation.fromNamespaceAndPath("destinedtraits", "quickdraw"));
            }

            if (hasQuickdraw) {
                attackSpeed.removeModifier(QUICKDRAW_MODIFIER_ID);
                attackSpeed.addTransientModifier(new AttributeModifier(QUICKDRAW_MODIFIER_ID, 100.0, AttributeModifier.Operation.ADD_VALUE));
            } else {
                attackSpeed.removeModifier(QUICKDRAW_MODIFIER_ID);
            }
        }
    }

    /**
     * TRIGGER A: Remove the buff the moment they land a strike on an entity.
     */
    @SubscribeEvent
    public static void onPlayerAttack(AttackEntityEvent event) {
        removeQuickdrawBuff(event.getEntity());
    }

    /**
     * TRIGGER B: Remove the buff if they punch a block (e.g., trying to mine with the weapon).
     */
    @SubscribeEvent
    public static void onBlockHit(PlayerInteractEvent.LeftClickBlock event) {
        removeQuickdrawBuff(event.getEntity());
    }

    /**
     * TRIGGER C: Remove the buff if they punch empty air.
     * We catch this by tracking the very first tick of the player's arm-swing animation.
     */
    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (player.swinging && player.swingTime == 1) {
            removeQuickdrawBuff(player);
        }
    }

    /**
     * Helper Method: Cleanly strips the modifier if it exists.
     */
    private static void removeQuickdrawBuff(Player player) {
        AttributeInstance attackSpeed = player.getAttribute(Attributes.ATTACK_SPEED);
        if (attackSpeed != null && attackSpeed.hasModifier(QUICKDRAW_MODIFIER_ID)) {
            attackSpeed.removeModifier(QUICKDRAW_MODIFIER_ID);
        }
    }
}