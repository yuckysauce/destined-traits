package com.doctorfuture.destinedtraits.perks;

import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.FinalizeSpawnEvent;

import java.util.Optional;

@EventBusSubscriber(modid = "destinedtraits")
public class MobSpawnPerkHandler {

    @SubscribeEvent
    public static void onMobSpawn(FinalizeSpawnEvent event) {
        Level level = event.getEntity().level();
        
        // Server side execution only
        if (level.isClientSide()) {
            return;
        }

        Mob mob = event.getEntity();
        
        // Process equipment on spawned mobs
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            ItemStack itemInSlot = mob.getItemBySlot(slot);
            
            if (!itemInSlot.isEmpty()) {
                NonLootPerks.apply(itemInSlot, (net.minecraft.server.level.ServerLevel) level, mob.position(), mob);
            }
        }
    }
}