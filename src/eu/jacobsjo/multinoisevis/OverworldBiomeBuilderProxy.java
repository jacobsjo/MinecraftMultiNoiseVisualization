package eu.jacobsjo.multinoisevis;

import com.google.common.collect.ImmutableList;
import com.mojang.datafixers.util.Pair;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.biome.OverworldBiomeBuilder;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class OverworldBiomeBuilderProxy {
    private Method addBiomes;
    OverworldBiomeBuilder biomeBuilder;

    public OverworldBiomeBuilderProxy() {
        biomeBuilder = new OverworldBiomeBuilder();
        try {
            if (biomeBuilder.getClass().getName().equals("net.minecraft.world.level.biome.OverworldBiomeBuilder"))
                addBiomes = biomeBuilder.getClass().getDeclaredMethod("addBiomes", ImmutableList.Builder.class);
            else
                addBiomes = biomeBuilder.getClass().getDeclaredMethod("a", ImmutableList.Builder.class);
            addBiomes.setAccessible(true);
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        }

    }

    public void addBiomes(ImmutableList.Builder<Pair<Climate.ParameterPoint, ResourceKey<Biome>>> builder){
        try {
            addBiomes.invoke(biomeBuilder, builder);
        } catch (IllegalAccessException | InvocationTargetException e) {
            e.printStackTrace();
        }
    }
}
