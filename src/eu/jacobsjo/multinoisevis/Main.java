package eu.jacobsjo.multinoisevis;

import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
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
import java.io.IOException;
import java.util.NoSuchElementException;
import java.util.Scanner;

public class Main {

    private static String lastPath = "";

    private static Dimension dimension;

    public static void main(String[] args) {
        SharedConstants.setVersion(new DummyVersion());
        Bootstrap.bootStrap();

        dimension = Dimension.getInstance();

        dimension.seed = System.currentTimeMillis();
        MultiNoiseBiomeResourceSource biomeSource = MultiNoiseBiomeResourceSource.overworld(dimension.seed);

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
        JMenuItem eOpenOverworld = new JMenuItem("Open Vanilla Overworld");
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
                        mapPanel.biomeSource = loadJson(lastPath);
                        mapPanel.redrawImage();
                        eOpenNether.setEnabled(true);
                        eOpenOverworld.setEnabled(true);
                        eSetSeed.setEnabled(!dimension.fixedSeed);
                        eRandomSeed.setEnabled(!dimension.fixedSeed);
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

        eOpenOverworld.addActionListener(actionEvent -> {
            mapPanel.biomeSource = MultiNoiseBiomeResourceSource.overworld(dimension.seed);
            mapPanel.redrawImage();
            dimension.name = "minecraft:overworld";
            dimension.fixedSeed = false;
            eOpenOverworld.setEnabled(false);
            eOpenNether.setEnabled(true);
            eSetSeed.setEnabled(true);
            eRandomSeed.setEnabled(true);
            try {
                monitor.stop();
                monitor.getObservers().forEach(monitor::removeObserver);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        eOpenOverworld.setEnabled(false);
        contextMenu.add(eOpenOverworld);

        eOpenNether.addActionListener(actionEvent -> {
            mapPanel.biomeSource = MultiNoiseBiomeResourceSource.nether(dimension.seed);
            mapPanel.redrawImage();
            dimension.name = "minecraft:the_nether";
            dimension.fixedSeed = false;
            eOpenOverworld.setEnabled(true);
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
        eOpenNether.setEnabled(true);
        contextMenu.add(eOpenNether);

        eSetSeed.addActionListener(actionEvent -> {
            String s = (String) JOptionPane.showInputDialog(frame, "Set seed: ", "Seed", JOptionPane.PLAIN_MESSAGE, null, null, dimension.seed);
            long ss;
            try {
                ss = Long.parseLong(s);
            } catch (NumberFormatException e) {
                ss = s.hashCode();
            }
            dimension.seed = ss;
            mapPanel.biomeSource = mapPanel.biomeSource.withSeed(ss);
            mapPanel.redrawImage();
        });
        contextMenu.add(eSetSeed);

        eRandomSeed.addActionListener(actionEvent -> {
            dimension.seed = System.currentTimeMillis();
            mapPanel.biomeSource = mapPanel.biomeSource.withSeed(dimension.seed);
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

        JMenuItem eToggleHillshade = new JMenuItem("Toggle Hillshading");
        eToggleHillshade.addActionListener(actionEvent -> {
            mapPanel.toggleHillshadeEnabled();;
            mapPanel.redrawImage();
            mapPanel.sceduleRedraw();
        });
        contextMenu.add(eToggleHillshade);

        JMenu eOpenVoronoiVertical = new JMenu("Set Voronoi Diagram Vertical");
        JMenuItem eOpenVoronoiVertical_temp = new JMenuItem("Temperature");
        eOpenVoronoiVertical_temp.addActionListener(actionEvent -> mapPanel.setFirstVoronoiParameter(MapPanel.Parameter.TEMPERATURE));
        JMenuItem eOpenVoronoiVertical_humid = new JMenuItem("Humidity");
        eOpenVoronoiVertical_humid.addActionListener(actionEvent -> mapPanel.setFirstVoronoiParameter(MapPanel.Parameter.HUMIDITY));
        JMenuItem eOpenVoronoiVertical_cont = new JMenuItem("Continentalness");
        eOpenVoronoiVertical_cont.addActionListener(actionEvent -> mapPanel.setFirstVoronoiParameter(MapPanel.Parameter.CONTINENTALNESS));
        JMenuItem eOpenVoronoiVertical_ero = new JMenuItem("Erosion");
        eOpenVoronoiVertical_ero.addActionListener(actionEvent -> mapPanel.setFirstVoronoiParameter(MapPanel.Parameter.EROSION));
        JMenuItem eOpenVoronoiVertical_weird = new JMenuItem("Weirdness");
        eOpenVoronoiVertical_weird.addActionListener(actionEvent -> mapPanel.setFirstVoronoiParameter(MapPanel.Parameter.WEIRDNESS));
        JMenuItem eOpenVoronoiVertical_depth = new JMenuItem("Depth");
        eOpenVoronoiVertical_depth.addActionListener(actionEvent -> mapPanel.setFirstVoronoiParameter(MapPanel.Parameter.DEPTH));
        eOpenVoronoiVertical.add(eOpenVoronoiVertical_temp);
        eOpenVoronoiVertical.add(eOpenVoronoiVertical_humid);
        eOpenVoronoiVertical.add(eOpenVoronoiVertical_cont);
        eOpenVoronoiVertical.add(eOpenVoronoiVertical_ero);
        eOpenVoronoiVertical.add(eOpenVoronoiVertical_weird);
        eOpenVoronoiVertical.add(eOpenVoronoiVertical_depth);
        contextMenu.add(eOpenVoronoiVertical);

        JMenu eOpenVoronoiHorizontal = new JMenu("Set Voronoi Diagram Horozontal");
        JMenuItem eOpenVoronoiHorizontal_temp = new JMenuItem("Temperature");
        eOpenVoronoiHorizontal_temp.addActionListener(actionEvent -> mapPanel.setSecondVoronoiParameter(MapPanel.Parameter.TEMPERATURE));
        JMenuItem eOpenVoronoiHorizontal_humid = new JMenuItem("Humidity");
        eOpenVoronoiHorizontal_humid.addActionListener(actionEvent -> mapPanel.setSecondVoronoiParameter(MapPanel.Parameter.HUMIDITY));
        JMenuItem eOpenVoronoiHorizontal_cont = new JMenuItem("Continentalness");
        eOpenVoronoiHorizontal_cont.addActionListener(actionEvent -> mapPanel.setSecondVoronoiParameter(MapPanel.Parameter.CONTINENTALNESS));
        JMenuItem eOpenVoronoiHorizontal_ero = new JMenuItem("Erosion");
        eOpenVoronoiHorizontal_ero.addActionListener(actionEvent -> mapPanel.setSecondVoronoiParameter(MapPanel.Parameter.EROSION));
        JMenuItem eOpenVoronoiHorizontal_weird = new JMenuItem("Weirdness");
        eOpenVoronoiHorizontal_weird.addActionListener(actionEvent -> mapPanel.setSecondVoronoiParameter(MapPanel.Parameter.WEIRDNESS));
        JMenuItem eOpenVoronoiHorizontal_depth = new JMenuItem("Depth");
        eOpenVoronoiHorizontal_depth.addActionListener(actionEvent -> mapPanel.setSecondVoronoiParameter(MapPanel.Parameter.DEPTH));
        eOpenVoronoiHorizontal.add(eOpenVoronoiHorizontal_temp);
        eOpenVoronoiHorizontal.add(eOpenVoronoiHorizontal_humid);
        eOpenVoronoiHorizontal.add(eOpenVoronoiHorizontal_cont);
        eOpenVoronoiHorizontal.add(eOpenVoronoiHorizontal_ero);
        eOpenVoronoiHorizontal.add(eOpenVoronoiHorizontal_weird);
        eOpenVoronoiHorizontal.add(eOpenVoronoiHorizontal_depth);
        contextMenu.add(eOpenVoronoiHorizontal);

        return contextMenu;
    }

    private static MultiNoiseBiomeResourceSource loadJson(String path) throws OperationNotSupportedException, JSONException, FileNotFoundException, NoSuchElementException {
        String json;
        File file = new File(path);

        File parent1 = file.getParentFile();
        File parent2 = parent1.getParentFile();
        File parent3 = parent2.getParentFile();

        String worldname = "";
        if (parent1.getName().equals("dimension") && parent3.getName().equals("data")){
            worldname = parent2.getName() + ":" + file.getName().substring(0, file.getName().length()-5);
        }

        json = new Scanner(file).useDelimiter("\\Z").next();
        dimension.name = worldname;
        dimension.fixedSeed = true;
        return MultiNoiseJsonReader.readJson(json);
    }

    private static void reload(MapPanel mapPanel){
        System.out.println("Reload!");
        try {
            mapPanel.biomeSource = loadJson(lastPath);
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
        mapPanel.biomeSource = new VoidBiomeResourceSource();
    }
}

