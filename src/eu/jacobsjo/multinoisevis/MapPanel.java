package eu.jacobsjo.multinoisevis;

import com.mojang.datafixers.util.Pair;
import net.minecraft.world.level.biome.Climate;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.util.TimerTask;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.Timer;

public class MapPanel extends JPanel implements MouseMotionListener, MouseListener, ComponentListener, MouseWheelListener {
    enum Parameter {
        TEMPERATURE("Temperature"),
        HUMIDITY("Humidity"),
        WEIRDNESS("Weirdness"),
        CONTINENTALNESS("Continentalness"),
        EROSION("Erosion"),
        DEPTH("Depth");

        private final String name;
        Parameter(String name){
            this.name = name;
        }

        @Override
        public String toString(){
            return name;
        }
    }


    public BiomeResourceSource biomeSource;
    private float offsetX = 0;
    private float offsetZ = 0;
    private final int movementDownsample = 12;
    private final int stationaryDownsample = 4;

    private int downsample = movementDownsample;
    private float scaling = 0.25f;
    private boolean doHillshade = true;

    private boolean isHillshadeEnabled = true;

    private int oldX;
    private int oldY;

    private boolean drag=false;

    private int mouseX;
    private int mouseY;
    private Timer timer;

    private String hightlightBiome = "";

    private BufferedImage image;
    private BufferedImage voronoiImage;
    Lock imageLock = new ReentrantLock();
    Lock voronoiImageLock = new ReentrantLock();
    private final BiomeColors biomeColors;

    private Thread redrawThread;
    private Thread voronoiRedrawThread;

    private Parameter voronoi1 = Parameter.TEMPERATURE;
    private Parameter voronoi2 = Parameter.HUMIDITY;

    public MapPanel(MultiNoiseBiomeResourceSource biomeSource, BiomeColors biomeColors){

        addMouseMotionListener(this);
        addMouseListener(this);
        addMouseWheelListener(this);
        addComponentListener(this);

        this.biomeSource = biomeSource;
        this.biomeColors = biomeColors;
        redrawImage();
        sceduleRedraw();
    }

    public void sceduleRedraw(){
        if (timer != null)
            timer.cancel();
        timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                downsample = stationaryDownsample;
                doHillshade = true;
                redrawImage();
            }
        }, 500);
    }


    @Override
    public void mouseDragged(MouseEvent e) {
        if (!drag) return;
        int dX = oldX - e.getLocationOnScreen().x;
        int dY = oldY - e.getLocationOnScreen().y;

        oldX = e.getLocationOnScreen().x;
        oldY = e.getLocationOnScreen().y;

        offsetX += dX * scaling;
        offsetZ += dY * scaling;

        redrawImage();
    }

    @Override
    public void mouseMoved(MouseEvent e) {
        mouseX = (int) (e.getX() * scaling + offsetX);
        mouseY = (int) (e.getY() * scaling + offsetZ);
        repaint();
        redrawVoronoi();
    }

    public String getTeleportCommand() {
        if (Dimension.getInstance().name.equals(""))
            return "/tp @s " + mouseX + " ~ " + mouseY;
        else
            return "/execute in " + Dimension.getInstance().name + " run tp @s " + (mouseX * 4) + " ~ " + (mouseY * 4);
    }

    public String getBiomeAtMouse(){
        return biomeSource.getSurfaceNoiseBiome(mouseX, mouseY).getFirst();
    }

    public void setHightlightBiome(String hightlightBiome){
        this.hightlightBiome = hightlightBiome;
    }

    public void toggleHillshadeEnabled(){
        isHillshadeEnabled = !isHillshadeEnabled;
    }

    public void setFirstVoronoiParameter(Parameter parameter){
        Parameter oldParam = this.voronoi1;
        this.voronoi1 = parameter;
        if (this.voronoi2 == this.voronoi1)
            this.voronoi2 = oldParam;

        redrawVoronoi();
    }

    public void setSecondVoronoiParameter(Parameter parameter){
        Parameter oldParam = this.voronoi2;
        this.voronoi2 = parameter;
        if (this.voronoi1 == this.voronoi2)
            this.voronoi1 = oldParam;
        redrawVoronoi();
    }

    public void redrawImage(){
        if (getWidth() == 0 || getHeight() == 0) return;

        if (redrawThread != null && redrawThread.isAlive()) {
            sceduleRedraw();
            return;
        }

        redrawThread = new Thread(() -> {
            BiomeResourceSource bs = biomeSource;
            int ds = downsample;
            boolean hs = doHillshade && isHillshadeEnabled;
            float s = scaling;
            float ox = offsetX;
            float oz = offsetZ;
            int w = getWidth();
            int h = getHeight();
            String hb = hightlightBiome;

            BufferedImage newImage = new BufferedImage(w / ds, h /ds, BufferedImage.TYPE_INT_RGB);
            for (int x = 0 ; x<w/ds ; x++){
                for (int z = 0; z<h/ds ; z++) {
                    double hillshade = 0;
                    if (hs) {
                        hillshade = calculateHillShading(bs, (int) (x * ds * s) + (int) ox, (int) (z * ds * s) + (int) oz);
                    }

                    String biome = bs.getSurfaceNoiseBiome(
                            (int) (x * ds * s) + (int) ox,
                            (int) (z * ds * s) + (int) oz
                    ).getFirst();
                    int color = biomeColors.getBiomeRGB(biome, (biome.equals(hb) && (((x * ds) + (int) (ox/s) + (z * ds) + (int) (oz/s)) >> 2 & 4) == 0), 25*hillshade);


                    newImage.setRGB(x, z, color);

                    if (downsample > ds) return;
                }
            }

            imageLock.lock();
            image = newImage;
            imageLock.unlock();
            repaint();

        });

        redrawThread.start();

        redrawVoronoi();
    }

    private double calculateHillShading(BiomeResourceSource biomeSource, int biomeX, int biomeZ){
        double offsetN = biomeSource.getOffsetAndFactor(biomeX-5, biomeZ)[0];
        double offsetS = biomeSource.getOffsetAndFactor(biomeX+5, biomeZ)[0];
        double offsetW = biomeSource.getOffsetAndFactor(biomeX, biomeZ-5)[0];
        double offsetE = biomeSource.getOffsetAndFactor(biomeX, biomeZ+5)[0];

        double gradientNS = (offsetN - offsetS)*16;
        double gradientWE = (offsetW - offsetE)*16;
        double gradient = Math.sqrt(Math.pow(2, gradientNS) + Math.pow(2, gradientWE));
        double slopeAngle = Math.atan(gradient);
        double slopeDirection = Math.atan2(gradientWE, gradientNS);

        return 0.342 * Math.cos(slopeAngle) + 0.939 * Math.sin(slopeAngle) * Math.cos(slopeDirection - Math.PI * 0.25);
    }

    public void redrawVoronoi(){
        if (getWidth() == 0 || getHeight() == 0) return;

        if (voronoiRedrawThread != null && voronoiRedrawThread.isAlive())
            return;

        voronoiRedrawThread = new Thread(() -> {
            int vs = Math.min(getWidth(),getHeight()) / 6;
            int vds = 1;
            Parameter v1 = voronoi1;
            Parameter v2 = voronoi2;
            BiomeResourceSource bs = biomeSource;
            String hb = hightlightBiome;

            int terrainHeight = biomeSource.getTerrainHeight(mouseX, mouseY);
            double x = (double)mouseX + biomeSource.getOffset(mouseX, 0, mouseY);
            double y = (double)terrainHeight/4.0 + biomeSource.getOffset(terrainHeight/4, mouseY, mouseX);
            double z = (double)mouseY + biomeSource.getOffset(mouseY, mouseX, 0);

            BufferedImage newVoronoiImage = new BufferedImage(vs / vds, vs /vds, BufferedImage.TYPE_INT_RGB);
            for (int p1 = 0 ; p1 < vs/vds ; p1++){
                for (int p2 = 0; p2 < vs/vds ; p2++) {
                    float param1 = ((float) p1 / vs * vds * 2.0f) - 1.0f;
                    float param2 = ((float) p2 / vs * vds * 2.0f) - 1.0f;

                    double temp = v1 == Parameter.TEMPERATURE ? param1 : v2 == Parameter.TEMPERATURE ? param2 : biomeSource.getTemperature(x, y, z);
                    double humid = v1 == Parameter.HUMIDITY ? param1 : v2 == Parameter.HUMIDITY ? param2 : biomeSource.getHumidity(x, y, z);
                    double cont = v1 == Parameter.CONTINENTALNESS ? param1 : v2 == Parameter.CONTINENTALNESS ? param2 : biomeSource.getContinentalness(x, 0, z);
                    double ero = v1 == Parameter.EROSION ? param1 : v2 == Parameter.EROSION ? param2 : biomeSource.getErosion(x, 0, z);
                    double weird = v1 == Parameter.WEIRDNESS ? param1 : v2 == Parameter.WEIRDNESS ? param2 : biomeSource.getWeirdness(x, 0, z);
                    double depth = v1 == Parameter.DEPTH ? param1 : v2 == Parameter.DEPTH ? param2 : 0.0;

                    Climate.TargetPoint target = Climate.target((float) temp, (float) humid, (float) cont, (float) ero, (float) depth, (float) weird);
                    double[] offsetAndFactor = bs.findOffsetAndFactor(target);
                    double darken = 1.0 - offsetAndFactor[0]*1;

                    String biome = bs.findBiome(target).getFirst();
                    int color = biomeColors.getBiomeRGB(biome, (biome.equals(hb) && (((p1 * vds) + (p2 * vds)) >> 2 & 4) == 0), darken);
                    newVoronoiImage.setRGB(p1, p2, color);
                }
            }
            voronoiImageLock.lock();
            voronoiImage = newVoronoiImage;
            voronoiImageLock.unlock();
            repaint();
        });

        voronoiRedrawThread.start();
    }

    @Override
    public void paintComponent(Graphics g) {
        if (image == null){
            redrawImage();
            return;
        }

        super.paintComponent(g);

        Graphics2D g2 = (Graphics2D) g.create();

        int terrainHeight = biomeSource.getTerrainHeight(mouseX, mouseY);

        Pair<String, Climate.ParameterPoint> biomeAndParams = biomeSource.getNoiseBiome(mouseX, terrainHeight/4, mouseY);
        String biome = biomeAndParams.getFirst();
        Climate.ParameterPoint biomeParams = biomeAndParams.getSecond();

        int vs = Math.min(getWidth(),getHeight()) / 3;

        g2.setFont(new Font("DejaVu Sans Mono", Font.PLAIN, 15));

        imageLock.lock();
        g2.drawImage(image, 0,0, getWidth(), getHeight(), this);
        imageLock.unlock();

        g2.setColor(new Color(0.2f, 0.2f,0.2f,1.0f));
        g2.fillRect(getWidth() - vs - 20 , 0, vs + 20, vs + 20);
        voronoiImageLock.lock();
        g2.drawImage(voronoiImage, getWidth() - vs , 0, vs, vs , this);
        voronoiImageLock.unlock();
        g2.setColor(Color.white);
        g2.drawString("-1", getWidth() - vs - 8, vs + 15);
        g2.drawString("1", getWidth() - 10, vs + 15);
        g2.drawString("1", getWidth() - vs - 15, vs);
        g2.drawString("-1", getWidth() - vs - 20, 15);
        g2.drawString(voronoi1.toString(), getWidth() - vs + 40, vs + 15);

        g2.rotate(-Math.PI/2);
        g2.drawString(voronoi2.toString(), -vs+ 50, getWidth() - vs - 5);
        g2.rotate(Math.PI/2);

        double x = (double)mouseX + biomeSource.getOffset(mouseX, 0, mouseY);
        double y = (double)terrainHeight/4.0 + biomeSource.getOffset(terrainHeight/4, mouseY, mouseX);
        double z = (double)mouseY + biomeSource.getOffset(mouseY, mouseX, 0);

        double temp = biomeSource.getTemperature(x,y,z);
        double humid = biomeSource.getHumidity(x,y,z);
        double cont = biomeSource.getContinentalness(x,0,z);
        double ero = biomeSource.getErosion(x,0,z);
        double weird = biomeSource.getWeirdness(x,0,z);

        double pointer1 = voronoi1 == Parameter.TEMPERATURE ? temp : voronoi1 == Parameter.HUMIDITY ? humid : voronoi1 == Parameter.CONTINENTALNESS ? cont : voronoi1 == Parameter.EROSION ? ero : voronoi1 == Parameter.WEIRDNESS ? weird : 0.0;
        double pointer2 = voronoi2 == Parameter.TEMPERATURE ? temp : voronoi2 == Parameter.HUMIDITY ? humid : voronoi2 == Parameter.CONTINENTALNESS ? cont : voronoi2 == Parameter.EROSION ? ero : voronoi2 == Parameter.WEIRDNESS ? weird : 0.0;
        pointer1 = Math.min(1.0, Math.max(pointer1, -1.0));
        pointer2 = Math.min(1.0, Math.max(pointer2, -1.0));

        g2.setXORMode(Color.BLACK);
        g2.fillOval((int) ((pointer1 + 1.0) * vs / 2.0f - 5) + getWidth() - vs, (int) ((pointer2 + 1.0) * vs / 2.0f - 5), 10, 10);
        g2.setPaintMode();

        int scaleLingLenght = 100000;
        while (scaleLingLenght / scaling > 0.6*getWidth()){
            scaleLingLenght /= 10;
        }

        g2.setColor(new Color(0.2f, 0.2f,0.2f,0.5f));
        g2.fillRect(0,0, 360,145);
        g2.fillRect(0,getHeight()-45, (int) Math.max((scaleLingLenght / scaling) + 25, 115) ,45);


        g2.setColor(Color.WHITE);

        g2.drawString("X: " + (mouseX * 4) + "| Y: " + terrainHeight + "| Z: " + (mouseY*4), 5, 15);
        g2.drawString(biome, 5, 30);
        g2.drawString(String.format("Temperature:     %13s / % .2f", biomeParams.temperature(), temp), 5, 45);
        g2.drawString(String.format("Humidity:        %13s / % .2f", biomeParams.humidity(), humid), 5, 60);
        g2.drawString(String.format("Continentalness: %13s / % .2f", biomeParams.continentalness(), cont), 5, 75);
        g2.drawString(String.format("Erosion:         %13s / % .2f", biomeParams.erosion(), ero), 5, 90);
        g2.drawString(String.format("Weirdness:       %13s / % .2f", biomeParams.weirdness(), weird), 5, 105);
        g2.drawString(String.format("Depth:           %13s /  0.00", biomeParams.depth()), 5, 120);
        g2.drawString(String.format("Offset:                % .4f /  0.00", biomeParams.offset()), 5, 135);


        g2.drawString("" + (scaleLingLenght * 4) + " Blocks", 12, getHeight()-26);
        g2.drawLine(10, getHeight()-30, 10, getHeight()-20);
        g2.drawLine(10, getHeight()-25, 10 + (int) (scaleLingLenght / scaling), getHeight()-25);
        g2.drawLine(10 + (int) (scaleLingLenght / scaling), getHeight()-30, 10 + (int) (scaleLingLenght / scaling), getHeight()-20);
    }

    @Override
    public void mouseClicked(MouseEvent e) {
    }

    @Override
    public void mousePressed(MouseEvent e) {
        if (e.getButton() != MouseEvent.BUTTON1) return;
        timer.cancel();
        drag=true;
        downsample = movementDownsample;
        doHillshade = false;
        oldX = e.getLocationOnScreen().x;
        oldY = e.getLocationOnScreen().y;
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        drag=false;
        sceduleRedraw();
    }

    @Override
    public void mouseEntered(MouseEvent e) {

    }

    @Override
    public void mouseExited(MouseEvent e) {

    }

    @Override
    public void componentResized(ComponentEvent e) {
        downsample=movementDownsample;
        doHillshade=false;
        redrawImage();
        sceduleRedraw();
    }

    @Override
    public void componentMoved(ComponentEvent e) {

    }

    @Override
    public void componentShown(ComponentEvent e) {

    }

    @Override
    public void componentHidden(ComponentEvent e) {

    }

    @Override
    public void mouseWheelMoved(MouseWheelEvent e) {
        timer.cancel();

        downsample = movementDownsample;
        doHillshade = false;

        offsetX = (offsetX + e.getX() * scaling);
        offsetZ = (offsetZ + e.getY() * scaling);

        scaling *=  Math.exp(e.getWheelRotation() * 0.1f);
        scaling = (float) Math.min(10, Math.max(0.02, scaling));

        offsetX = (offsetX - e.getX() * scaling);
        offsetZ = (offsetZ - e.getY() * scaling);

        redrawImage();
        sceduleRedraw();
    }
}
