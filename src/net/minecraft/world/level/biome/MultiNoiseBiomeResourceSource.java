package net.minecraft.world.level.biome;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableList.Builder;
import com.mojang.datafixers.util.Pair;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.levelgen.WorldgenRandom;
import net.minecraft.world.level.levelgen.synth.NormalNoise;

import java.util.Optional;
import java.util.function.Supplier;

public class MultiNoiseBiomeResourceSource {
    private final NormalNoise temperatureNoise;
    private final NormalNoise humidityNoise;
    private final NormalNoise continentalnessNoise;
    private final NormalNoise erosionNoise;
    private final NormalNoise weirdnessNoise;
    private final NormalNoise offsetNoise;
//    private TerrainShaper shaper;
    private final Climate.ParameterList<String> parameters;

    public MultiNoiseBiomeResourceSource(long seed, Climate.ParameterList<String> biomes, MultiNoiseBiomeSource.NoiseParameters param_2, MultiNoiseBiomeSource.NoiseParameters param_3, MultiNoiseBiomeSource.NoiseParameters param_4, MultiNoiseBiomeSource.NoiseParameters param_5, MultiNoiseBiomeSource.NoiseParameters param_6, int param_7, int param_8, boolean param_9, Optional param_10) {
//        this.shaper = new TerrainShaper();
        this.temperatureNoise = NormalNoise.create(new WorldgenRandom(seed), param_2.firstOctave(), param_2.amplitudes());
        this.humidityNoise = NormalNoise.create(new WorldgenRandom(seed + 1L), param_3.firstOctave(), param_3.amplitudes());
        this.continentalnessNoise = NormalNoise.create(new WorldgenRandom(seed + 2L), param_4.firstOctave(), param_4.amplitudes());
        this.erosionNoise = NormalNoise.create(new WorldgenRandom(seed + 3L), param_5.firstOctave(), param_5.amplitudes());
        this.weirdnessNoise = NormalNoise.create(new WorldgenRandom(seed + 4L), param_6.firstOctave(), param_6.amplitudes());
        this.offsetNoise = NormalNoise.create(new WorldgenRandom(seed + 5L), -3, 1.0D, 1.0D, 1.0D, 0.0D);
        this.parameters = biomes;
    }

    public static MultiNoiseBiomeResourceSource overworld(long seed) {
        Builder<Pair<Climate.ParameterPoint, ResourceKey<Biome>>> builder = ImmutableList.builder();
        (new OverworldBiomeBuilder()).addBiomes(builder);
        ImmutableList<Pair<Climate.ParameterPoint, Supplier<String>>> biomes = builder.build().stream().map((param_1) -> param_1.mapSecond((param_1x) -> ((Supplier<String>) () -> param_1x.location().toString()))).collect(ImmutableList.toImmutableList());;
        MultiNoiseBiomeSource.NoiseParameters var_1 = new MultiNoiseBiomeSource.NoiseParameters(-9, 1.0D, 1.0D, 0.0D, 1.0D, 1.0D, 0.0D);
        MultiNoiseBiomeSource.NoiseParameters var_2 = new MultiNoiseBiomeSource.NoiseParameters(-7, 1.0D, 0.0D, 1.0D, 0.0D, 1.0D, 0.0D);
        MultiNoiseBiomeSource.NoiseParameters var_3 = new MultiNoiseBiomeSource.NoiseParameters(-9, 1.0D, 1.0D, 2.0D, 2.0D, 2.0D, 1.0D, 1.0D, 1.0D, 1.0D);
        MultiNoiseBiomeSource.NoiseParameters var_4 = new MultiNoiseBiomeSource.NoiseParameters(-9, 1.0D, 1.0D, 0.0D, 1.0D, 1.0D);
        MultiNoiseBiomeSource.NoiseParameters var_5 = new MultiNoiseBiomeSource.NoiseParameters(-7, 1.0D, 2.0D, 1.0D, 0.0D, 0.0D, 0.0D);
        return new MultiNoiseBiomeResourceSource(seed, new Climate.ParameterList<>(biomes), var_1, var_2, var_3, var_4, var_5, -16, 48, false, Optional.empty());
    }

    public String getNoiseBiome(int biomeX, int biomeY, int biomeZ) {
        double x = (double)biomeX + this.getOffset(biomeX, 0, biomeZ);
        double y = (double)biomeY + this.getOffset(biomeY, biomeZ, biomeX);
        double z = (double)biomeZ + this.getOffset(biomeZ, biomeX, 0);
        float continentalness = (float)this.getContinentalness(x, 0.0D, z);
        float erosion = (float)this.getErosion(x, 0.0D, z);
        float weirdness = (float)this.getWeirdness(x, 0.0D, z);
        /*double var_6 = this.shaper.offset(this.shaper.makePoint(continentalness, erosion, weirdness));
        double var_7 = 1.0D;
        double var_8 = -0.51875D;
        double density = NoiseSampler.computeDimensionDensity(1.0D, -0.51875D, biomeY * 4) + var_6;*/
        double density = 0.0;
        Climate.TargetPoint target = Climate.target((float)this.getTemperature(x, y, z), (float)this.getHumidity(x, y, z), continentalness, erosion, (float)density, weirdness);
        return this.parameters.findBiome(target, () -> "minecraft:the_void");
    }

    public String findBiome(Climate.TargetPoint target){
        return this.parameters.findBiome(target, () -> "minecraft:the_void");
    }

    public double getOffset(int param_0, int param_1, int param_2) {
        return this.offsetNoise.getValue(param_0, param_1, param_2) * 4.0D;
    }

    public double getTemperature(double param_0, double param_1, double param_2) {
        return this.temperatureNoise.getValue(param_0, param_1, param_2);
    }

    public double getHumidity(double param_0, double param_1, double param_2) {
        return this.humidityNoise.getValue(param_0, param_1, param_2);
    }

    public double getContinentalness(double param_0, double param_1, double param_2) {
        return this.continentalnessNoise.getValue(param_0, param_1, param_2);
    }

    public double getErosion(double param_0, double param_1, double param_2) {
        return this.erosionNoise.getValue(param_0, param_1, param_2);
    }

    public double getWeirdness(double param_0, double param_1, double param_2) {
        return this.weirdnessNoise.getValue(param_0, param_1, param_2);
    }

/*
    public double[] getOffsetAndFactor(int param_0, int param_1) {
        double var_0 = (double)param_0 + this.getOffset(param_0, 0, param_1);
        double var_1 = (double)param_1 + this.getOffset(param_1, param_0, 0);
        float var_2 = (float)this.getContinentalness(var_0, 0.0D, var_1);
        float var_3 = (float)this.getWeirdness(var_0, 0.0D, var_1);
        float var_4 = (float)this.getErosion(var_0, 0.0D, var_1);
        TerrainShaper.Point var_5 = this.shaper.makePoint(var_2, var_4, var_3);
        return new double[]{(double)this.shaper.offset(var_5), (double)this.shaper.factor(var_5)};
    }*/

}
