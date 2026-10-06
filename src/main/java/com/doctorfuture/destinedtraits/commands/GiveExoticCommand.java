package com.doctorfuture.destinedtraits.commands;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import com.mojang.logging.LogUtils;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.core.Registry;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.resources.RegistryOps;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.server.packs.resources.Resource;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

import com.doctorfuture.destinedtraits.init.ModRegistries;
import com.doctorfuture.destinedtraits.perks.ExoticLootModifier;
import com.doctorfuture.destinedtraits.perks.PerkPool;

import java.io.Reader;
import java.util.Comparator;
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.concurrent.CompletableFuture;
import java.util.function.Predicate;

/**
 * Adds "/destinedtraits give_exotic <exotic> [target]" so an exotic can be spawned on demand for testing.
 */
@EventBusSubscriber(modid = "destinedtraits")
public final class GiveExoticCommand {

    private static final String EXOTIC_MODIFIER_TYPE = "destinedtraits:exotic_injection";
    private static final org.slf4j.Logger LOGGER = LogUtils.getLogger();
    private record ExoticDefinition(String name, ExoticLootModifier modifier) {}

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(
            Commands.literal("destinedtraits")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("give_exotic")
                    .then(Commands.argument("exotic", StringArgumentType.word())
                        .suggests(GiveExoticCommand::suggestExotics)
                        .executes(ctx -> giveExotic(ctx.getSource(), StringArgumentType.getString(ctx, "exotic"), ctx.getSource().getPlayerOrException()))
                        .then(Commands.argument("target", EntityArgument.player())
                            .executes(ctx -> giveExotic(ctx.getSource(), StringArgumentType.getString(ctx, "exotic"), EntityArgument.getPlayer(ctx, "target")))
                        )
                    )
                )
        );
    }

    private static CompletableFuture<Suggestions> suggestExotics(com.mojang.brigadier.context.CommandContext<CommandSourceStack> ctx, SuggestionsBuilder builder) {
        loadExotics(ctx.getSource()).keySet().forEach(builder::suggest);
        builder.suggest("random");
        return builder.buildFuture();
    }

    private static int giveExotic(CommandSourceStack source, String exoticKey, ServerPlayer target) {
        Map<String, ExoticDefinition> exotics = loadExotics(source);
        if (exotics.isEmpty()) {
            source.sendFailure(Component.literal("No exotic loot modifier definitions were found in the active data packs."));
            return 0;
        }

        String resolvedKey = "random".equals(exoticKey)
            ? exotics.keySet().stream().skip(RandomSource.create().nextInt(exotics.size())).findFirst().orElseThrow()
            : exoticKey;
        ExoticDefinition definition = exotics.get(resolvedKey);
        if (definition == null) {
            source.sendFailure(Component.literal("Unknown exotic '" + exoticKey + "'. Valid options: " + String.join(", ", exotics.keySet()) + ", random"));
            return 0;
        }

        Registry<PerkPool> poolRegistry = source.getServer().registryAccess().registryOrThrow(ModRegistries.PERK_POOLS_KEY);
        ItemStack exotic = definition.modifier().createExoticStack(RandomSource.create(), poolRegistry);

        if (!target.getInventory().add(exotic)) {
            target.drop(exotic, false);
        }

        source.sendSuccess(() -> Component.literal("Gave " + definition.name() + " to " + target.getName().getString()), true);
        return 1;
    }

    private static Map<String, ExoticDefinition> loadExotics(CommandSourceStack source) {
        Predicate<ResourceLocation> isLootModifier = id -> id.getPath().endsWith(".json");
        Map<ResourceLocation, Resource> resources = source.getServer().getResourceManager().listResources("loot_modifiers", isLootModifier);
        Map<String, ExoticDefinition> exotics = new LinkedHashMap<>();

        resources.entrySet().stream()
            .sorted(Map.Entry.comparingByKey(Comparator.comparing(ResourceLocation::toString)))
            .forEach(entry -> {
                ResourceLocation resourceId = entry.getKey();
                String path = resourceId.getPath();
                if (!path.startsWith("loot_modifiers/")) return;
                String modifierId = path.substring("loot_modifiers/".length(), path.length() - ".json".length());
                String key = modifierId.startsWith("exotic_")
                    ? modifierId.substring("exotic_".length())
                    : modifierId;
                if (!"destinedtraits".equals(resourceId.getNamespace())) {
                    key = resourceId.getNamespace() + ":" + key;
                }
                String exoticKey = key;

                try (Reader reader = entry.getValue().openAsReader()) {
                    JsonElement json = JsonParser.parseReader(reader);
                    if (!json.isJsonObject()
                            || !json.getAsJsonObject().has("type")
                            || !EXOTIC_MODIFIER_TYPE.equals(json.getAsJsonObject().get("type").getAsString())) {
                        return;
                    }
                    ExoticLootModifier.CODEC.codec().parse(
                            RegistryOps.create(JsonOps.INSTANCE, source.getServer().registryAccess()), json)
                        .resultOrPartial(error -> LOGGER.warn("Unable to decode exotic command definition from {}: {}", resourceId, error))
                        .ifPresent(modifier -> exotics.put(exoticKey, new ExoticDefinition(modifier.getCustomName(), modifier)));
                } catch (Exception exception) {
                    LOGGER.warn("Unable to load exotic command definition from {}", resourceId, exception);
                }
            });

        return exotics;
    }
}
