package eu.jacobsjo.multinoisevis;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableList.Builder;
import com.mojang.datafixers.util.Pair;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.*;
import net.minecraft.world.level.biome.BiomeSource.TerrainShape;
import net.minecraft.world.level.levelgen.WorldgenRandom;
import net.minecraft.world.level.levelgen.synth.NormalNoise;

import java.util.function.Supplier;

public class MultiNoiseBiomeResourceSource implements BiomeResourceSource {
    private final NormalNoise temperatureNoise;
    private final NormalNoise humidityNoise;
    private final NormalNoise continentalnessNoise;
    private final NormalNoise erosionNoise;
    private final NormalNoise weirdnessNoise;
    private final NormalNoise offsetNoise;
    private final NormalNoise mountainPeakNoise;

    private final MultiNoiseBiomeSource.NoiseParameters temperatureNoiseParameters;
    private final MultiNoiseBiomeSource.NoiseParameters humidityNoiseParameters;
    private final MultiNoiseBiomeSource.NoiseParameters continentalnessNoiseParameters;
    private final MultiNoiseBiomeSource.NoiseParameters erosionNoiseParameters;
    private final MultiNoiseBiomeSource.NoiseParameters weirdnessNoiseParameters;

//    private TerrainShaper shaper;
    private final Climate.ParameterList<Pair<String, Climate.ParameterPoint>> parameters;
    private final TerrainShaper shaper;

    public MultiNoiseBiomeResourceSource(long seed, Climate.ParameterList<Pair<String, Climate.ParameterPoint>> biomes, MultiNoiseBiomeSource.NoiseParameters temperatueNoiseParameters, MultiNoiseBiomeSource.NoiseParameters humidityNoiseParameters, MultiNoiseBiomeSource.NoiseParameters continentalnessNoiseParameters, MultiNoiseBiomeSource.NoiseParameters erosionNoiseParameters, MultiNoiseBiomeSource.NoiseParameters weirdnessNoiseParameters) {
        this.temperatureNoiseParameters = temperatueNoiseParameters;
        this.humidityNoiseParameters = humidityNoiseParameters;
        this.continentalnessNoiseParameters = continentalnessNoiseParameters;
        this. erosionNoiseParameters = erosionNoiseParameters;
        this.weirdnessNoiseParameters = weirdnessNoiseParameters;

        this.shaper = new TerrainShaper();
        this.temperatureNoise = NormalNoise.create(new WorldgenRandom(seed), temperatueNoiseParameters.firstOctave(), temperatueNoiseParameters.amplitudes());
        this.humidityNoise = NormalNoise.create(new WorldgenRandom(seed + 1L), humidityNoiseParameters.firstOctave(), humidityNoiseParameters.amplitudes());
        this.continentalnessNoise = NormalNoise.create(new WorldgenRandom(seed + 2L), continentalnessNoiseParameters.firstOctave(), continentalnessNoiseParameters.amplitudes());
        this.erosionNoise = NormalNoise.create(new WorldgenRandom(seed + 3L), erosionNoiseParameters.firstOctave(), erosionNoiseParameters.amplitudes());
        this.weirdnessNoise = NormalNoise.create(new WorldgenRandom(seed + 4L), weirdnessNoiseParameters.firstOctave(), weirdnessNoiseParameters.amplitudes());
        this.offsetNoise = NormalNoise.create(new WorldgenRandom(seed + 5L), -3, 1.0D, 1.0D, 1.0D, 0.0D);
        this.mountainPeakNoise = NormalNoise.create(new WorldgenRandom(42L), -16, 1.0D, 1.0D, 1.0D, 1.0D, 1.0D, 1.0D, 1.0D, 1.0D, 1.0D, 1.0D, 1.0D, 1.0D, 1.0D, 1.0D, 1.0D, 1.0D);
        this.parameters = biomes;
    }

    public static MultiNoiseBiomeResourceSource overworld(long seed) {
        Builder<Pair<Climate.ParameterPoint, ResourceKey<Biome>>> builder = ImmutableList.builder();
        (new OverworldBiomeBuilderProxy()).addBiomes(builder);
        ImmutableList<Pair<Climate.ParameterPoint, Supplier<Pair<String, Climate.ParameterPoint>>>> biomes
                = builder.build().stream().map(
                        (param_1) -> Pair.of(param_1.getFirst(), (Supplier<Pair<String, Climate.ParameterPoint>>) () -> Pair.of(param_1.getSecond().location().toString(), param_1.getFirst()))
                ).collect(ImmutableList.toImmutableList());;
        MultiNoiseBiomeSource.NoiseParameters temperatureNoiseParameters = new MultiNoiseBiomeSource.NoiseParameters(-9, 1.5D, 0.0D, 1.0D, 0.0D, 0.0D, 0.0D);
        MultiNoiseBiomeSource.NoiseParameters humidityNoiseParameter = new MultiNoiseBiomeSource.NoiseParameters(-7, 1.0D, 1.0D, 0.0D, 0.0D, 0.0D, 0.0D);
        MultiNoiseBiomeSource.NoiseParameters continentalnessNoiseParameters = new MultiNoiseBiomeSource.NoiseParameters(-9, 1.0D, 1.0D, 2.0D, 2.0D, 2.0D, 1.0D, 1.0D, 1.0D, 1.0D);
        MultiNoiseBiomeSource.NoiseParameters erosionNoiseParameters = new MultiNoiseBiomeSource.NoiseParameters(-9, 1.0D, 1.0D, 0.0D, 1.0D, 1.0D);
        MultiNoiseBiomeSource.NoiseParameters weirdnessNoiseParameters = new MultiNoiseBiomeSource.NoiseParameters(-7, 1.0D, 2.0D, 1.0D, 0.0D, 0.0D, 0.0D);
        return new MultiNoiseBiomeResourceSource(seed, new Climate.ParameterList<>(biomes), temperatureNoiseParameters, humidityNoiseParameter, continentalnessNoiseParameters, erosionNoiseParameters, weirdnessNoiseParameters);
    }

    public static MultiNoiseBiomeResourceSource nether(long seed) {
        MultiNoiseBiomeSource.NoiseParameters noiseParameters = new MultiNoiseBiomeSource.NoiseParameters(-7, ImmutableList.of(1.0D, 1.0D));
        return new MultiNoiseBiomeResourceSource(seed, new Climate.ParameterList<>(ImmutableList.of(
                createPair("minecraft:nether_wastes", Climate.parameters(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F)),
                createPair("minecraft:soul_sand_valley", Climate.parameters(0.0F, -0.5F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F)),
                createPair("minecraft:crimson_forest", Climate.parameters(0.4F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F)),
                createPair("minecraft:warped_forest", Climate.parameters(0.0F, 0.5F, 0.0F, 0.0F, 0.0F, 0.0F, 0.375F)),
                createPair("minecraft:basalt_deltas", Climate.parameters(-0.5F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.175F))
        )), noiseParameters, noiseParameters, noiseParameters, noiseParameters, noiseParameters);
    }

    private static Pair<Climate.ParameterPoint, Supplier<Pair<String, Climate.ParameterPoint>>> createPair(String name, Climate.ParameterPoint parameter){
        return Pair.of(parameter, () -> Pair.of(name, parameter));
    }

    public MultiNoiseBiomeResourceSource withSeed(long seed){
        return new MultiNoiseBiomeResourceSource(seed, this.parameters, this.temperatureNoiseParameters, this.humidityNoiseParameters, this.continentalnessNoiseParameters, this.erosionNoiseParameters, this.weirdnessNoiseParameters);
    }

    public int getTerrainHeight(int biomeX, int biomeZ) {
        TerrainShape shape = getTerrainShape(biomeX, biomeZ);
        double scaling = 3000.0F / 4.0F; // TODO: divided by noise size_horizontal = 1
        double peak = mountainPeakNoise.getValue(scaling * biomeX, 0.0D, scaling * biomeZ);
        double peakValue = peak > 0.0D ? shape.peaks * peak : shape.peaks / 2.0D * peak;
        return (int) (64 + (shape.offset + peakValue) * 128);
    }

    public Pair<String, Climate.ParameterPoint> getSurfaceNoiseBiome(int biomeX, int biomeZ) {
        return getNoiseBiome(biomeX, (getTerrainHeight(biomeX, biomeZ)/4), biomeZ);
    }

    public Pair<String, Climate.ParameterPoint> getNoiseBiome(int biomeX, int biomeY, int biomeZ) {
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
        return findBiome(target);
    }

    public Pair<String, Climate.ParameterPoint> findBiome(Climate.TargetPoint target){
        return this.parameters.findBiome(target,
                () -> Pair.of("minecraft:the_void", Climate.parameters(
                        Climate.point(0.0f),
                        Climate.point(0.0f),
                        Climate.point(0.0f),
                        Climate.point(0.0f),
                        Climate.point(0.0f),
                        Climate.point(0.0f),
                        1.0f
                )));
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

    public TerrainShape getTerrainShape(int x, int z) {
        double x_offset = (double)x + this.getOffset(x, 0, z);
        double z_offset = (double)z + this.getOffset(z, x, 0);
        float continentalness = (float)this.getContinentalness(x_offset, 0.0D, z_offset);
        float weirdness = (float)this.getWeirdness(x_offset, 0.0D, z_offset);
        float erosion = (float)this.getErosion(x_offset, 0.0D, z_offset);
        TerrainShaper.Point targetPoint = this.shaper.makePoint(continentalness, erosion, weirdness);
        boolean var_6 = TerrainShaper.isCoastal(continentalness, weirdness);
        return new TerrainShape((double)this.shaper.offset(targetPoint), (double)this.shaper.factor(targetPoint), var_6, this.shaper.peaks(targetPoint));
    }

    public TerrainShape findTerrainShape(Climate.TargetPoint target, boolean isCostal) {
        TerrainShaper.Point var_5 = this.shaper.makePoint(target.continentalness(), target.erosion(), target.weirdness());
        return new TerrainShape((double)this.shaper.offset(var_5), (double)this.shaper.factor(var_5), isCostal, this.shaper.peaks(var_5));
    }


}
