package eu.jacobsjo.multinoisevis;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.Scanner;

public class BiomeColors {

    private JSONObject object;
    public BiomeColors(String configFilename){
        String json = null;
        File file = new File(configFilename);
        try {
            json = new Scanner(file).useDelimiter("\\Z").next();
        } catch (FileNotFoundException e) {
            InputStream in = getClass().getResourceAsStream("/biome_colors.json");
            json = new Scanner(in).useDelimiter("\\Z").next();
        }
        object = new JSONObject(json);
    }

    public int getBiomeRGB(String biome_name, boolean highlight, double darken){
        int red, green, blue;
        if (object.has(biome_name)) {
            JSONArray colorArray = object.getJSONArray(biome_name);
            red = colorArray.getInt(0);
            green = colorArray.getInt(1);
            blue = colorArray.getInt(2);
        } else {
            int hash = biome_name.hashCode();
            red = hash >> 16 & 0xFF;
            green = hash >> 8 & 0xFF;
            blue = hash & 0xFF;
        }

        red = Math.min(Math.max((int)(red - darken), 0x00),0xFF);
        green = Math.min(Math.max((int)(green - darken), 0x00), 0xFF);
        blue = Math.min(Math.max((int)(blue - darken), 0x00), 0xFF);

        if (highlight){
            red = Math.min((int)(red *1.2 + 50), 0xFF);
            green = Math.min((int)(green *1.2 + 50), 0xFF);
            blue = Math.min((int)(blue *1.2 + 50), 0xFF);
        }

        return red << 16 | green  << 8 | blue;
    }
}
