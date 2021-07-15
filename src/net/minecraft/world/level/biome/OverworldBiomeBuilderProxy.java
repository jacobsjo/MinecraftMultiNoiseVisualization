package net.minecraft.world.level.biome;

import com.google.common.collect.ImmutableList;
import com.mojang.datafixers.util.Pair;
import net.minecraft.resources.ResourceKey;

public class OverworldBiomeBuilderProxy {
    public void addBiomes(ImmutableList.Builder<Pair<Climate.ParameterPoint, ResourceKey<Biome>>> builder){
        (new OverworldBiomeBuilder()).addBiomes(builder);
    }
}
