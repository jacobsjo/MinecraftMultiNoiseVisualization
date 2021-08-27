package eu.jacobsjo.multinoisevis;

import org.apache.commons.io.monitor.FileAlterationListener;
import org.apache.commons.io.monitor.FileAlterationListenerAdaptor;
import org.apache.commons.io.monitor.FileAlterationMonitor;
import org.apache.commons.io.monitor.FileAlterationObserver;
import org.json.JSONException;

import javax.naming.OperationNotSupportedException;
import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Scanner;

public class Main {

    private static String lastPath = "";

    public static void main(String[] args) {
        long seed = System.currentTimeMillis();
        MultiNoiseStringBiomeSource biomeSource = MultiNoiseStringBiomeSource.defaultNether(seed);

        JFrame frame = new JFrame("Minecraft Multinoise Visualization");
        frame.setSize(1000, 1000);
        frame.getContentPane().setLayout(new GridLayout(1,2));

        BiomeColors biomeColors = new BiomeColors("biome_colors.json");
        MapPanel mapPanel = new MapPanel(biomeSource, biomeColors);

        JPopupMenu contextMenu = createPopupMenu(mapPanel, frame);
        mapPanel.setComponentPopupMenu(contextMenu);

        frame.getContentPane().add(mapPanel);

        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setVisible(true);

    }

    private static JPopupMenu createPopupMenu(MapPanel mapPanel, JFrame frame){

        JPopupMenu contextMenu = new JPopupMenu("File");

        JMenuItem eTpCommand = new JMenuItem("Copy teleport command");
        eTpCommand.addActionListener(actionEvent -> {
            StringSelection stringSelection = new StringSelection(mapPanel.getTeleportCommand());
            Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
            clipboard.setContents(stringSelection, null);
        });
        contextMenu.add(eTpCommand);

        FileAlterationMonitor monitor = new FileAlterationMonitor(10);
        try {
            monitor.start();
        } catch (Exception e) {
            e.printStackTrace();
        }

        JMenuItem eOpen = new JMenuItem("Open Dimension json");
        JMenuItem eOpenNether = new JMenuItem("Open Vanilla Nether");
        JMenuItem eSetSeed = new JMenuItem("Set seed");
        JMenuItem eRandomSeed = new JMenuItem("Random seed");

        eOpen.addActionListener(actionEvent -> {
            JFileChooser fileChooser = new JFileChooser(lastPath);
            FileNameExtensionFilter filter = new FileNameExtensionFilter("JSON files", "json");
            fileChooser.setFileFilter(filter);
            int returnVal = fileChooser.showOpenDialog(frame);
            if (returnVal == JFileChooser.APPROVE_OPTION) {
                String dir;
                try {
                    lastPath = fileChooser.getSelectedFile().getCanonicalPath();
                    dir = fileChooser.getSelectedFile().getParentFile().getCanonicalPath();
                } catch (IOException e) {
                    e.printStackTrace();
                    return;
                }

                try {
                    try {
                        mapPanel.biomeSource = loadJson(mapPanel.biomeSource.getSeed(), lastPath);
                        mapPanel.redrawImage();
                        eOpenNether.setEnabled(true);
                        eSetSeed.setEnabled(!mapPanel.biomeSource.isFixedSeed());
                        eRandomSeed.setEnabled(!mapPanel.biomeSource.isFixedSeed());
                    } catch (OperationNotSupportedException e) {
                        JOptionPane.showMessageDialog(null, "Opended dimension is not of type minecraft:multi_noise", "Could not open file", JOptionPane.ERROR_MESSAGE);
                    } catch (JSONException e) {
                        JOptionPane.showMessageDialog(null, "JSON format not correct", "Could not open file", JOptionPane.ERROR_MESSAGE);
                        e.printStackTrace();
                    } finally {
                        FileAlterationObserver observer = new FileAlterationObserver(dir);
                        FileAlterationListener listener = new FileAlterationListenerAdaptor() {
                            @Override
                            public void onFileCreate(File file) {
                                try {
                                    if (file.getCanonicalPath().equals(lastPath))
                                        reload(mapPanel);
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                            }

                            @Override
                            public void onFileDelete(File file) {
                                try {
                                    if (file.getCanonicalPath().equals(lastPath)) {
                                        loadVoid(mapPanel);
                                        mapPanel.redrawImage();
                                    }
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                            }

                            @Override
                            public void onFileChange(File file) {
                                try {
                                    if (file.getCanonicalPath().equals(lastPath))
                                        reload(mapPanel);
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                            }
                        };
                        observer.addListener(listener);
                        monitor.getObservers().forEach(monitor::removeObserver);
                        monitor.addObserver(observer);
                    }
                } catch (HeadlessException | FileNotFoundException e) {
                    JOptionPane.showMessageDialog(null, "Failed to open file", "Could not open file", JOptionPane.ERROR_MESSAGE);
                    e.printStackTrace();
                }

            }
        });
        contextMenu.add(eOpen);


        eOpenNether.addActionListener(actionEvent -> {
            mapPanel.biomeSource = MultiNoiseStringBiomeSource.defaultNether(mapPanel.biomeSource.getSeed());
            mapPanel.redrawImage();
            eOpenNether.setEnabled(false);
            eSetSeed.setEnabled(true);
            eRandomSeed.setEnabled(true);
            try {
                monitor.stop();
                monitor.getObservers().forEach(monitor::removeObserver);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        eOpenNether.setEnabled(false);
        contextMenu.add(eOpenNether);

        eSetSeed.addActionListener(actionEvent -> {
            String s = (String) JOptionPane.showInputDialog(frame, "Set seed: ", "Seed", JOptionPane.PLAIN_MESSAGE, null, null, mapPanel.biomeSource.getSeed());
            long ss;
            try {
                ss = Long.parseLong(s);
            } catch (NumberFormatException e) {
                ss = (long) s.hashCode();
            }
            mapPanel.biomeSource = mapPanel.biomeSource.withSeed(ss);
            mapPanel.redrawImage();
        });
        contextMenu.add(eSetSeed);

        eRandomSeed.addActionListener(actionEvent -> {
            mapPanel.biomeSource = mapPanel.biomeSource.withSeed(System.currentTimeMillis());
            mapPanel.redrawImage();
        });
        contextMenu.add(eRandomSeed);

        JMenuItem eHighlightBiome = new JMenuItem("Highlight Biome");
        eHighlightBiome.addActionListener(actionEvent -> {
            String s = (String) JOptionPane.showInputDialog(frame, "set biome: ", "Highlight Biome", JOptionPane.PLAIN_MESSAGE, null, null, mapPanel.getBiomeAtMouse());
            mapPanel.setHightlightBiome(s);
            mapPanel.redrawImage();
        });
        contextMenu.add(eHighlightBiome);

        JMenuItem eFullResolution = new JMenuItem("Render Full Resolution Image");
        eFullResolution.addActionListener(actionEvent -> {
            mapPanel.setFullResolution();
            mapPanel.redrawImage();;
        });
        contextMenu.add(eFullResolution);

        JMenuItem eGenStatistics = new JMenuItem("Generate Biome Statistics");
        eGenStatistics.addActionListener(actionEvent -> {
            JFileChooser fileChooser = new JFileChooser(lastPath);
            FileNameExtensionFilter filter = new FileNameExtensionFilter("TXT files", "txt");
            fileChooser.setFileFilter(filter);
            int returnVal = fileChooser.showSaveDialog(frame);
            if (returnVal == JFileChooser.APPROVE_OPTION) {
                try {
                    FileWriter writer = new FileWriter(fileChooser.getSelectedFile());
                    mapPanel.exportStatistics(writer);
                    writer.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
        contextMenu.add(eGenStatistics);

        JMenu eOpenVoronoi = new JMenu("Set Voronoi Diagram");

        JMenuItem eOpenVoronoi_temp_humid = new JMenuItem("Temperature / Humidity");
        eOpenVoronoi_temp_humid.addActionListener(actionEvent -> mapPanel.setVoronoiParameters(MapPanel.Parameter.TEMPERATURE, MapPanel.Parameter.HUMIDITY));
        JMenuItem eOpenVoronoi_temp_alt = new JMenuItem("Temperature / Altitude");
        eOpenVoronoi_temp_alt.addActionListener(actionEvent -> mapPanel.setVoronoiParameters(MapPanel.Parameter.TEMPERATURE, MapPanel.Parameter.ALTITUDE));
        JMenuItem eOpenVoronoi_temp_weird = new JMenuItem("Temperature / Weirdness");
        eOpenVoronoi_temp_weird.addActionListener(actionEvent -> mapPanel.setVoronoiParameters(MapPanel.Parameter.TEMPERATURE, MapPanel.Parameter.WEIRDNESS));
        JMenuItem eOpenVoronoi_humid_alt = new JMenuItem("Humidity / Altitude");
        eOpenVoronoi_humid_alt.addActionListener(actionEvent -> mapPanel.setVoronoiParameters(MapPanel.Parameter.HUMIDITY, MapPanel.Parameter.ALTITUDE));
        JMenuItem eOpenVoronoi_humid_weird = new JMenuItem("Humidity / Weirdness");
        eOpenVoronoi_humid_weird.addActionListener(actionEvent -> mapPanel.setVoronoiParameters(MapPanel.Parameter.HUMIDITY, MapPanel.Parameter.WEIRDNESS));
        JMenuItem eOpenVoronoi_alt_weird = new JMenuItem("Altitude / Weirdness");
        eOpenVoronoi_alt_weird.addActionListener(actionEvent -> mapPanel.setVoronoiParameters(MapPanel.Parameter.ALTITUDE, MapPanel.Parameter.WEIRDNESS));

        eOpenVoronoi.add(eOpenVoronoi_temp_humid);
        eOpenVoronoi.add(eOpenVoronoi_temp_alt);
        eOpenVoronoi.add(eOpenVoronoi_temp_weird);
        eOpenVoronoi.add(eOpenVoronoi_humid_alt);
        eOpenVoronoi.add(eOpenVoronoi_humid_weird);
        eOpenVoronoi.add(eOpenVoronoi_alt_weird);

        contextMenu.add(eOpenVoronoi);

        return contextMenu;
    }

    private static MultiNoiseStringBiomeSource loadJson(long seed, String path) throws OperationNotSupportedException, JSONException, FileNotFoundException {
        String json = null;
        File file = new File(path);

        File parent1 = file.getParentFile();
        File parent2 = parent1.getParentFile();
        File parent3 = parent2.getParentFile();

        String worldname = "";
        if (parent1.getName().equals("dimension") && parent3.getName().equals("data")){
            worldname = parent2.getName() + ":" + file.getName().substring(0, file.getName().length()-5);
        }

        json = new Scanner(file).useDelimiter("\\Z").next();
        return MultiNoiseStringBiomeSource.readJson(seed, json, worldname);
    }

    private static void reload(MapPanel mapPanel){
        System.out.println("Reload!");
        try {
            MultiNoiseStringBiomeSource biomeSource = loadJson(mapPanel.biomeSource.getSeed(), lastPath);
            mapPanel.biomeSource = biomeSource;
        } catch (OperationNotSupportedException e) {
            System.out.println("ERROR: Opended dimension is not of type minecraft:multi_noise");
            loadVoid(mapPanel);
        } catch (JSONException e) {
            System.out.println("ERROR: JSON format not correct");
            e.printStackTrace();
            loadVoid(mapPanel);
        } catch (FileNotFoundException e) {
            System.out.println("ERROR: Failed to open file");
            e.printStackTrace();
            loadVoid(mapPanel);
        }

        mapPanel.redrawImage();
    }

    private static void loadVoid(MapPanel mapPanel){
        mapPanel.biomeSource = MultiNoiseStringBiomeSource.voidSource();
    }
}

