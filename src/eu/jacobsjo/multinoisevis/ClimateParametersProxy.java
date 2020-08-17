package eu.jacobsjo.multinoisevis;

import net.minecraft.world.level.biome.Biome;

public class ClimateParametersProxy extends Biome.ClimateParameters {

    private final float temperature;
    private final float humidity;
    private final float altitude;
    private final float weirdness;
    private final float offset;

    public ClimateParametersProxy(float temerature, float humidity, float altitude, float weirdness, float offset) {
        super(temerature, humidity, altitude, weirdness, offset);

        this.temperature = temerature;
        this.humidity = humidity;
        this.altitude = altitude;
        this.weirdness = weirdness;
        this.offset = offset;
    }

    public float getTemperature(){
        return temperature;
    }

    public float getHumidity(){
        return humidity;
    }

    public float getAltitude(){
        return altitude;
    }

    public float getWeirdness(){
        return weirdness;
    }

    public float getOffset(){
        return offset;
    }
}