package com.doctorfuture.destinedtraits.init;

import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.registries.DataPackRegistryEvent;
import net.neoforged.neoforge.registries.RegisterEvent;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import com.doctorfuture.destinedtraits.perks.PerkPool;
import com.doctorfuture.destinedtraits.perks.PerkLootModifier;
import com.doctorfuture.destinedtraits.perks.ExoticLootModifier;
import com.mojang.serialization.Codec;
import java.util.List;

@EventBusSubscriber(modid = "destinedtraits")
public class ModRegistries {

    public static final DataComponentType<String> MASTERWORK_COMPONENT = DataComponentType.<String>builder()
        .persistent(Codec.STRING)
        .networkSynchronized(ByteBufCodecs.STRING_UTF8)
        .build();
    public static final DataComponentType<String> MASTERWORK_PENALTY_COMPONENT = DataComponentType.<String>builder()
        .persistent(Codec.STRING)
        .networkSynchronized(ByteBufCodecs.STRING_UTF8)
        .build();
    
    public static final ResourceKey<Registry<PerkPool>> PERK_POOLS_KEY = 
        ResourceKey.createRegistryKey(ResourceLocation.fromNamespaceAndPath("destinedtraits", "perk_pools"));

    @SubscribeEvent
    public static void onNewDatapackRegistry(DataPackRegistryEvent.NewRegistry event) {
        event.dataPackRegistry(PERK_POOLS_KEY, PerkPool.CODEC, PerkPool.CODEC);
    }

    @SubscribeEvent
    public static void onRegister(RegisterEvent event) {
        
        // 1. Registers the item Data Component 
		event.register(Registries.DATA_COMPONENT_TYPE, helper -> {
            helper.register(ResourceLocation.fromNamespaceAndPath("destinedtraits", "masterwork"), MASTERWORK_COMPONENT);
		    helper.register(ResourceLocation.fromNamespaceAndPath("destinedtraits", "masterwork_penalty"), MASTERWORK_PENALTY_COMPONENT);
		    helper.register(ResourceLocation.fromNamespaceAndPath("destinedtraits", "perks"),
		        DataComponentType.<List<ResourceLocation>>builder()
				    .persistent(ResourceLocation.CODEC.listOf()) // <-- Revert this back to .persistent
				    .networkSynchronized(ResourceLocation.STREAM_CODEC.apply(ByteBufCodecs.list()))
				    .build()
		    );
		});

        // 2. Registers the Global Loot Modifier Codecs
        event.register(NeoForgeRegistries.Keys.GLOBAL_LOOT_MODIFIER_SERIALIZERS, helper -> {
            
            // Standard dynamic perk applicator
            helper.register(ResourceLocation.fromNamespaceAndPath("destinedtraits", "add_perks"), PerkLootModifier.CODEC);
            
            // Exotic static item injector
            helper.register(ResourceLocation.fromNamespaceAndPath("destinedtraits", "exotic_injection"), ExoticLootModifier.CODEC);
            
        });
    }
}