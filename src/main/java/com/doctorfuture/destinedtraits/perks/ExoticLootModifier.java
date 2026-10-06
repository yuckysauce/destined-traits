package com.doctorfuture.destinedtraits.perks;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomModelData;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.neoforged.neoforge.common.loot.IGlobalLootModifier;
import net.neoforged.neoforge.common.loot.LootModifier;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ExoticLootModifier extends LootModifier {

    // 1. Moved the custom pool in here so the Codec limits are respected
    public record ExoticPerks(
        ResourceLocation originTrait, 
        Optional<ResourceLocation> leftPerk, 
        Optional<ResourceLocation> rightPerk,
        Optional<ResourceLocation> customPool
    ) {
        public static final Codec<ExoticPerks> CODEC = RecordCodecBuilder.create(inst -> inst.group(
            ResourceLocation.CODEC.fieldOf("origin_trait").forGetter(ExoticPerks::originTrait),
            ResourceLocation.CODEC.optionalFieldOf("left_perk").forGetter(ExoticPerks::leftPerk),
            ResourceLocation.CODEC.optionalFieldOf("right_perk").forGetter(ExoticPerks::rightPerk),
            ResourceLocation.CODEC.optionalFieldOf("perk_pool").forGetter(ExoticPerks::customPool)
        ).apply(inst, ExoticPerks::new));
    }

    private record CodecFields(
        Optional<Float> chance,
        Item baseItem,
        String customName,
        ExoticPerks perks,
        int tierMin,
        int tierMax,
        String biomeTheme,
        Optional<Integer> customModelData,
        Optional<ResourceLocation> lootTable
    ) {}

    private final Optional<Float> chance;
    private final Item baseItem;
    private final String customName;
    private final ExoticPerks perks;
    private final int tierMin;
    private final int tierMax;
    private final String biomeTheme; 
    private final Optional<Integer> customModelData;
    private final Optional<ResourceLocation> lootTable;

    public static final MapCodec<ExoticLootModifier> CODEC = RecordCodecBuilder.mapCodec(inst ->
        codecStart(inst).and(
            inst.group(
                Codec.FLOAT.optionalFieldOf("chance").forGetter((ExoticLootModifier m) -> m.chance),
                BuiltInRegistries.ITEM.byNameCodec().fieldOf("base_item").forGetter((ExoticLootModifier m) -> m.baseItem),
                Codec.STRING.fieldOf("custom_name").forGetter((ExoticLootModifier m) -> m.customName),
                ExoticPerks.CODEC.fieldOf("perks").forGetter((ExoticLootModifier m) -> m.perks),
                Codec.INT.optionalFieldOf("tier_min", 1).forGetter((ExoticLootModifier m) -> m.tierMin),
                Codec.INT.optionalFieldOf("tier_max", 5).forGetter((ExoticLootModifier m) -> m.tierMax),
                Codec.STRING.optionalFieldOf("biome_theme", "default").forGetter((ExoticLootModifier m) -> m.biomeTheme),
                Codec.INT.optionalFieldOf("custom_model_data").forGetter((ExoticLootModifier m) -> m.customModelData),
                ResourceLocation.CODEC.optionalFieldOf("loot_table").forGetter((ExoticLootModifier m) -> m.lootTable)
            ).apply(inst, CodecFields::new)
        ).apply(inst, (conditions, fields) -> new ExoticLootModifier(
            conditions,
            fields.chance(),
            fields.baseItem(),
            fields.customName(),
            fields.perks(),
            fields.tierMin(),
            fields.tierMax(),
            fields.biomeTheme(),
            fields.customModelData(),
            fields.lootTable()
        ))
    );

    // Now exactly 8 arguments, making the compiler very happy
    public ExoticLootModifier(
        LootItemCondition[] conditionsIn, 
        Optional<Float> chance, 
        Item baseItem, 
        String customName, 
        ExoticPerks perks, 
        int tierMin, 
        int tierMax, 
        String biomeTheme,
        Optional<Integer> customModelData,
        Optional<ResourceLocation> lootTable
    ) {
        super(conditionsIn);
        this.chance = chance;
        this.baseItem = baseItem;
        this.customName = customName;
        this.perks = perks;
        this.tierMin = tierMin;
        this.tierMax = tierMax;
        this.biomeTheme = biomeTheme;
        this.customModelData = customModelData;
        this.lootTable = lootTable;
    }

    @Override
    @SuppressWarnings("unchecked")
    protected ObjectArrayList<ItemStack> doApply(ObjectArrayList<ItemStack> generatedLoot, LootContext context) {
        
        DataComponentType<List<ResourceLocation>> perkComponent = 
            (DataComponentType<List<ResourceLocation>>) (Object) BuiltInRegistries.DATA_COMPONENT_TYPE.get(ResourceLocation.fromNamespaceAndPath("destinedtraits", "perks"));

        if (perkComponent == null || NonLootPerks.isActive()) return generatedLoot;

        if (lootTable.isPresent() && !lootTable.get().equals(context.getQueriedLootTableId())) {
            return generatedLoot;
        }

        float configChance = com.doctorfuture.destinedtraits.configuration.DestinedTraitsConfiguration.EXOTICDROPRATE.get().floatValue();
        float finalChance = this.chance.orElse(configChance);
        
        if (context.getRandom().nextFloat() < finalChance) {
            
            for (int i = 0; i < generatedLoot.size(); i++) {
                ItemStack stack = generatedLoot.get(i);
                
                boolean isEquipment = stack.getItem() instanceof net.minecraft.world.item.TieredItem || 
                                      stack.getItem() instanceof net.minecraft.world.item.ArmorItem || 
                                      stack.getItem() instanceof net.minecraft.world.item.ProjectileWeaponItem ||
                                      stack.getItem() instanceof net.minecraft.world.item.TridentItem ||
                                      stack.getItem() instanceof net.minecraft.world.item.MaceItem;

                if (isEquipment) {
                    net.minecraft.core.Registry<com.doctorfuture.destinedtraits.perks.PerkPool> poolRegistry = 
                        context.getLevel().registryAccess().registryOrThrow(com.doctorfuture.destinedtraits.init.ModRegistries.PERK_POOLS_KEY);

                    ItemStack exotic = createExoticStack(
                        context.getRandom(),
                        poolRegistry,
                        this.baseItem,
                        this.customName,
                        this.perks,
                        this.tierMin,
                        this.tierMax,
                        this.biomeTheme,
                        this.customModelData
                    );
                    generatedLoot.set(i, exotic);
                    break;
                }
            }
        }
        return generatedLoot;
    }

    /**
     * Builds a fully-configured exotic ItemStack (name, tier, perks) without requiring a LootContext.
     * Shared by the loot modifier and the admin "give exotic" command.
     */
    @SuppressWarnings("unchecked")
    public static ItemStack createExoticStack(
        net.minecraft.util.RandomSource random,
        net.minecraft.core.Registry<com.doctorfuture.destinedtraits.perks.PerkPool> poolRegistry,
        Item baseItem,
        String customName,
        ExoticPerks perks,
        int tierMin,
        int tierMax,
        String biomeTheme,
        Optional<Integer> customModelData
    ) {
        DataComponentType<List<ResourceLocation>> perkComponent =
            (DataComponentType<List<ResourceLocation>>) (Object) BuiltInRegistries.DATA_COMPONENT_TYPE.get(ResourceLocation.fromNamespaceAndPath("destinedtraits", "perks"));

        ItemStack exotic = new ItemStack(baseItem);
        customModelData.ifPresent(modelData -> exotic.set(DataComponents.CUSTOM_MODEL_DATA, new CustomModelData(modelData)));

        exotic.set(DataComponents.CUSTOM_NAME, Component.literal(customName).withStyle(style -> style.withColor(net.minecraft.ChatFormatting.GOLD).withItalic(false)));

        int finalTier = random.nextInt((tierMax - tierMin) + 1) + tierMin;
        net.minecraft.world.item.component.CustomData customData = exotic.getOrDefault(DataComponents.CUSTOM_DATA, net.minecraft.world.item.component.CustomData.EMPTY);
        exotic.set(DataComponents.CUSTOM_DATA, customData.update(tag -> {
            tag.putInt("tier", finalTier);
            tag.putBoolean("is_exotic", true);
            tag.putBoolean("has_origin_trait", true);
        }));
        if (finalTier >= 5) {
            MasterworkUtils.applyRandom(exotic, random);
        }

        String category = null;
        String suffix = null;
        Item exoticItem = exotic.getItem();

        if (exoticItem instanceof net.minecraft.world.item.ArmorItem || exoticItem instanceof net.minecraft.world.item.ShieldItem) {
            category = "armor";
            suffix = "armor";
        } else if (exoticItem instanceof net.minecraft.world.item.ProjectileWeaponItem) {
            category = "ranged";
            suffix = "ranged";
        } else if (exotic.is(net.minecraft.tags.ItemTags.WEAPON_ENCHANTABLE) || exoticItem instanceof net.minecraft.world.item.TridentItem) {
            category = "melee";
            suffix = "melee";
        }

        com.doctorfuture.destinedtraits.perks.PerkPool pool = null;
        ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(baseItem);

        // Grab custom pool from perks record if present
        if (perks.customPool().isPresent()) {
            pool = poolRegistry.get(perks.customPool().get());
        }

        if (pool == null) {
            ResourceLocation itemThemePool = ResourceLocation.fromNamespaceAndPath("destinedtraits", "items/" + itemId.getNamespace() + "/" + itemId.getPath() + "_" + biomeTheme);
            pool = poolRegistry.get(itemThemePool);
        }

        if (pool == null) {
            ResourceLocation itemDefaultPool = ResourceLocation.fromNamespaceAndPath("destinedtraits", "items/" + itemId.getNamespace() + "/" + itemId.getPath());
            pool = poolRegistry.get(itemDefaultPool);
        }

        if (pool == null && category != null) {
            pool = poolRegistry.get(ResourceLocation.fromNamespaceAndPath("destinedtraits", category + "/" + biomeTheme + "_" + suffix));
            if (pool == null) {
                pool = poolRegistry.get(ResourceLocation.fromNamespaceAndPath("destinedtraits", category + "/default_" + suffix));
            }
        }

        List<ResourceLocation> exoticPerks = new ArrayList<>();
        exoticPerks.add(perks.originTrait());

        ResourceLocation left = perks.leftPerk().orElse(null);
        ResourceLocation right = perks.rightPerk().orElse(null);

        if (pool != null) {
            if (left == null && pool.leftColumn() != null && !pool.leftColumn().isEmpty()) {
                left = pool.leftColumn().get(random.nextInt(pool.leftColumn().size()));
            }
            if (right == null && pool.rightColumn() != null && !pool.rightColumn().isEmpty()) {
                right = pool.rightColumn().get(random.nextInt(pool.rightColumn().size()));
            }
        }

        if (left != null) exoticPerks.add(left);
        if (right != null) exoticPerks.add(right);

        if (perkComponent != null) {
            exotic.set(perkComponent, exoticPerks);
        }

        return exotic;
    }

    public ItemStack createExoticStack(
        net.minecraft.util.RandomSource random,
        net.minecraft.core.Registry<com.doctorfuture.destinedtraits.perks.PerkPool> poolRegistry
    ) {
        return createExoticStack(random, poolRegistry, this.baseItem, this.customName, this.perks, this.tierMin, this.tierMax, this.biomeTheme, this.customModelData);
    }

    public String getCustomName() {
        return this.customName;
    }

    @Override
    public MapCodec<? extends IGlobalLootModifier> codec() {
        return CODEC;
    }
}