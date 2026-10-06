package com.doctorfuture.destinedtraits.client;

import net.minecraft.client.resources.language.I18n;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;
import com.doctorfuture.destinedtraits.PerkIconRegistry;
import com.doctorfuture.destinedtraits.perks.MasterworkUtils;

import java.util.ArrayList;
import java.util.List;

@EventBusSubscriber(modid = "destinedtraits", value = Dist.CLIENT)
public class PerkTooltipHandler {

    @SubscribeEvent
    public static void onTooltip(ItemTooltipEvent event) {
        DataComponentType<List<ResourceLocation>> perkComponent = 
            (DataComponentType<List<ResourceLocation>>) (Object) BuiltInRegistries.DATA_COMPONENT_TYPE.get(ResourceLocation.fromNamespaceAndPath("destinedtraits", "perks"));

        if (perkComponent != null && event.getItemStack().has(perkComponent)) {
            List<ResourceLocation> perks = event.getItemStack().get(perkComponent);

            if (perks != null && !perks.isEmpty()) {
                
                int insertIndex = event.getToolTip().size(); 
                
                for (int i = 0; i < event.getToolTip().size(); i++) {
                    Component line = event.getToolTip().get(i);
                    if (line.getContents() instanceof net.minecraft.network.chat.contents.TranslatableContents translatable) {
                        if (translatable.getKey().startsWith("item.modifiers.")) {
                            insertIndex = Math.max(1, i - 1); 
                            break;
                        }
                    }
                }

                int tier = 0;
                boolean isExotic = false;
                net.minecraft.world.item.component.CustomData customData = event.getItemStack().get(net.minecraft.core.component.DataComponents.CUSTOM_DATA);
                if (customData != null) {
                    if (customData.contains("tier")) tier = customData.copyTag().getInt("tier"); 
                    if (customData.contains("is_exotic")) isExotic = customData.copyTag().getBoolean("is_exotic");
                }

                if ((tier == 5 || isExotic) && !event.getToolTip().isEmpty()) {
                    String plainName = event.getToolTip().get(0).getString(); 
                    if (isExotic) {
                        event.getToolTip().set(0, Component.literal(plainName).withStyle(style -> style.withColor(ChatFormatting.GOLD).withItalic(false)));
                    } else {
                        event.getToolTip().set(0, Component.literal(plainName).withStyle(ChatFormatting.YELLOW));
                    }
                }
                boolean isInspectPressed = false;
                boolean keyFound = false;
                Component inspectKeyName = Component.empty(); 
                
                if (Minecraft.getInstance().getWindow() != null) {
                    for (net.minecraft.client.KeyMapping keyMapping : Minecraft.getInstance().options.keyMappings) {
                        if (keyMapping.getName().contains("inspect") && keyMapping.getName().contains("destinedtraits")) {
                            keyFound = true;
                            inspectKeyName = keyMapping.getTranslatedKeyMessage(); 
                            
                            if (InputConstants.isKeyDown(Minecraft.getInstance().getWindow().getWindow(), keyMapping.getKey().getValue())) {
                                isInspectPressed = true;
                            }
                        }
                    }
                }
                List<Component> customLines = new ArrayList<>();
                
                if (tier > 0) {
                    String diamonds = "♦".repeat(tier);
                    customLines.add(Component.literal(diamonds).withStyle(ChatFormatting.WHITE));
                }
                
                customLines.add(Component.literal("Traits").withStyle(ChatFormatting.BLUE));

                ResourceLocation originTrait = perks.size() > 0 ? perks.get(0) : null;
                ResourceLocation leftPerk = perks.size() > 1 ? perks.get(1) : null;
                ResourceLocation rightPerk = perks.size() > 2 ? perks.get(2) : null;
                
                if (originTrait != null) {
                    boolean enhanced = com.doctorfuture.destinedtraits.procedures.WeaponPerkUtils.isOriginEnhanced(event.getItemStack(), originTrait);
                    ChatFormatting color = isExotic ? ChatFormatting.GOLD : (enhanced ? ChatFormatting.YELLOW : ChatFormatting.GRAY);
                    Component icon = enhanced
                            ? Component.literal(" ✦ ").withStyle(ChatFormatting.YELLOW)
                            : Component.literal(" ■ ").withStyle(color);
					Component name = Component.literal(formatPerkName(originTrait)).withStyle(color);
					customLines.add(Component.empty().append(icon).append(name));
                    if (isInspectPressed) {
                        addMultiLineDescription(customLines, originTrait);
                    }
                }

                if (leftPerk != null) {
                    boolean enhanced = com.doctorfuture.destinedtraits.procedures.WeaponPerkUtils.isEnhanced(event.getItemStack(), leftPerk);
                    ChatFormatting color = enhanced ? ChatFormatting.YELLOW : ChatFormatting.GRAY;
                    Component icon = enhanced
                            ? Component.literal(" ✦ ").withStyle(ChatFormatting.YELLOW)
                            : Component.literal(" ■ ").withStyle(color);
					Component name = Component.literal(formatPerkName(leftPerk)).withStyle(color);
					customLines.add(Component.empty().append(icon).append(name));
                    if (isInspectPressed) {
                        addMultiLineDescription(customLines, leftPerk);
                    }
                }
                

                if (rightPerk != null) {
                    boolean enhanced = com.doctorfuture.destinedtraits.procedures.WeaponPerkUtils.isEnhanced(event.getItemStack(), rightPerk);
                    ChatFormatting color = enhanced ? ChatFormatting.YELLOW : ChatFormatting.GRAY;
                    Component icon = enhanced
                            ? Component.literal(" ✦ ").withStyle(ChatFormatting.YELLOW)
                            : Component.literal(" ■ ").withStyle(color);
					Component name = Component.literal(formatPerkName(rightPerk)).withStyle(color);
					customLines.add(Component.empty().append(icon).append(name));
                    if (isInspectPressed) {
                        addMultiLineDescription(customLines, rightPerk);
                    }
                }
                
                String masterworkBonus = event.getItemStack().get(com.doctorfuture.destinedtraits.init.ModRegistries.MASTERWORK_COMPONENT);
                String masterworkPenalty = event.getItemStack().get(com.doctorfuture.destinedtraits.init.ModRegistries.MASTERWORK_PENALTY_COMPONENT);
                if (masterworkBonus != null && masterworkPenalty != null) {
                    customLines.add(Component.empty());
                    customLines.add(Component.translatable("masterwork.destinedtraits.title").withStyle(ChatFormatting.YELLOW));
                    MasterworkUtils.Stats stats = Minecraft.getInstance().player != null
                            ? MasterworkUtils.getStats(Minecraft.getInstance().player)
                            : new MasterworkUtils.Stats(100.0, 100.0, 100.0, 100.0);
                    if (isInspectPressed) {
                        for (String stat : List.of("duration", "efficiency", "range", "strength")) {
                            customLines.add(masterworkInspectStatLine(stat, masterworkBonus, masterworkPenalty,
                                    getMasterworkStatTotal(stat, stats)));
                        }
                    } else {
                        customLines.add(masterworkStatLine("+25", masterworkBonus, getMasterworkStatTotal(masterworkBonus, stats)));
                        customLines.add(masterworkStatLine("-15", masterworkPenalty, getMasterworkStatTotal(masterworkPenalty, stats)));
                    }
                }
                
                if (!isInspectPressed && (originTrait != null || leftPerk != null || rightPerk != null)) {
                    if (masterworkBonus != null && masterworkPenalty != null) customLines.add(Component.empty());
                    customLines.add(Component.translatable("tooltip.destinedtraits.inspect_prompt", inspectKeyName.copy()).withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
                }

                event.getToolTip().addAll(insertIndex, customLines);
            }
        }
    }

    private static Component masterworkStatLine(String modifier, String stat, double total) {
        return Component.translatable("masterwork.destinedtraits.stat_line", Component.literal(modifier),
                Component.translatable("masterwork.stat." + stat), Component.literal((int) Math.round(total) + "%"))
                .withStyle(ChatFormatting.GRAY);
    }

    private static Component masterworkInspectStatLine(String stat, String bonus, String penalty, double total) {
        String modifier = stat.equals(bonus) ? "+25" : stat.equals(penalty) ? "-15" : "0";
        ChatFormatting modifierColor = modifier.equals("+25") ? ChatFormatting.GREEN
            : modifier.equals("-15") ? ChatFormatting.RED : ChatFormatting.GRAY;
        return Component.empty()
                .append(Component.translatable("masterwork.stat." + stat).withStyle(ChatFormatting.GRAY))
                .append(Component.literal(" " + (int) Math.round(total) + "% (").withStyle(ChatFormatting.GRAY))
                .append(Component.literal(modifier).withStyle(modifierColor))
                .append(Component.literal(")").withStyle(ChatFormatting.GRAY));
    }

    private static double getMasterworkStatTotal(String stat, MasterworkUtils.Stats stats) {
        return switch (stat) {
            case "strength" -> stats.strength();
            case "range" -> stats.range();
            case "duration" -> stats.duration();
            case "efficiency" -> stats.efficiency();
            default -> 100.0;
        };
    }

    private static String formatPerkName(ResourceLocation perk) {
        String rawName = perk.getPath();
        String perkPath = "creeping_tendrils".equals(rawName) ? "shrieking_evasion" : rawName;
        String nameKey = "perk." + perk.getNamespace() + "." + perkPath + ".name";
        if (I18n.exists(nameKey)) return I18n.get(nameKey);
        String[] words = rawName.split("_");
        StringBuilder formattedName = new StringBuilder();
        
        for (String word : words) {
            if (!word.isEmpty()) {
                formattedName.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1)).append(" ");
            }
        }
        return formattedName.toString().trim();
    }
    
	private static void addMultiLineDescription(List<Component> customLines, ResourceLocation perk) {
    String perkPath = "creeping_tendrils".equals(perk.getPath()) ? "shrieking_evasion" : perk.getPath();
    String descKey = "perk." + perk.getNamespace() + "." + perkPath + ".desc";
        
        if (I18n.exists(descKey)) {
            String rawText = I18n.get(descKey);
            String[] splitLines = rawText.replace("\\n", "\n").split("\n");
            
            for (String lineText : splitLines) {
                customLines.add(Component.literal("    " + lineText).withStyle(ChatFormatting.DARK_GRAY));
            }
        }
    }
}