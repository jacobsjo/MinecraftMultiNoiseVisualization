package eu.jacobsjo.multinoisevis;

import com.mojang.datafixers.util.Pair;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.biome.MultiNoiseBiomeSource;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.naming.OperationNotSupportedException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class MultiNoiseJsonReader {

    public static MultiNoiseBiomeResourceSource readJson(String json) throws OperationNotSupportedException {
        JSONObject dimension = new JSONObject(json);
        JSONObject generator = dimension.getJSONObject("generator");
        JSONObject biome_source = generator.getJSONObject("biome_source");
        String type = biome_source.getString("type");
        if (!type.equals("minecraft:multi_noise")){
            throw new OperationNotSupportedException("Opened dimenstion is not of type minecraft:multi_noise");
        }

        MultiNoiseBiomeSource.NoiseParameters temperatureParams = jsonToNoiseParams(biome_source.getJSONObject("temperature_noise"));
        MultiNoiseBiomeSource.NoiseParameters humidityParams = jsonToNoiseParams(biome_source.getJSONObject("humidity_noise"));
        MultiNoiseBiomeSource.NoiseParameters continentalnessParams = jsonToNoiseParams(biome_source.getJSONObject("continentalness_noise"));
        MultiNoiseBiomeSource.NoiseParameters erosionParams = jsonToNoiseParams(biome_source.getJSONObject("erosion_noise"));
        MultiNoiseBiomeSource.NoiseParameters weirdnessParams = jsonToNoiseParams(biome_source.getJSONObject("weirdness_noise"));

        List<Pair<Climate.ParameterPoint, Supplier<Pair<String, Climate.ParameterPoint>>>> biome_list = new ArrayList<>();

        JSONArray biomes = biome_source.getJSONArray("biomes");
        for (Object b : biomes){
            JSONObject biome = (JSONObject) b;
            String biome_name = biome.getString("biome");
            JSONObject parameters = biome.getJSONObject("parameters");
            Climate.Parameter temperature = readParameter(parameters,"temperature");
            Climate.Parameter humidity = readParameter(parameters,"humidity");
            Climate.Parameter continentalness = readParameter(parameters,"continentalness");
            Climate.Parameter erosion = readParameter(parameters,"erosion");
            Climate.Parameter weirdness = readParameter(parameters,"weirdness");
            Climate.Parameter depth = readParameter(parameters,"depth");
            float offset = (float) parameters.getDouble("offset");
            Climate.ParameterPoint climateParameters = Climate.parameters(temperature, humidity, continentalness, erosion, depth, weirdness, offset);
            biome_list.add(createPair(biome_name, climateParameters));
        }

        long seed = biome_source.getLong("seed");
        Dimension.getInstance().seed = seed;
        return new MultiNoiseBiomeResourceSource(seed, new Climate.ParameterList<>(biome_list), temperatureParams, humidityParams, continentalnessParams, erosionParams, weirdnessParams);
    }

    private static MultiNoiseBiomeSource.NoiseParameters jsonToNoiseParams(JSONObject obj){
        JSONArray amplitudes = obj.getJSONArray("amplitudes");
        ArrayList<Double> amp = new ArrayList<>();
        for (Object a : amplitudes){
            amp.add(((Number) a).doubleValue());
        }

        int firstOctave = obj.getInt("firstOctave");
        return new MultiNoiseBiomeSource.NoiseParameters(firstOctave, amp);
    }

    private static Climate.Parameter readParameter(JSONObject parent, String field){
        Object params = parent.get(field);
        if (params instanceof JSONArray){
            return Climate.range((float) ((JSONArray) params).getDouble(0), (float) ((JSONArray) params).getDouble(1));
        } else {
            return Climate.point((float) parent.getDouble(field));
        }
    }

    private static Pair<Climate.ParameterPoint, Supplier<Pair<String, Climate.ParameterPoint>>> createPair(String name, Climate.ParameterPoint parameter){
        return Pair.of(parameter, () -> Pair.of(name, parameter));
    }
}