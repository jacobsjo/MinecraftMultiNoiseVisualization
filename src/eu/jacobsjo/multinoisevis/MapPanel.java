package eu.jacobsjo.multinoisevis;

import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.biome.MultiNoiseBiomeResourceSource;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class MapPanel extends JPanel implements MouseMotionListener, MouseListener, ComponentListener, MouseWheelListener {
    enum Parameter {
        TEMPERATURE("Temperature"),
        HUMIDITY("Humidity"),
        WEIRDNESS("Weirdness"),
        CONTINENTALNESS("Continentalness"),
        EROSION("Erosion");

        private String name;
        Parameter(String name){
            this.name = name;
        }

        @Override
        public String toString(){
            return name;
        }
    }


    public MultiNoiseBiomeResourceSource biomeSource;
    private float offsetX = 0;
    private float offsetZ = 0;
    private final int movementDownsample = 8;
    private final int stationaryDownsample = 4;

    private int downsample = movementDownsample;
    private float scaling = 0.25f;

    private int oldX;
    private int oldY;

    private boolean drag=false;

    private int mouseX;
    private int mouseY;
    private final Timer timer;

    private String hightlightBiome = "";

    private BufferedImage image;
    private BufferedImage voronoiImage;
    Lock imageLock = new ReentrantLock();
    Lock voronoiImageLock = new ReentrantLock();
    private BiomeColors biomeColors;

    private Thread redrawThread;
    private Thread voronoiRedrawThread;

    private Parameter voronoi1 = Parameter.TEMPERATURE;
    private Parameter voronoi2 = Parameter.HUMIDITY;

    public MapPanel(MultiNoiseBiomeResourceSource biomeSource, BiomeColors biomeColors){

        timer = new Timer(500, actionEvent -> {
            downsample = stationaryDownsample;
            redrawImage();
        });

        timer.start();

        addMouseMotionListener(this);
        addMouseListener(this);
        addMouseWheelListener(this);
        addComponentListener(this);

        //this.width = width;
        //this.height = height;
        this.biomeSource = biomeSource;
        this.biomeColors = biomeColors;
        redrawImage();
    }

/*    @Override
    public void mousePressed(MouseEvent e) {
        if (panel.contains(e.getPoint())) {
            dX = e.getLocationOnScreen().x - panel.getX();
            dY = e.getLocationOnScreen().y - panel.getY();
            panel.setDraggable(true);
        }
    }*/

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

    public String getTeleportCommand(){
        //if (biomeSource.getWorldname().equals(""))
            return "/tp @s " + mouseX + " ~ " + mouseY;
        //else
        //    return "/execute in " + biomeSource.getWorldname() + " run tp @s " + (mouseX*4) + " ~ " + (mouseY*4);
    }

    public String getBiomeAtMouse(){
        return biomeSource.getNoiseBiome(mouseX, 0, mouseY);
    }

    public void setHightlightBiome(String hightlightBiome){
        this.hightlightBiome = hightlightBiome;
    }

    public void setVoronoiParameters(Parameter parameter1, Parameter parameter2){
        this.voronoi1 = parameter1;
        this.voronoi2 = parameter2;
        redrawVoronoi();
    }

    public void redrawImage(){
        if (getWidth() == 0 || getHeight() == 0) return;

        if (redrawThread != null && redrawThread.isAlive())
            return;

        redrawThread = new Thread(() -> {
            MultiNoiseBiomeResourceSource bs = biomeSource;
            BiomeColors bc = biomeColors;
            int ds = downsample;
            float s = scaling;
            float ox = offsetX;
            float oz = offsetZ;
            int w = getWidth();
            int h = getHeight();
            String hb = hightlightBiome;

            BufferedImage newImage = new BufferedImage(w / ds, h /ds, BufferedImage.TYPE_INT_RGB);
            for (int x = 0 ; x<w/ds ; x++){
                for (int z = 0; z<h/ds ; z++) {
                    String biome = bs.getNoiseBiome(
                            (int) (x * ds * s) + (int) ox,
                            0,
                            (int) (z * ds * s) + (int) oz
                    );
                    int color = bc.getBiomeRGB(biome, (biome.equals(hb) && (((x * ds) + (int) (ox/s) + (z * ds) + (int) (oz/s)) >> 2 & 4) == 0));

                    newImage.setRGB(x, z, color);
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

    public void redrawVoronoi(){
        if (getWidth() == 0 || getHeight() == 0) return;

        if (voronoiRedrawThread != null && voronoiRedrawThread.isAlive())
            return;

        voronoiRedrawThread = new Thread(() -> {
            int vs = Math.min(getWidth(),getHeight()) / 3;
            int vds = 1;
            Parameter v1 = voronoi1;
            Parameter v2 = voronoi2;
            MultiNoiseBiomeResourceSource bs = biomeSource;
            BiomeColors bc = biomeColors;
            String hb = hightlightBiome;
            int mx = mouseX;
            int my = mouseY;

            BufferedImage newVoronoiImage = new BufferedImage(vs / vds, vs /vds, BufferedImage.TYPE_INT_RGB);
            for (int p1 = 0 ; p1 < vs/vds ; p1++){
                for (int p2 = 0; p2 < vs/vds ; p2++) {
                    float param1 = ((float) p1 / vs * vds * 4.0f) - 2.0f;
                    float param2 = ((float) p2 / vs * vds * 4.0f) - 2.0f;

                    double temp = v1 == Parameter.TEMPERATURE ? param1 : v2 == Parameter.TEMPERATURE ? param2 : biomeSource.getTemperature(mx, 0, my);
                    double humid = v1 == Parameter.HUMIDITY ? param1 : v2 == Parameter.HUMIDITY ? param2 : biomeSource.getHumidity(mx, 0, my);
                    double cont = v1 == Parameter.CONTINENTALNESS ? param1 : v2 == Parameter.CONTINENTALNESS ? param2 : biomeSource.getContinentalness(mx, 0, my);
                    double ero = v1 == Parameter.EROSION ? param1 : v2 == Parameter.EROSION ? param2 : biomeSource.getErosion(mx, 0, my);
                    double weird = v1 == Parameter.WEIRDNESS ? param1 : v2 == Parameter.WEIRDNESS ? param2 : biomeSource.getWeirdness(mx, 0, my);

                    String biome = bs.findBiome(Climate.target((float) temp, (float) humid, (float) cont, (float) ero, 0.0f, (float) weird));
                    int color = bc.getBiomeRGB(biome, (biome.equals(hb) && (((p1 * vds) + (p2 * vds)) >> 2 & 4) == 0));
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

        String biome = biomeSource.getNoiseBiome(mouseX, 0, mouseY);

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
        g2.drawString("-2", getWidth() - vs - 8, vs + 15);
        g2.drawString("2", getWidth() - 10, vs + 15);
        g2.drawString("2", getWidth() - vs - 15, vs);
        g2.drawString("-2", getWidth() - vs - 20, 15);
        g2.drawString(voronoi1.toString(), getWidth() - vs + 40, vs + 15);

        g2.rotate(-Math.PI/2);
        g2.drawString(voronoi2.toString(), -vs+ 50, getWidth() - vs - 5);
        g2.rotate(Math.PI/2);

        double temp = biomeSource.getTemperature(mouseX, 0, mouseY);
        double humid = biomeSource.getHumidity(mouseX, 0, mouseY);
        double cont = biomeSource.getContinentalness(mouseX, 0, mouseY);
        double ero = biomeSource.getErosion(mouseX, 0, mouseY);
        double weird = biomeSource.getWeirdness(mouseX, 0, mouseY);

        double pointer1 = voronoi1 == Parameter.TEMPERATURE ? temp : voronoi1 == Parameter.HUMIDITY ? humid : voronoi1 == Parameter.CONTINENTALNESS ? cont : voronoi1 == Parameter.EROSION ? ero : weird;
        double pointer2 = voronoi2 == Parameter.TEMPERATURE ? temp : voronoi2 == Parameter.HUMIDITY ? humid : voronoi2 == Parameter.CONTINENTALNESS ? cont : voronoi2 == Parameter.EROSION ? ero : weird;
        pointer1 = Math.min(2.0, Math.max(pointer1, -2.0));
        pointer2 = Math.min(2.0, Math.max(pointer2, -2.0));

        g2.setXORMode(Color.BLACK);
        g2.fillOval((int) ((pointer1 + 2.0) * vs / 4.0f - 5) + getWidth() - vs, (int) ((pointer2 + 2.0) * vs / 4.0f - 5), 10, 10);
        g2.setPaintMode();

        int scaleLingLenght = 100000;
        while (scaleLingLenght / scaling > 0.6*getWidth()){
            scaleLingLenght /= 10;
        }

        g2.setColor(new Color(0.2f, 0.2f,0.2f,0.5f));
        g2.fillRect(0,0, 250,110);
        g2.fillRect(0,getHeight()-45, (int) Math.max((scaleLingLenght / scaling) + 25, 115) ,45);


        g2.setColor(Color.WHITE);

        g2.drawString("X: " + (mouseX * 4) + "| Z: " + (mouseY*4), 5, 15);
        g2.drawString(biome, 5, 30);
/*        g2.drawString(String.format("Temperature: % .2f / % .2f", biome.getSecond().getTemperature(), temp), 5, 45);
        g2.drawString(String.format("Humidity:    % .2f / % .2f", biome.getSecond().getHumidity(), humid), 5, 60);
        g2.drawString(String.format("Altitude:    % .2f / % .2f", biome.getSecond().getAltitude(), alt), 5, 75);
        g2.drawString(String.format("Weirdness:   % .2f / % .2f", biome.getSecond().getWeirdness(), weird), 5, 90);
        g2.drawString(String.format("Offset:      % .2f /  0.00", biome.getSecond().getOffset()), 5, 105);*/


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
        timer.stop();
        drag=true;
        downsample = movementDownsample;
        oldX = e.getLocationOnScreen().x;
        oldY = e.getLocationOnScreen().y;
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        drag=false;
        timer.restart();
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
        redrawImage();
        timer.restart();
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
        timer.stop();
        downsample = movementDownsample;

        offsetX = (offsetX + e.getX() * scaling);
        offsetZ = (offsetZ + e.getY() * scaling);

        scaling *=  Math.exp(e.getWheelRotation() * 0.1f);;
        scaling = (float) Math.min(10, Math.max(0.02, scaling));

        offsetX = (offsetX - e.getX() * scaling);
        offsetZ = (offsetZ - e.getY() * scaling);

        redrawImage();
        timer.restart();
    }
}
