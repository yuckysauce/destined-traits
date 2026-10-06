package com.doctorfuture.destinedtraits;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.ChatFormatting;
import net.minecraft.resources.ResourceLocation;
import java.util.HashMap;
import java.util.Map;

public class PerkIconRegistry {
    private static final Map<ResourceLocation, String> ICON_MAP = new HashMap<>();
    
    // Default fallback icon if a perk doesn't have a registered icon
    private static final String DEFAULT_ICON = "\uE000";

    static {
        // Left Column Perks
        register(ResourceLocation.fromNamespaceAndPath("destinedtraits", "rapid_hit"), "\uE001");
        register(ResourceLocation.fromNamespaceAndPath("destinedtraits", "pulse_monitor"), "\uE002");
        register(ResourceLocation.fromNamespaceAndPath("destinedtraits", "subsistence"), "\uE003");
        register(ResourceLocation.fromNamespaceAndPath("destinedtraits", "field_prep"), "\uE004");
        register(ResourceLocation.fromNamespaceAndPath("destinedtraits", "quickdraw"), "\uE005");
        register(ResourceLocation.fromNamespaceAndPath("destinedtraits", "utilitarian"), "\uE006");
        register(ResourceLocation.fromNamespaceAndPath("destinedtraits", "demolitionist"), "\uE007");
        register(ResourceLocation.fromNamespaceAndPath("destinedtraits", "attrition_orbs"), "\uE008");
        register(ResourceLocation.fromNamespaceAndPath("destinedtraits", "loose_change"), "\uE009");
        register(ResourceLocation.fromNamespaceAndPath("destinedtraits", "eager_edge"), "\uE00A");
        register(ResourceLocation.fromNamespaceAndPath("destinedtraits", "repulsor_brace"), "\uE00B");
        register(ResourceLocation.fromNamespaceAndPath("destinedtraits", "perpetual_motion"), "\uE00C");
        register(ResourceLocation.fromNamespaceAndPath("destinedtraits", "envious_arsenal"), "\uE00D");
        register(ResourceLocation.fromNamespaceAndPath("destinedtraits", "killing_wind"), "\uE00E");
        register(ResourceLocation.fromNamespaceAndPath("destinedtraits", "ensemble"), "\uE00F");
        register(ResourceLocation.fromNamespaceAndPath("destinedtraits", "perfect_float"), "\uE010");
        register(ResourceLocation.fromNamespaceAndPath("destinedtraits", "cauterize"), "\uE011");

        // Right Column Perks
        register(ResourceLocation.fromNamespaceAndPath("destinedtraits", "rampage"), "\uE012");
        register(ResourceLocation.fromNamespaceAndPath("destinedtraits", "under_pressure"), "\uE013");
        register(ResourceLocation.fromNamespaceAndPath("destinedtraits", "unrelenting"), "\uE014");
        register(ResourceLocation.fromNamespaceAndPath("destinedtraits", "adrenaline_junkie"), "\uE015");
        register(ResourceLocation.fromNamespaceAndPath("destinedtraits", "withering_touch"), "\uE016");
        register(ResourceLocation.fromNamespaceAndPath("destinedtraits", "adagio"), "\uE017");
        register(ResourceLocation.fromNamespaceAndPath("destinedtraits", "surrounded"), "\uE018");
        register(ResourceLocation.fromNamespaceAndPath("destinedtraits", "tear"), "\uE019");
        register(ResourceLocation.fromNamespaceAndPath("destinedtraits", "dragonfly"), "\uE01A");
        register(ResourceLocation.fromNamespaceAndPath("destinedtraits", "under_over"), "\uE01B");
        register(ResourceLocation.fromNamespaceAndPath("destinedtraits", "target_lock"), "\uE01C");
        register(ResourceLocation.fromNamespaceAndPath("destinedtraits", "precision_instrument"), "\uE01D");
        register(ResourceLocation.fromNamespaceAndPath("destinedtraits", "harmony"), "\uE01E");
        register(ResourceLocation.fromNamespaceAndPath("destinedtraits", "high_ground"), "\uE01F");
        register(ResourceLocation.fromNamespaceAndPath("destinedtraits", "frenzy"), "\uE020");
        register(ResourceLocation.fromNamespaceAndPath("destinedtraits", "firing_line"), "\uE021");

        // Origin traits
        register(ResourceLocation.fromNamespaceAndPath("destinedtraits", "in_the_deep"), "\uE022");
        register(ResourceLocation.fromNamespaceAndPath("destinedtraits", "hydrostatic_pressure"), "\uE023");
        register(ResourceLocation.fromNamespaceAndPath("destinedtraits", "glass_them"), "\uE024");
        register(ResourceLocation.fromNamespaceAndPath("destinedtraits", "flowing_vitreum"), "\uE025");
        register(ResourceLocation.fromNamespaceAndPath("destinedtraits", "tidal_pulse"), "\uE026");
        register(ResourceLocation.fromNamespaceAndPath("destinedtraits", "soulfire_eminence"), "\uE027");
        register(ResourceLocation.fromNamespaceAndPath("destinedtraits", "nowhere_fast"), "\uE028");
    }

    public static void register(ResourceLocation perkId, String unicodeChar) {
        ICON_MAP.put(perkId, unicodeChar);
    }

    public static MutableComponent getIconComponent(ResourceLocation perkId) {
        String symbol = ICON_MAP.getOrDefault(perkId, DEFAULT_ICON);
        return Component.literal(symbol + " ").withStyle(ChatFormatting.WHITE);
    }
}