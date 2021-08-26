package eu.jacobsjo.multinoisevis;

import com.mojang.datafixers.util.Pair;
import net.minecraft.world.level.biome.Climate;

public interface BiomeResourceSource {
    Pair<String, Climate.ParameterPoint> getSurfaceNoiseBiome(int biomeX, int biomeZ);
    Pair<String, Climate.ParameterPoint> getNoiseBiome(int biomeX, int biomeY, int biomeZ);
    Pair<String, Climate.ParameterPoint> findBiome(Climate.TargetPoint target);

    double getOffset(int param_0, int param_1, int param_2);
    double getTemperature(double param_0, double param_1, double param_2);
    double getHumidity(double param_0, double param_1, double param_2);
    double getContinentalness(double param_0, double param_1, double param_2);
    double getErosion(double param_0, double param_1, double param_2);
    double getWeirdness(double param_0, double param_1, double param_2);

    BiomeResourceSource withSeed(long seed);
    TerrainShape getTerrainShape(int biomeX, int biomeZ);
    TerrainShape findTerrainShape(Climate.TargetPoint target, boolean isCostal);

    int getTerrainHeight(int biomeX, int biomeZ);

    class TerrainShape {
        public final double offset;
        public final double factor;
        public final boolean coastal;
        public final float peaks;

        TerrainShape(double offset, double factor, boolean costal, float peaks) {
            this.offset = offset;
            this.factor = factor;
            this.coastal = costal;
            this.peaks = peaks;
        }
    }

}
