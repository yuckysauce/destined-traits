package com.doctorfuture.destinedtraits.procedures;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.util.RandomSource;
import net.minecraft.util.Mth;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.component.CustomData;

import java.util.List;
import java.util.Collections;
import com.doctorfuture.destinedtraits.perks.MasterworkUtils;

public class BuiltDifferentProcedure {
    public static void execute(Entity entity, ItemStack itemstack) {
        // We now check the provided itemstack directly instead of the main hand
        if (entity == null || itemstack == null || itemstack.isEmpty())
            return;

        double enhancedbonus = 1;

        if (entity instanceof Player player) {
            // Retrieve the custom data component for the perks
            DataComponentType<List<ResourceLocation>> perkComponent = (DataComponentType<List<ResourceLocation>>) (Object) BuiltInRegistries.DATA_COMPONENT_TYPE
                    .get(ResourceLocation.fromNamespaceAndPath("destinedtraits", "perks"));

            if (perkComponent != null) {
                List<ResourceLocation> perks = itemstack.getOrDefault(perkComponent, Collections.emptyList());
                ResourceLocation builtDifferentPerk = ResourceLocation.fromNamespaceAndPath("destinedtraits", "built_different");

                // Check if the item actually has the perk
                if (perks.contains(builtDifferentPerk)) {
                    int index = perks.indexOf(builtDifferentPerk);
                    int tier = itemstack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getInt("tier");

                    // Returns true if left perk is tier 3+, or right perk is tier 4
                    if (WeaponPerkUtils.isEnhancedAtIndex(itemstack, index)) {
                        enhancedbonus = 2;
                    }

                    MasterworkUtils.Stats masterworks = MasterworkUtils.getStats(player);
                    float procChance = Math.min(1.0F, (float) (enhancedbonus * 0.1 * masterworks.strengthMultiplier()));
                    if (player.getRandom().nextFloat() < procChance) {
                        // 50/50 chance to either heal or feed the player
                        if (Mth.nextInt(RandomSource.create(), 0, 1) == 1) {
                            player.setHealth((float) (player.getHealth() + masterworks.strengthMultiplier()));
                        } else {
                            int food = Math.max(1, (int) Math.round(masterworks.strengthMultiplier()));
                            player.getFoodData().setFoodLevel(Math.min(20, player.getFoodData().getFoodLevel() + food));
                        }
                    }
                }
            }
        }
    }
}