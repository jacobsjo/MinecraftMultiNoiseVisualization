package eu.jacobsjo.multinoisevis;

import com.mojang.datafixers.util.Pair;
import net.minecraft.world.level.biome.Climate;

public interface BiomeResourceSource {
    Pair<String, Climate.ParameterPoint> getSurfaceNoiseBiome(int biomeX, int biomeZ);
    Pair<String, Climate.ParameterPoint> getNoiseBiome(int biomeX, int biomeY, int biomeZ);
    Pair<String, Climate.ParameterPoint> findBiome(Climate.TargetPoint target);

    double getTemperature(double param_0, double param_1, double param_2);
    double getHumidity(double param_0, double param_1, double param_2);
    double getContinentalness(double param_0, double param_1, double param_2);
    double getErosion(double param_0, double param_1, double param_2);
    double getWeirdness(double param_0, double param_1, double param_2);

    BiomeResourceSource withSeed(long seed);
    double[] getOffsetAndFactor(int biomeX, int biomeZ);
    double[] findOffsetAndFactor(Climate.TargetPoint target);

    int getTerrainHeight(int biomeX, int biomeZ);
}
