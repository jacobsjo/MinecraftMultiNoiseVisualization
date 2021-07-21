package eu.jacobsjo.multinoisevis;

import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.level.biome.TerrainShaper;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;

public class SplineHeightExport {
    public static void main(String[] args) throws IOException {
        SharedConstants.setVersion(new DummyVersion());
        Bootstrap.bootStrap();

        BufferedWriter writer = new BufferedWriter(new FileWriter("factor.csv"));
        writer.write("continentalness, erosion, weirdness, offset\n");

        float[] splinePointsContinentalness = {-1.1f, -1.005f, -0.51f, -0.44f, -0.18f, -0.16f, -0.15f, -0.1f, 0.25f, 1.0f};
        float[] splinePointsErosion = {-0.9f, -0.4f, -0.35f, -0.1f, 0.2f, 1.0f};

        TerrainShaper shaper = new TerrainShaper();

        for (float continentalness: splinePointsContinentalness) {
            for (float erosion: splinePointsErosion) {
                for (int weirdness = -100; weirdness <= 100 ; weirdness++) {
                    TerrainShaper.Point point = shaper.makePoint(continentalness, erosion, weirdness / 100.0f);
                    float offset = shaper.factor(point);
                    writer.write(continentalness + ", " + erosion + ", " + weirdness + ", " + offset + "\n");
                }
            }
        }

        writer.close();
    }
}
