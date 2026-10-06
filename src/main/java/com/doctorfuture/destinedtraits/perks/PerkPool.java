package com.doctorfuture.destinedtraits.perks;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.ResourceLocation;
import java.util.List;

public record PerkPool(List<ResourceLocation> leftColumn, List<ResourceLocation> rightColumn) {
    public static final Codec<PerkPool> CODEC = RecordCodecBuilder.create(instance -> instance.group(
        ResourceLocation.CODEC.listOf().fieldOf("left_column").forGetter(PerkPool::leftColumn),
        ResourceLocation.CODEC.listOf().fieldOf("right_column").forGetter(PerkPool::rightColumn)
    ).apply(instance, PerkPool::new));
}