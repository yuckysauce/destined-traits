package com.doctorfuture.destinedtraits.procedures;

import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.bus.api.Event;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerXpEvent;
import com.doctorfuture.destinedtraits.network.DestinedtraitsModVariables;
import com.doctorfuture.destinedtraits.perks.MasterworkUtils;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@EventBusSubscriber
public class BetterAlreadyProcedure {
    
    @SubscribeEvent
    public static void onPlayerXPChange(PlayerXpEvent.XpChange event) {
        if (event.getEntity() != null) {
            execute(event, event.getEntity(), event.getAmount());
        }
    }

    public static void execute(Entity entity, double amount) {
        execute(null, entity, amount);
    }

    private static void execute(@Nullable Event event, Entity entity, double amount) {
        // Hunger is exclusively a Player mechanic, requiring a direct cast to Player
        if (!(entity instanceof Player player) || player.level().isClientSide()) return;
        
        // Prevent the perk from triggering or draining when spending XP
        if (amount <= 0) return;

        @SuppressWarnings("unchecked")
        DataComponentType<List<ResourceLocation>> perkComponent = 
            (DataComponentType<List<ResourceLocation>>) (Object) BuiltInRegistries.DATA_COMPONENT_TYPE.get(ResourceLocation.fromNamespaceAndPath("destinedtraits", "perks"));

        if (perkComponent != null) {
            ResourceLocation better_already = ResourceLocation.fromNamespaceAndPath("destinedtraits", "better_already");

            int totalBetterAlreadyPieces = 0;
            boolean hasEnhancedPiece = false;

            List<ItemStack> itemsToCheck = new ArrayList<>();
            
            // 2. Add all actually equipped armor pieces
            player.getArmorSlots().forEach(itemsToCheck::add);
            
            // 3. Get the offhand item and only add it if it is NOT armor
            ItemStack offhandItem = player.getOffhandItem();
            if (!offhandItem.isEmpty() && !(offhandItem.getItem() instanceof net.minecraft.world.item.ArmorItem)) {
                itemsToCheck.add(offhandItem);
            }
            
            // 4. Run the exact same loop over the combined, filtered list
            for (ItemStack item : itemsToCheck) {
                if (!item.isEmpty()) {
                    List<ResourceLocation> perks = item.getOrDefault(perkComponent, Collections.emptyList());
                    int index = perks.indexOf(better_already);
                    
                    if (index != -1) {
                        totalBetterAlreadyPieces++; 
                        
                        int tier = item.getOrDefault(net.minecraft.core.component.DataComponents.CUSTOM_DATA, net.minecraft.world.item.component.CustomData.EMPTY).copyTag().getInt("tier");
                        if (WeaponPerkUtils.isEnhancedAtIndex(item, index)) {
                            hasEnhancedPiece = true;
                        }
                    }
                }
            }

            // 2. Only execute if they are actually wearing better already armor
            if (totalBetterAlreadyPieces > 0) {
                DestinedtraitsModVariables.PlayerVariables vars = player.getData(DestinedtraitsModVariables.PLAYER_VARIABLES);
                MasterworkUtils.Stats masterworks = MasterworkUtils.getStats(player);
                
                // Add to progress (XP amount multiplied by how many pieces they are wearing)
                vars.betteralready_progress += (amount * totalBetterAlreadyPieces);
                vars.markSyncDirty();

                // 3. Trigger hunger healing if threshold is reached
                int requiredXp = masterworks.scaleEfficiencyCost(42);
                if (vars.betteralready_progress >= requiredXp) {
                    
                    // Check if the player's hunger is below the maximum of 20
                    if (player.getFoodData().getFoodLevel() < 20) {
                        int hungerAmount = Math.max(1, (int) Math.round(masterworks.scaleStrength(hasEnhancedPiece ? 2 : 1)));
                        
                        // Prevent overhealing past the maximum hunger level
                        player.getFoodData().setFoodLevel(Math.min(20, player.getFoodData().getFoodLevel() + hungerAmount));
                        
                        // Hard reset progress to 0, wiping any excess XP from large orbs
                        vars.betteralready_progress = 0; 
                        vars.markSyncDirty();
                    } else {
                        // Cap progress at 42 so the hunger heal is instantly ready the next time they sprint or heal naturally
                        vars.betteralready_progress = requiredXp;
                        vars.markSyncDirty();
                    }
                }
            }
        }
    }
}