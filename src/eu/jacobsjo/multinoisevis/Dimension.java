package eu.jacobsjo.multinoisevis;

public class Dimension {
    private static Dimension instance;
    public static Dimension getInstance(){
        if (instance == null){
            instance = new Dimension();
        }
        return instance;
    }

    private Dimension(){}

    public long seed = 0L;
    public boolean fixedSeed = false;
    public String name = "minecraft:overworld";
}
