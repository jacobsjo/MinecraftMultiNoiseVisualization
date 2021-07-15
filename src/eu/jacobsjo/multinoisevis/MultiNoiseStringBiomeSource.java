//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package eu.jacobsjo.multinoisevis;

/*
import com.google.common.collect.ImmutableList;
import com.mojang.datafixers.util.Pair;
import it.unimi.dsi.fastutil.doubles.DoubleArrayList;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.WorldgenRandom;
import net.minecraft.world.level.levelgen.synth.NormalNoise;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import javax.naming.OperationNotSupportedException;
import java.util.ArrayList;
import java.util.List;

public class MultiNoiseStringBiomeSource {
    private static final Pair<Integer, List<Double>> DEFAULT_NOISE_PARAMETERS = Pair.of(-7, ImmutableList.of(1.0D, 1.0D));

    private final long seed;
    private final boolean fixedSeed;
    private final String worldname;

    private final NormalNoise temperatureNoise;
    private final NormalNoise humidityNoise;
    private final NormalNoise altitudeNoise;
    private final NormalNoise weirdnessNoise;

    Pair<Integer, List<Double>> temperatureParams;
    Pair<Integer, List<Double>> humidiyParams;
    Pair<Integer, List<Double>> altitudeParams;
    Pair<Integer, List<Double>> weirdnessParams;

    private List<Pair<String, ClimateParametersProxy>> biome_list;

    private MultiNoiseStringBiomeSource(long seed, List<Pair<String, ClimateParametersProxy>> biome_list, Pair<Integer, List<Double>> temperatureParams, Pair<Integer, List<Double>> humidityParams, Pair<Integer, List<Double>> altitudeParams, Pair<Integer, List<Double>> weirdnessParams, boolean fixedSeed, String worldname) {
        this.seed = seed;
        this.fixedSeed = fixedSeed;
        this.worldname = worldname;

        this.temperatureNoise = NormalNoise.create(new WorldgenRandom(seed), temperatureParams.getFirst(), new DoubleArrayList(temperatureParams.getSecond()));
        this.humidityNoise = NormalNoise.create(new WorldgenRandom(seed+1L), humidityParams.getFirst(), new DoubleArrayList(humidityParams.getSecond()));
        this.altitudeNoise = NormalNoise.create(new WorldgenRandom(seed+2L), altitudeParams.getFirst(), new DoubleArrayList(altitudeParams.getSecond()));
        this.weirdnessNoise = NormalNoise.create(new WorldgenRandom(seed+3L), weirdnessParams.getFirst(), new DoubleArrayList(weirdnessParams.getSecond()));

        this.temperatureParams = temperatureParams;
        this.humidiyParams = humidityParams;
        this.altitudeParams = altitudeParams;
        this.weirdnessParams = weirdnessParams;

        this.biome_list = biome_list;
    }


    public MultiNoiseStringBiomeSource withSeed(long seed){
        if (fixedSeed) {
            System.out.println("WARNING: The biome seed is fixed by the Datapack. Changing the seed has no effect");
            return this;
        }

        return new MultiNoiseStringBiomeSource(seed, biome_list, temperatureParams, humidiyParams, altitudeParams, weirdnessParams, false, worldname);
    }

    public long getSeed(){
        return seed;
    }

    public boolean isFixedSeed(){
        return fixedSeed;
    }

    public String getWorldname(){
        return worldname;
    }

    static MultiNoiseStringBiomeSource defaultNether(long seed) {
        return new MultiNoiseStringBiomeSource(seed, ImmutableList.of(
                Pair.of("minecraft:nether_wastes", new ClimateParametersProxy(0.0F, 0.0F, 0.0F, 0.0F, 0.0F)),
                Pair.of("minecraft:soul_sand_valley", new ClimateParametersProxy(0.0F, -0.5F, 0.0F, 0.0F, 0.0F)),
                Pair.of("minecraft:crimson_forest", new ClimateParametersProxy(0.4F, 0.0F, 0.0F, 0.0F, 0.0F)),
                Pair.of("minecraft:warped_forest", new ClimateParametersProxy(0.0F, 0.5F, 0.0F, 0.0F, 0.375F)),
                Pair.of("minecraft:basalt_deltas", new ClimateParametersProxy(-0.5F, 0.0F, 0.0F, 0.0F, 0.175F))
        ), DEFAULT_NOISE_PARAMETERS, DEFAULT_NOISE_PARAMETERS, DEFAULT_NOISE_PARAMETERS, DEFAULT_NOISE_PARAMETERS, false, "minecraft:the_nether");
    }

    static MultiNoiseStringBiomeSource readJson(long seed, String json, String worldname) throws OperationNotSupportedException {
        JSONObject dimension = new JSONObject(json);
        JSONObject generator = dimension.getJSONObject("generator");
        JSONObject biome_source = generator.getJSONObject("biome_source");
        String type = biome_source.getString("type");
        if (!type.equals("minecraft:multi_noise")){
            throw new OperationNotSupportedException("Opened dimenstion is not of type minecraft:multi_noise");
        }

        Pair<Integer, List<Double>> temperatureParams = jsonToNoiseParams(biome_source.getJSONObject("temperature_noise"));
        Pair<Integer, List<Double>> humidityParams = jsonToNoiseParams(biome_source.getJSONObject("humidity_noise"));
        Pair<Integer, List<Double>> altitudeParams = jsonToNoiseParams(biome_source.getJSONObject("altitude_noise"));
        Pair<Integer, List<Double>> weirdnessParams = jsonToNoiseParams(biome_source.getJSONObject("weirdness_noise"));

        List<Pair<String, ClimateParametersProxy>> biome_list = new ArrayList<>();

        JSONArray biomes = biome_source.getJSONArray("biomes");
        for (Object b : biomes){
            JSONObject biome = (JSONObject) b;
            String biome_name = biome.getString("biome");
            JSONObject parameters = biome.getJSONObject("parameters");
            float temperature = parameters.getFloat("temperature");
            float humidity = parameters.getFloat("humidity");
            float altitude = parameters.getFloat("altitude");
            float weirdness = parameters.getFloat("weirdness");
            float offset = parameters.getFloat("offset");
            ClimateParametersProxy climateParameters = new ClimateParametersProxy(temperature, humidity, altitude, weirdness, offset);
            biome_list.add(new Pair<>(biome_name, climateParameters));
        }

        boolean fixedSeed = false;
        try {
            long s = biome_source.getLong("seed");
            if (s != 0){
                seed = s;
                fixedSeed = true;
            }
        } catch (JSONException ignored){ }

        return new MultiNoiseStringBiomeSource(seed, biome_list, temperatureParams, humidityParams, altitudeParams, weirdnessParams, fixedSeed, worldname);
    }

    private static Pair<Integer, List<Double>> jsonToNoiseParams(JSONObject obj){
        JSONArray amplitudes = obj.getJSONArray("amplitudes");
        ArrayList<Double> amp = new ArrayList<>();
        for (Object a : amplitudes){
            amp.add(((Number) a).doubleValue());
        }

        int firstOctave = obj.getInt("firstOctave");
        return Pair.of(firstOctave, amp);
    }

    static MultiNoiseStringBiomeSource voidSource() {
        return new MultiNoiseStringBiomeSource(0L, ImmutableList.of(
                Pair.of("minecraft:the_void", new ClimateParametersProxy(0.0F, 0.0F, 0.0F, 0.0F, 0.0F))
        ), DEFAULT_NOISE_PARAMETERS, DEFAULT_NOISE_PARAMETERS, DEFAULT_NOISE_PARAMETERS, DEFAULT_NOISE_PARAMETERS, false, "");
    }

    public String getBiome(int x, int y, int z) {
        return getBiomeAndParameters(x, y, z).getFirst();
    }

    public String getBiome(float temperature, float humidity, float altitude, float weirdness){
        return getBiomeAndParameters(temperature, humidity, altitude, weirdness).getFirst();
    }

    public Pair<String, ClimateParametersProxy> getBiomeAndParameters(int x, int y, int z) {
        return getBiomeAndParameters(getTemperature(x, y, z), getHumidity(x, y, z), getAltitude(x, y, z), getWeirdness(x, y, z));
    }

    public Pair<String, ClimateParametersProxy> getBiomeAndParameters(float temperature, float humidity, float altitude, float weirdness){
        Biome.ClimateParameters currentClimate = new Biome.ClimateParameters(temperature, humidity, altitude, weirdness, 0.0f);

        return biome_list.stream().min((a,b)->Float.compare(a.getSecond().fitness(currentClimate), b.getSecond().fitness(currentClimate))).orElseGet(() -> Pair.of("minecraft:the_void", new ClimateParametersProxy(0.0F, 0.0F, 0.0F, 0.0F, 0.0F)));
    }

    public float getTemperature(int x, int y, int z){
        return (float) temperatureNoise.getValue(x, y, z);
    }

    public float getHumidity(int x, int y, int z) {
        return (float) humidityNoise.getValue(x, y, z);
    }

    public float getAltitude(int x, int y, int z) {
        return (float) altitudeNoise.getValue(x, y, z);
    }

    public float getWeirdness(int x, int y, int z) {
        return (float) weirdnessNoise.getValue(x, y, z);
    }
}
*/