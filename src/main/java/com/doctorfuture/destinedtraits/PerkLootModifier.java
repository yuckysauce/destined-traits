package com.doctorfuture.destinedtraits.perks;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.neoforged.neoforge.common.loot.IGlobalLootModifier;
import net.neoforged.neoforge.common.loot.LootModifier;
import com.doctorfuture.destinedtraits.init.ModRegistries;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

public class PerkLootModifier extends LootModifier {
    private final int tierMin;
    private final int tierMax;
    private final String biomeTheme; 
    private final int tierBonus;
    private final Optional<ResourceLocation> originTraitWeapon;
    private final Optional<ResourceLocation> originTraitArmor;
    private final Optional<ResourceLocation> customPool;

    public static final MapCodec<PerkLootModifier> CODEC = RecordCodecBuilder.mapCodec(inst ->
        codecStart(inst).and(
            inst.group(
                Codec.INT.optionalFieldOf("tier_min", 1).forGetter(modifier -> modifier.tierMin),
                Codec.INT.optionalFieldOf("tier_max", 5).forGetter(modifier -> modifier.tierMax),
                Codec.STRING.optionalFieldOf("biome_theme", "default").forGetter(modifier -> modifier.biomeTheme),
                Codec.INT.optionalFieldOf("tier_bonus", 0).forGetter(modifier -> modifier.tierBonus),
                ResourceLocation.CODEC.optionalFieldOf("origin_trait_weapon").forGetter(modifier -> modifier.originTraitWeapon),
                ResourceLocation.CODEC.optionalFieldOf("origin_trait_armor").forGetter(modifier -> modifier.originTraitArmor),
                ResourceLocation.CODEC.optionalFieldOf("perk_pool").forGetter(modifier -> modifier.customPool)
            )
        ).apply(inst, PerkLootModifier::new)
    );

    public PerkLootModifier(LootItemCondition[] conditionsIn, int tierMin, int tierMax, String biomeTheme, int tierBonus, Optional<ResourceLocation> originTraitWeapon, Optional<ResourceLocation> originTraitArmor, Optional<ResourceLocation> customPool) {
        super(conditionsIn);
        this.tierMin = tierMin;
        this.tierMax = tierMax;
        this.biomeTheme = biomeTheme;
        this.tierBonus = tierBonus;
        this.originTraitWeapon = originTraitWeapon;
        this.originTraitArmor = originTraitArmor;
        this.customPool = customPool;
    }

    @Override
    protected ObjectArrayList<ItemStack> doApply(ObjectArrayList<ItemStack> generatedLoot, LootContext context) {
        for (ItemStack itemstack : generatedLoot) {
            applyItemPerks(
                itemstack, 
                context.getLevel(), 
                this.tierMin, 
                this.tierMax, 
                this.biomeTheme, 
                this.tierBonus, 
                this.originTraitWeapon, 
                this.originTraitArmor, 
                this.customPool
            );
        }
        return generatedLoot;
    }

    @Override
    public MapCodec<? extends IGlobalLootModifier> codec() {
        return CODEC;
    }

    // --- OVERLOAD FOR 7-PARAMETER CALLERS (CraftingPerkHandler & MobSpawnPerkHandler) ---
    public static void applyItemPerks(
        ItemStack itemstack, 
        Level level, 
        int tierMin, 
        int tierMax, 
        String biomeTheme, 
        int tierBonus, 
        Optional<ResourceLocation> customPool
    ) {
        applyItemPerks(
            itemstack, 
            level, 
            tierMin, 
            tierMax, 
            biomeTheme, 
            tierBonus, 
            Optional.empty(), 
            Optional.empty(), 
            customPool
        );
    }

    // --- FULL 9-PARAMETER METHOD WITH ORIGIN TRAIT SUPPORT ---
    @SuppressWarnings("unchecked")
    public static void applyItemPerks(
        ItemStack itemstack, 
        Level level, 
        int tierMin, 
        int tierMax, 
        String biomeTheme, 
        int tierBonus, 
        Optional<ResourceLocation> originTraitWeapon, 
        Optional<ResourceLocation> originTraitArmor, 
        Optional<ResourceLocation> customPool
    ) {
        if (level.isClientSide()) return;

        Registry<PerkPool> poolRegistry = level.registryAccess().registryOrThrow(ModRegistries.PERK_POOLS_KEY);

        DataComponentType<List<ResourceLocation>> perkComponent = 
            (DataComponentType<List<ResourceLocation>>) (Object) BuiltInRegistries.DATA_COMPONENT_TYPE.get(ResourceLocation.fromNamespaceAndPath("destinedtraits", "perks"));

        if (perkComponent == null) return;

        // Early exit if the item already has perks
        if (!itemstack.getOrDefault(perkComponent, Collections.emptyList()).isEmpty()) {
            return; 
        }

        String category = null;
        String suffix = null;
        net.minecraft.world.item.Item item = itemstack.getItem();

        if (item instanceof net.minecraft.world.item.ArmorItem || item instanceof net.minecraft.world.item.ShieldItem) {
            category = "armor";
            suffix = "armor";
        } else if (item instanceof net.minecraft.world.item.ProjectileWeaponItem) {
            category = "ranged";
            suffix = "ranged";
        } else if (itemstack.is(ItemTags.WEAPON_ENCHANTABLE) || item instanceof net.minecraft.world.item.TridentItem) {
            category = "melee";
            suffix = "melee";
        }
        
        if (category == null) return;

        // 1. Initialize the perk list
        List<ResourceLocation> currentPerks = new ArrayList<>();

        // 2. Apply Origin Traits immediately (bypasses the pool check)
        if ((category.equals("melee") || category.equals("ranged")) && originTraitWeapon.isPresent()) {
            ResourceLocation weaponTrait = originTraitWeapon.get();
            if (!currentPerks.contains(weaponTrait)) currentPerks.add(weaponTrait);
        } else if (category.equals("armor") && originTraitArmor.isPresent()) {
            ResourceLocation armorTrait = originTraitArmor.get();
            if (!currentPerks.contains(armorTrait)) currentPerks.add(armorTrait);
        }

        // 3. Find Random Pool
        String targetTheme = biomeTheme;
        PerkPool pool = null;
        ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(itemstack.getItem());

        if (customPool.isPresent()) {
            pool = poolRegistry.get(customPool.get());
        }

        if (pool == null) {
            pool = poolRegistry.get(ResourceLocation.fromNamespaceAndPath("destinedtraits", "items/" + itemId.getNamespace() + "/" + itemId.getPath() + "_" + targetTheme));
        }

        if (pool == null) {
            pool = poolRegistry.get(ResourceLocation.fromNamespaceAndPath("destinedtraits", "items/" + itemId.getNamespace() + "/" + itemId.getPath()));
        }

        if (pool == null) {
            pool = poolRegistry.get(ResourceLocation.fromNamespaceAndPath("destinedtraits", category + "/" + targetTheme + "_" + suffix));
            if (pool == null) {
                pool = poolRegistry.get(ResourceLocation.fromNamespaceAndPath("destinedtraits", category + "/default_" + suffix));
            }
        }

        // 4. Apply Random Pool Perks
        if (pool != null) {
            var random = level.getRandom();
            net.minecraft.world.item.component.CustomData customData = itemstack.getOrDefault(net.minecraft.core.component.DataComponents.CUSTOM_DATA, net.minecraft.world.item.component.CustomData.EMPTY);
            
            int[] computedTier = new int[1];
            
            itemstack.set(net.minecraft.core.component.DataComponents.CUSTOM_DATA, customData.update(tag -> {
                int baseTier;
                if (tag.contains("tier")) {
                    baseTier = tag.getInt("tier");
                } else {
                    int min = tag.contains("tier_min") ? tag.getInt("tier_min") : tierMin;
                    int max = tag.contains("tier_max") ? tag.getInt("tier_max") : tierMax;
                    tag.remove("tier_min");
                    tag.remove("tier_max");
                    
                    int bound = (max - min) + 1;
                    baseTier = random.nextInt(bound > 0 ? bound : 1) + min;
                }
                int finalTier = Math.min(5, baseTier + tierBonus);
                tag.putInt("tier", finalTier);
                computedTier[0] = finalTier;
            }));

            int finalTier = computedTier[0];
            
            if (finalTier >= 1 && pool.leftColumn() != null && !pool.leftColumn().isEmpty()) {
                ResourceLocation leftPerk = pool.leftColumn().get(random.nextInt(pool.leftColumn().size()));
                if (!currentPerks.contains(leftPerk)) currentPerks.add(leftPerk);
            }
            
            if (finalTier >= 1 && pool.rightColumn() != null && !pool.rightColumn().isEmpty()) {
                ResourceLocation rightPerk = pool.rightColumn().get(random.nextInt(pool.rightColumn().size()));
                if (!currentPerks.contains(rightPerk)) currentPerks.add(rightPerk);
            }

            if (finalTier >= 5) {
                com.doctorfuture.destinedtraits.perks.MasterworkUtils.applyRandom(itemstack, random);
            }
        }

        // 5. Save the data component if we added anything (Origin traits or Pool perks)
        if (!currentPerks.isEmpty()) {
            itemstack.set(perkComponent, currentPerks);
        }
        if (originTraitWeapon.isPresent() || originTraitArmor.isPresent()) {
            itemstack.update(net.minecraft.core.component.DataComponents.CUSTOM_DATA,
                net.minecraft.world.item.component.CustomData.EMPTY,
                customData -> customData.update(tag -> tag.putBoolean("has_origin_trait", true)));
        }
    }
}