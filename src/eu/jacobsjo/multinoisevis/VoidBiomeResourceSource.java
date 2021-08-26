package eu.jacobsjo.multinoisevis;

import com.mojang.datafixers.util.Pair;
import net.minecraft.world.level.biome.Climate;

public class VoidBiomeResourceSource implements BiomeResourceSource{

    @Override
    public Pair<String, Climate.ParameterPoint> getSurfaceNoiseBiome(int biomeX, int biomeZ) {
        return Pair.of("minecraft:the_void", Climate.parameters(
                Climate.point(0.0f),
                Climate.point(0.0f),
                Climate.point(0.0f),
                Climate.point(0.0f),
                Climate.point(0.0f),
                Climate.point(0.0f),
                1.0f
        ));
    }

    @Override
    public Pair<String, Climate.ParameterPoint> getNoiseBiome(int biomeX, int biomeY, int biomeZ) {
        return Pair.of("minecraft:the_void", Climate.parameters(
                Climate.point(0.0f),
                Climate.point(0.0f),
                Climate.point(0.0f),
                Climate.point(0.0f),
                Climate.point(0.0f),
                Climate.point(0.0f),
                1.0f
        ));
    }

    @Override
    public Pair<String, Climate.ParameterPoint> findBiome(Climate.TargetPoint target) {
        return Pair.of("minecraft:the_void", Climate.parameters(
                Climate.point(0.0f),
                Climate.point(0.0f),
                Climate.point(0.0f),
                Climate.point(0.0f),
                Climate.point(0.0f),
                Climate.point(0.0f),
                1.0f
        ));
    }

    @Override
    public double getOffset(int param_0, int param_1, int param_2) {
        return 0;
    }

    @Override
    public double getTemperature(double param_0, double param_1, double param_2) {
        return 0;
    }

    @Override
    public double getHumidity(double param_0, double param_1, double param_2) {
        return 0;
    }

    @Override
    public double getContinentalness(double param_0, double param_1, double param_2) {
        return 0;
    }

    @Override
    public double getErosion(double param_0, double param_1, double param_2) {
        return 0;
    }

    @Override
    public double getWeirdness(double param_0, double param_1, double param_2) {
        return 0;
    }

    @Override
    public BiomeResourceSource withSeed(long seed) {
        return this;
    }

    @Override
    public TerrainShape getTerrainShape(int biomeX, int biomeZ) {
        return new TerrainShape(0, 1, false, 0);
    }

    @Override
    public TerrainShape findTerrainShape(Climate.TargetPoint target, boolean isCostal) {
        return new TerrainShape(0, 1, false, 0);
    }

    @Override
    public int getTerrainHeight(int biomeX, int biomeZ) {
        return 64;
    }
}
