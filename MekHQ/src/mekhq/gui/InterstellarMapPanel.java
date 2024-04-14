/*
 * Copyright (c) 2011-2022 - The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MekHQ.
 *
 * MekHQ is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version.
 *
 * MekHQ is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MekHQ. If not, see <http://www.gnu.org/licenses/>.
 */
package mekhq.gui;

import megamek.codeUtilities.ObjectUtility;
import megamek.common.EquipmentType;
import megamek.common.annotations.Nullable;
import mekhq.MHQConstants;
import mekhq.MekHQ;
import mekhq.campaign.Campaign;
import mekhq.campaign.JumpPath;
import mekhq.campaign.universe.*;
import mekhq.campaign.universe.Faction.Tag;
import org.apache.logging.log4j.LogManager;

import javax.imageio.ImageIO;
import javax.swing.Timer;
import javax.swing.*;
import javax.vecmath.Vector2d;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.time.LocalDate;
import java.util.List;
import java.util.*;

/**
 * This is not functional yet. Just testing things out.
 * A lot of this code is borrowed from InterstellarMap.java in MekWars
 * @author  Jay Lawson (jaylawson39 at yahoo.com)
 */
public class InterstellarMapPanel extends JPanel {
    private static final Vector2d[] BASE_HEXCOORDS = {
        new Vector2d(1.0, 0.0),
        new Vector2d(Math.cos(Math.PI / 3.0), Math.sin(Math.PI / 3.0)),
        new Vector2d(Math.cos(2.0 * Math.PI / 3.0), Math.sin(2.0 * Math.PI / 3.0)),
        new Vector2d(-1.0, 0.0),
        new Vector2d(Math.cos(4.0 * Math.PI / 3.0), Math.sin(4.0 * Math.PI / 3.0)),
        new Vector2d(Math.cos(5.0 * Math.PI / 3.0), Math.sin(5.0 * Math.PI / 3.0))
    };

    private JLayeredPane pane;
    private JPanel mapPanel;
    private JViewport optionView;
    private JPanel optionPanel;
    private JButton optionButton;

    // Map view options
    private JRadioButton optFactions;
    private JRadioButton optTech;
    private JRadioButton optIndustry;
    private JRadioButton optRawMaterials;
    private JRadioButton optOutput;
    private JRadioButton optAgriculture;
    private JRadioButton optPopulation;
    private JRadioButton optHPG;
    private JRadioButton optRecharge;

    private JCheckBox optEmptySystems;
    private JCheckBox optHPGNetwork;
    private JCheckBox optISWAreas;

    private Timer optionPanelTimer;
    private boolean optionPanelHidden;

    private ArrayList<PlanetarySystem> systems;

    private JumpPath jumpPath;
    private Campaign campaign;
    private InnerStellarMapConfig conf = new InnerStellarMapConfig();
    private CampaignGUI hqview;
    private PlanetarySystem selectedSystem = null;
    private Point lastMousePos = null;
    private int mouseMod = 0;

    private transient double minX;
    private transient double minY;
    private transient double maxX;
    private transient double maxY;
    private transient LocalDate now;

    public InterstellarMapPanel(Campaign c, CampaignGUI view) {
        campaign = c;
        systems = campaign.getSystems();
        hqview = view;
        jumpPath = new JumpPath();
        optionPanelHidden = true;
        optionPanelTimer = new Timer(50, new ActionListener() {
            Point viewPoint = new Point();

            @Override
            public void actionPerformed(ActionEvent e) {
                int width = optionView.getWidth();
                int height = optionView.getHeight();
                int maxWidth = optionPanel.getWidth();
                int maxHeight = optionPanel.getHeight();
                int minWidth = 30;
                int minHeight = 30;
                if (optionPanelHidden && ((width !=  minWidth) || (height != minHeight))) {
                    width = Math.max(width - maxWidth / 5, minWidth);
                    height = Math.max(height - maxHeight / 5, minHeight);
                } else if (!optionPanelHidden && ((width != maxWidth) || (height != maxHeight))) {
                    width = Math.min(width + maxWidth / 5, maxWidth);
                    height = Math.min(height + maxHeight / 5, maxHeight);
                } else {
                    optionPanelTimer.stop();
                    return;
                }
                optionView.setBounds(pane.getParent().getWidth() - 10 - width, pane.getParent().getHeight() - 10 - height, width, height);
                viewPoint.move(0, maxHeight - minHeight);
                optionView.setViewPosition(viewPoint);
                optionView.revalidate();
                repaint();
            }
        });

        setBorder(BorderFactory.createLineBorder(Color.black));

        addKeyListener(new KeyAdapter() {
            /** Handle the key pressed event from the text field. */
            @Override
            public void keyPressed(KeyEvent e) {
                int keyCode = e.getKeyCode();
                boolean moved = false;
                if (keyCode == KeyEvent.VK_LEFT) {
                    conf.centerY -= 1.0;
                    moved = true;
                }
                if (keyCode == KeyEvent.VK_RIGHT) {
                    conf.centerY += 1.0;
                    moved = true;
                }
                if (keyCode == KeyEvent.VK_DOWN) {
                    conf.centerX += 1.0;
                    moved = true;
                }
                if (keyCode == KeyEvent.VK_UP) {
                    conf.centerX -= 1.0;
                    moved = true;
                }
                if (moved) {
                    repaint();
                }
            }
        });

        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                lastMousePos = new Point(e.getX(), e.getY());
            }

            @Override
            public void mouseExited(MouseEvent e) {
                lastMousePos = null;
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                maybeShowPopup(e);
                mouseMod = 0;
            }

            @Override
            public void mousePressed(MouseEvent e) {
                maybeShowPopup(e);
                mouseMod = e.getButton();
            }


            public void maybeShowPopup(MouseEvent e) {
                if (e.isPopupTrigger()) {
                    JPopupMenu popup = new JPopupMenu();
                    JMenuItem item = new JMenuItem("Zoom In");
                    item.addActionListener(ae -> zoom(1.5, lastMousePos));
                    popup.add(item);
                    item = new JMenuItem("Zoom Out");
                    item.addActionListener(ae -> zoom(0.5, lastMousePos));
                    popup.add(item);
                    JMenu centerM = new JMenu("Center Map");
                    item = new JMenuItem("On Selected Planet");
                    item.setEnabled(selectedSystem != null);
                    if (selectedSystem != null) {
                        // only add if there is a planet to center on
                        item.addActionListener(ae -> center(selectedSystem));
                    }
                    centerM.add(item);
                    item = new JMenuItem("On Current Location");
                    item.setEnabled(campaign.getCurrentSystem() != null);
                    if (campaign.getCurrentSystem() != null) {
                        // only add if there is a planet to center on
                        item.addActionListener(evt -> {
                            selectedSystem = campaign.getCurrentSystem();
                            center(campaign.getCurrentSystem());
                        });
                    }
                    centerM.add(item);
                    item = new JMenuItem("On Terra");
                    item.addActionListener(evt -> {
                        conf.centerX = 0.0;
                        conf.centerY = 0.0;
                        repaint();
                    });
                    centerM.add(item);
                    popup.add(centerM);
                    item = new JMenuItem("Cancel Current Trip");
                    item.setEnabled(null != campaign.getLocation().getJumpPath());
                    item.addActionListener(evt -> {
                        campaign.getLocation().setJumpPath(null);
                        repaint();
                    });
                    popup.add(item);
                    item = new JMenuItem("Save Map (64 Mpx at current zoom level) ...");
                    item.setEnabled(true);
                    item.addActionListener(evt -> {
                        final int imgSize = 8192;
                        BufferedImage img = new BufferedImage(imgSize, imgSize, BufferedImage.TYPE_INT_RGB);
                        Graphics g = img.createGraphics();
                        int originalWidth = getWidth();
                        int originalHeight = getHeight();
                        double originalX = conf.centerX;
                        double originalY = conf.centerY;
                        try {
                            Optional<File> file = FileDialogs.saveStarMap(hqview.getFrame());
                            if (file.isPresent()) {
                                mapPanel.setSize(imgSize, imgSize);
                                conf.centerX += (imgSize - originalWidth) / conf.scale / 2.0;
                                conf.centerY += (imgSize - originalHeight) / conf.scale / 2.0;
                                mapPanel.print(g);
                                ImageIO.write(img, "png", file.get());
                            }
                        } catch (Exception ex) {
                            LogManager.getLogger().error("", ex);
                        }
                        conf.centerX = originalX;
                        conf.centerY = originalY;
                        g.dispose();
                        mapPanel.repaint();
                    });
                    popup.add(item);
                    JMenu menuGM = new JMenu("GM Mode");
                    item = new JMenuItem("Move to selected planet");
                    item.setEnabled((selectedSystem != null) && campaign.isGM());
                    if (selectedSystem != null) {
                        // only add if there is a planet to center on
                        item.addActionListener(evt -> {
                            campaign.moveToPlanetarySystem(selectedSystem);
                            jumpPath = new JumpPath();
                            center(selectedSystem);
                        });
                    }
                    menuGM.add(item);
                    /*
                     * TODO: re-enable this later
                    item = new JMenuItem("Edit planetary events");
                    item.setEnabled(selectedSystem != null && campaign.isGM());
                    if (selectedSystem != null) {
                        item.setText("Edit planetary events for " + selectedSystem.getPrintableName(Utilities.getDateTimeDay(campaign.getCalendar())));
                        item.addActionListener(new ActionListener() {
                            @Override
                            public void actionPerformed(ActionEvent ae) {
                                openPlanetEventEditor(selectedSystem);
                            }
                        });
                    }
                    menuGM.add(item);
                    */

                    item = new JMenuItem("Recharge Jumpdrive");
                    item.setEnabled(campaign.getLocation().isRecharging(campaign) && campaign.isGM());
                    item.addActionListener(evt -> {
                        campaign.getLocation().setRecharged(campaign);
                        campaign.addReport("GM: Jumpship drives fully charged");
                        hqview.refreshLocation();
                    });
                    menuGM.add(item);

                    popup.add(menuGM);
                    popup.show(e.getComponent(), e.getX() + 10, e.getY() + 10);
                }
            }

            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getButton() == MouseEvent.BUTTON1) {
                    if (e.getClickCount() >= 2) {
                        //center and zoom
                        changeSelectedSystem(nearestNeighbour(scr2mapX(e.getX()), scr2mapY(e.getY())));
                        if (conf.scale < 4.0) {
                            conf.scale = 4.0;
                        }
                        center(selectedSystem);
                        //bring up planetary system map
                        hqview.getMapTab().switchPlanetaryMap(selectedSystem);
                    } else {
                        PlanetarySystem target = nearestNeighbour(scr2mapX(e.getX()), scr2mapY(e.getY()));
                        if (null == target) {
                            return;
                        }
                        if (e.isAltDown()) {
                            //calculate a new jump path from the current location
                            jumpPath = campaign.calculateJumpPath(campaign.getCurrentSystem(), target);
                            selectedSystem = target;
                            repaint();
                            notifyListeners();
                            return;
                        } else if (e.isShiftDown()) {
                            //add to the existing jump path
                            PlanetarySystem lastSystem = jumpPath.getLastSystem();
                            if (null == lastSystem) {
                                lastSystem = campaign.getCurrentSystem();
                            }
                            JumpPath addPath = campaign.calculateJumpPath(lastSystem, target);
                            if (!jumpPath.isEmpty()) {
                                addPath.removeFirstSystem();
                            }
                            jumpPath.addSystems(addPath.getSystems());
                            selectedSystem = target;
                            repaint();
                            notifyListeners();
                            return;
                        }
                        changeSelectedSystem(target);
                        repaint();
                    }
                }
            }
        });

        addMouseMotionListener(new MouseAdapter() {
            @Override
            public void mouseDragged(MouseEvent e) {
                if (mouseMod != MouseEvent.BUTTON1) {
                   return;
                }
                if (lastMousePos != null) {
                    conf.centerX -= (lastMousePos.x - e.getX()) / conf.scale;
                    conf.centerY -= (lastMousePos.y - e.getY()) / conf.scale;
                    lastMousePos.x = e.getX();
                    lastMousePos.y = e.getY();
                }
                repaint();
            }

            @Override
            public void mouseMoved(MouseEvent e) {
                if (lastMousePos == null) {
                    lastMousePos = new Point(e.getX(), e.getY());
                } else {
                    lastMousePos.x = e.getX();
                    lastMousePos.y = e.getY();
                }
            }
        });

        addMouseWheelListener(new MouseAdapter() {
            @Override
            public void mouseWheelMoved(MouseWheelEvent e) {
                 zoom(Math.pow(1.175, -1 * e.getWheelRotation()), e.getPoint());
             }
        });

        pane = new JLayeredPane();
        mapPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(Color.BLACK);
                g2.fillRect(0, 0, getWidth(), getHeight());
                double size = 1 + 5 * Math.log(conf.scale);
                size = Math.max(Math.min(size, conf.maxdotSize), conf.minDotSize);

                final Stroke thick = new BasicStroke(2.0f);
                final Stroke thin = new BasicStroke(1.2f);
                final Stroke dashed = new BasicStroke(1.5f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 0, new float[] { 3 }, 0);
                final Stroke dotted = new BasicStroke(1.5f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 0, new float[] { 2, 5 }, 0);
                final Color darkCyan = new Color(0, 100, 50);

                minX = scr2mapX(- size * 2.0);
                minY = scr2mapY(getHeight() + size * 2.0);
                maxX = scr2mapX(getWidth() + size * 2.0);
                maxY = scr2mapY(- size * 2.0);
                now = campaign.getLocalDate();

                Arc2D.Double arc = new Arc2D.Double();

                // Draw auras around selected planet
                if (selectedSystem != null) {
                    final double x = map2scrX(selectedSystem.getX());
                    final double y = map2scrY(selectedSystem.getY());
                    // Contract Search Radius Aura
                    if (!campaign.getCampaignOptions().getContractMarketMethod().isNone()
                            && MekHQ.getMHQOptions().getInterstellarMapShowContractSearchRadius()) {
                        final double z = map2scrX(selectedSystem.getX()
                                + campaign.getCampaignOptions().getContractSearchRadius());
                        final double contractSearchRadius = z - x;
                        g2.setPaint(MekHQ.getMHQOptions().getInterstellarMapContractSearchRadiusColour());
                        arc.setArcByCenter(x, y, contractSearchRadius, 0, 360, Arc2D.OPEN);
                        g2.fill(arc);
                    }

                    // Acquisition Search Radius Aura
                    if (campaign.getCampaignOptions().isUsePlanetaryAcquisition()
                            && MekHQ.getMHQOptions().getInterstellarMapShowPlanetaryAcquisitionRadius()
                            && (conf.scale > MekHQ.getMHQOptions().getInterstellarMapShowPlanetaryAcquisitionRadiusMinimumZoom())) {
                        final double z = map2scrX(selectedSystem.getX()
                                + (MHQConstants.MAX_JUMP_RADIUS * campaign.getCampaignOptions().getMaxJumpsPlanetaryAcquisition()));
                        final double contractSearchRadius = z - x;
                        g2.setPaint(MekHQ.getMHQOptions().getInterstellarMapPlanetaryAcquisitionRadiusColour());
                        arc.setArcByCenter(x, y, contractSearchRadius, 0, 360, Arc2D.OPEN);
                        g2.fill(arc);
                    }

                    // Jump Radius Aura
                    if (MekHQ.getMHQOptions().getInterstellarMapShowJumpRadius()
                            && (conf.scale > MekHQ.getMHQOptions().getInterstellarMapShowJumpRadiusMinimumZoom())) {
                        final double z = map2scrX(selectedSystem.getX() + MHQConstants.MAX_JUMP_RADIUS);
                        final double jumpRadius = z - x;
                        g2.setPaint(MekHQ.getMHQOptions().getInterstellarMapJumpRadiusColour());
                        arc.setArcByCenter(x, y, jumpRadius, 0, 360, Arc2D.OPEN);
                        g2.fill(arc);
                    }

                    // Don't override HPG Network drawing
                    if (optHPGNetwork.isSelected()) {
                        final double z = map2scrX(selectedSystem.getX() + 50);
                        final double jumpRadius = z - x;
                        g2.setPaint(darkCyan);
                        g2.setStroke(dotted);
                        arc.setArcByCenter(x, y, jumpRadius, 0, 360, Arc2D.OPEN);
                        g2.draw(arc);
                    }
                }

                if ((conf.scale > 1.0) && optISWAreas.isSelected()) {
                    // IDEA: Allow for different hex sizes later on.
                    final double HEX_SIZE = 30.0;
                    final double SPACING_X = HEX_SIZE * Math.sqrt(3) / 2.0;
                    AffineTransform transform = getMap2ScrTransform();
                    int minX = (int) Math.floor(scr2mapX(0.0) / SPACING_X);
                    int maxX = (int) Math.ceil(scr2mapX(getWidth()) / SPACING_X);
                    int minY = (int) Math.floor(scr2mapY(getHeight()) / HEX_SIZE);
                    int maxY = (int) Math.ceil(scr2mapY(0.0) / HEX_SIZE);
                    GeneralPath path = new GeneralPath();
                    for (int x = minX; x <= maxX; ++ x) {
                        for (int y = minY; y <= maxY; ++ y) {
                            double coordX = x * SPACING_X;
                            double coordY = y * HEX_SIZE + (x % 2) * HEX_SIZE / 2.0;
                            setupHexPath(path, coordX, coordY, HEX_SIZE / 2.0);

                            Paint factionPaint = new Color(0.0f, 0.0f, 0.0f, 0.25f);
                            Paint linePaint = new Color(1.0f, 1.0f, 1.0f, 0.25f);
                            Set<Faction> hexFactions = new HashSet<>();
                            for (PlanetarySystem system : Systems.getInstance().getNearbySystems(coordX, coordY, (int) Math.round(HEX_SIZE * 1.3))) {
                                if (!isSystemEmpty(system) && path.contains(system.getX(), system.getY())) {
                                    hexFactions.addAll(system.getFactionSet(now));
                                }
                            }

                            path.transform(transform);

                            if (hexFactions.size() == 1) {
                                // Single-faction hex
                                Color factionColor = hexFactions.iterator().next().getColor();
                                float[] colorComponents = new float[4];
                                factionColor.getComponents(colorComponents);
                                factionPaint = new Color(colorComponents[0], colorComponents[1], colorComponents[2], 0.25f);
                                Color lineColor = factionColor.brighter();
                                lineColor.getComponents(colorComponents);
                                linePaint = new Color(colorComponents[0], colorComponents[1], colorComponents[2], 0.25f);
                            } else if (hexFactions.size() > 1) {
                                // Create the painted stripes data
                                int factionSize = hexFactions.size();
                                Iterator<Faction> factionIterator = hexFactions.iterator();
                                float[] colorComponents = new float[4];
                                float[] paintFractions = new float[factionSize * 2];
                                Color[] paintColors = new Color[factionSize * 2];
                                for (int i = 0; i < factionSize; ++ i) {
                                    paintFractions[i * 2] = i * (1.0f / factionSize) + 0.001f;
                                    paintFractions[i * 2 + 1] = (i + 1) * (1.0f / factionSize);
                                    Color factionColor = factionIterator.next().getColor();
                                    factionColor.getComponents(colorComponents);
                                    factionColor = new Color(colorComponents[0], colorComponents[1], colorComponents[2], 0.25f);
                                    paintColors[i * 2] = factionColor;
                                    paintColors[i * 2 + 1] = factionColor;
                                }
                                paintFractions[0] = 0.0f;

                                // Determine where to anchor the stripes
                                Point2D firstPoint = new Point2D.Double(map2scrX(coordX), map2scrY(coordY));
                                Point2D secondPoint = new Point2D.Double(
                                    firstPoint.getX() + 6 * conf.scale,
                                    firstPoint.getY() + 6 * conf.scale);
                                factionPaint = new LinearGradientPaint(
                                    firstPoint, secondPoint, paintFractions, paintColors,
                                    MultipleGradientPaint.CycleMethod.REPEAT);
                                linePaint = new Color(1.0f, 0.2f, 0.0f, 0.5f);
                            }
                            g2.setPaint(factionPaint);
                            g2.fill(path);
                            g2.setPaint(linePaint);
                            Shape clip = g2.getClip();
                            g2.clip(path);
                            g2.setStroke(new BasicStroke(4.0f));
                            g2.draw(path);
                            g2.setClip(clip);
                        }
                    }
                }

                // draw a jump path
                g2.setStroke(new BasicStroke(1.0f));
                for (int i = 0; i < jumpPath.size(); i++) {
                    PlanetarySystem systemB = jumpPath.get(i);
                    double x = map2scrX(systemB.getX());
                    double y = map2scrY(systemB.getY());
                    // lest try rings
                    g2.setPaint(Color.WHITE);
                    arc.setArcByCenter(x, y, size * 1.8, 0, 360, Arc2D.OPEN);
                    g2.fill(arc);
                    g2.setPaint(Color.BLACK);
                    arc.setArcByCenter(x, y, size * 1.6, 0, 360, Arc2D.OPEN);
                    g2.fill(arc);
                    g2.setPaint(Color.WHITE);
                    arc.setArcByCenter(x, y, size * 1.4, 0, 360, Arc2D.OPEN);
                    g2.fill(arc);
                    g2.setPaint(Color.BLACK);
                    arc.setArcByCenter(x, y, size * 1.2, 0, 360, Arc2D.OPEN);
                    g2.fill(arc);
                    if (i > 0) {
                        PlanetarySystem systemA = jumpPath.get(i-1);
                        g2.setPaint(Color.WHITE);
                        g2.draw(new Line2D.Double(map2scrX(systemA.getX()), map2scrY(systemA.getY()), map2scrX(systemB.getX()), map2scrY(systemB.getY())));
                    }
                }

                // Brute-force HPG network drawing. Will be optimised
                if (optHPGNetwork.isSelected()) {
                    // Grab the network from the planet manager
                    Collection<Systems.HPGLink> hpgNetwork = Systems.getInstance().getHPGNetwork(now);

                    for (PlanetarySystem system : systems) {
                        if (isSystemVisible(system, true)) {
                            double x = map2scrX(system.getX());
                            double y = map2scrY(system.getY());
                            int hpgRating = ObjectUtility.nonNull(system.getHPG(now), EquipmentType.RATING_X);
                            if (hpgRating == EquipmentType.RATING_A) {
                                g2.setPaint(Color.CYAN);
                                arc.setArcByCenter(x, y, size * 1.6, 0, 360, Arc2D.OPEN);
                                g2.setStroke(thick);
                                g2.draw(arc);
                            }
                            if (hpgRating == EquipmentType.RATING_A || hpgRating == EquipmentType.RATING_B) {
                                g2.setPaint(Color.CYAN);
                                arc.setArcByCenter(x, y, size * 1.3, 0, 360, Arc2D.OPEN);
                                g2.setStroke(thin);
                                g2.draw(arc);
                            }
                            if (hpgRating == EquipmentType.RATING_C) {
                                g2.setPaint(Color.CYAN);
                                arc.setArcByCenter(x, y, size * 1.3, 0, 360, Arc2D.OPEN);
                                g2.setStroke(dashed);
                                g2.draw(arc);
                            }
                            if (hpgRating == EquipmentType.RATING_D) {
                                g2.setPaint(darkCyan);
                                arc.setArcByCenter(x, y, size * 1.3, 0, 360, Arc2D.OPEN);
                                g2.setStroke(dotted);
                                g2.draw(arc);
                            }
                        }
                    }
                    for (Systems.HPGLink link : hpgNetwork) {
                        PlanetarySystem p1 = link.primary;
                        PlanetarySystem p2 = link.secondary;
                        if (isSystemVisible(p1, false) || isSystemVisible(p2, false)) {
                            if (link.rating == EquipmentType.RATING_A) {
                                g2.setPaint(Color.CYAN);
                                g2.setStroke(thick);
                                g2.draw(new Line2D.Double(map2scrX(p1.getX()), map2scrY(p1.getY()), map2scrX(p2.getX()), map2scrY(p2.getY())));
                            }
                            if (link.rating == EquipmentType.RATING_B) {
                                g2.setPaint(Color.CYAN);
                                g2.setStroke(dashed);
                                g2.draw(new Line2D.Double(map2scrX(p1.getX()), map2scrY(p1.getY()), map2scrX(p2.getX()), map2scrY(p2.getY())));
                            }
                        }
                    }
                    g2.setStroke(new BasicStroke(1.0f));
                }

                // check to see if the unit is traveling on a jump path currently and if so
                // draw this one too, in a different color
                if (null != campaign.getLocation().getJumpPath()) {
                    for (int i = 0; i < campaign.getLocation().getJumpPath().size(); i++) {
                        PlanetarySystem systemB = campaign.getLocation().getJumpPath().get(i);
                        double x = map2scrX(systemB.getX());
                        double y = map2scrY(systemB.getY());
                        // lest try rings
                        g2.setPaint(Color.YELLOW);
                        arc.setArcByCenter(x, y, size * 1.8, 0, 360, Arc2D.OPEN);
                        g2.fill(arc);
                        g2.setPaint(Color.BLACK);
                        arc.setArcByCenter(x, y, size * 1.6, 0, 360, Arc2D.OPEN);
                        g2.fill(arc);
                        g2.setPaint(Color.YELLOW);
                        arc.setArcByCenter(x, y, size * 1.4, 0, 360, Arc2D.OPEN);
                        g2.fill(arc);
                        g2.setPaint(Color.BLACK);
                        arc.setArcByCenter(x, y, size * 1.2, 0, 360, Arc2D.OPEN);
                        g2.fill(arc);
                        if (i > 0) {
                            PlanetarySystem systemA = campaign.getLocation().getJumpPath().get(i - 1);
                            g2.setPaint(Color.YELLOW);
                            g2.draw(new Line2D.Double(map2scrX(systemA.getX()), map2scrY(systemA.getY()),
                                    map2scrX(systemB.getX()), map2scrY(systemB.getY())));
                        }
                    }
                }

                Map<Faction, String> capitals = new HashMap<>();
                for (Faction faction : Factions.getInstance().getFactions()) {
                    capitals.put(faction, faction.getStartingPlanet(campaign.getLocalDate()));
                }

                for (PlanetarySystem system : systems) {
                    if (isSystemVisible(system, false)) {
                        double x = map2scrX(system.getX());
                        double y = map2scrY(system.getY());
                        if (system.equals(campaign.getCurrentSystem())) {
                            // lets try rings
                            g2.setPaint(Color.ORANGE);
                            arc.setArcByCenter(x, y, size * 1.8, 0, 360, Arc2D.OPEN);
                            g2.fill(arc);
                            g2.setPaint(Color.BLACK);
                            arc.setArcByCenter(x, y, size * 1.6, 0, 360, Arc2D.OPEN);
                            g2.fill(arc);
                            g2.setPaint(Color.ORANGE);
                            arc.setArcByCenter(x, y, size * 1.4, 0, 360, Arc2D.OPEN);
                            g2.fill(arc);
                            g2.setPaint(Color.BLACK);
                            arc.setArcByCenter(x, y, size * 1.2, 0, 360, Arc2D.OPEN);
                            g2.fill(arc);
                        }
                        if ((null != selectedSystem) && selectedSystem.equals(system)) {
                            // lets try rings
                            g2.setPaint(Color.WHITE);
                            arc.setArcByCenter(x, y, size * 1.8, 0, 360, Arc2D.OPEN);
                            g2.fill(arc);
                            g2.setPaint(Color.BLACK);
                            arc.setArcByCenter(x, y, size * 1.6, 0, 360, Arc2D.OPEN);
                            g2.fill(arc);
                            g2.setPaint(Color.WHITE);
                            arc.setArcByCenter(x, y, size * 1.4, 0, 360, Arc2D.OPEN);
                            g2.fill(arc);
                            g2.setPaint(Color.BLACK);
                            arc.setArcByCenter(x, y, size * 1.2, 0, 360, Arc2D.OPEN);
                            g2.fill(arc);
                        }

                        // if factions are selected then we need to do it differently, because
                        // of multiple factions per planet
                        if (isFactionsSelected()) {
                            Set<Faction> factions = system.getFactionSet(now);
                            if ((null != factions) && !isSystemEmpty(system)) {
                                int i = 0;
                                for (Faction faction : factions) {
                                    if (capitals.get(faction).equals(system.getId())) {
                                        g2.setPaint(new Color(212, 175, 55));
                                        arc.setArcByCenter(x, y, size + 3, 0, 360.0 * (1 - ((double) i) / factions.size()), Arc2D.PIE);
                                        g2.fill(arc);
                                    }
                                    if (campaign.getCampaignOptions().isUseAtB()
                                            && campaign.getAtBConfig().isHiringHall(system.getId(), campaign.getLocalDate())) {
                                        g2.setPaint(new Color(248, 150, 60));
                                        arc.setArcByCenter(x, y, size + 4, 0, 360.0 * (1 - ((double) i) / factions.size()), Arc2D.PIE);
                                        g2.fill(arc);
                                    }
                                    g2.setPaint(faction.getColor());
                                    arc.setArcByCenter(x, y, size, 0, 360.0 * (1 - ((double) i) / factions.size()), Arc2D.PIE);
                                    g2.fill(arc);
                                    ++ i;
                                }
                            } else {
                                // Just a black circle then
                                g2.setPaint(new Color(0.0f, 0.0f, 0.0f, 0.5f));
                                arc.setArcByCenter(x, y, size, 0, 360.0, Arc2D.PIE);
                                g2.fill(arc);
                            }
                        } else {
                            g2.setPaint(getSystemColor(system));
                            arc.setArcByCenter(x, y, size, 0, 360.0, Arc2D.PIE);
                            g2.fill(arc);
                        }
                    }
                }

                // cycle through planets again and assign names - to make sure names go on outside
                for (PlanetarySystem system : systems) {
                    if (isSystemVisible(system, !optEmptySystems.isSelected())) {
                        double x = map2scrX(system.getX());
                        double y = map2scrY(system.getY());
                        if ((conf.showPlanetNamesThreshold == 0) || (conf.scale > conf.showPlanetNamesThreshold)
                                || jumpPath.contains(system)
                                || ((campaign.getLocation().getJumpPath() != null) && campaign.getLocation().getJumpPath().contains(system))) {
                            final String planetName = system.getPrintableName(campaign.getLocalDate());
                            final float xPos = (float) (x + size * 1.8);
                            final float yPos = (float) y;
                            g2.setPaint(Color.BLACK);
                            g2.drawString(planetName, xPos - 1f, yPos - 1f);
                            g2.drawString(planetName, xPos + 1f, yPos - 1f);
                            g2.drawString(planetName, xPos + 1f, yPos + 1f);
                            g2.drawString(planetName, xPos - 1f, yPos + 1f);
                            g2.setPaint(Color.WHITE);
                            g2.drawString(planetName, xPos, yPos);
                        }
                    }
                }
            }
        };
        pane.add(mapPanel, Integer.valueOf(1));

        optionPanel = new JPanel();
        optionPanel.setLayout(new BoxLayout(optionPanel, BoxLayout.Y_AXIS));
        optionPanel.setBackground(new Color(0, 100, 230, 200));
        optionPanel.setBorder(BorderFactory.createEmptyBorder(3, 3, 3, 3));

        Icon checkboxIcon = new ImageIcon("data/images/misc/checkbox_unselected.png");
        Icon checkboxSelectedIcon = new ImageIcon("data/images/misc/checkbox_selected.png");

        optionPanel.add(createLabel("Color:"));

        optFactions = createOptionRadioButton("Faction", checkboxIcon, checkboxSelectedIcon);
        optionPanel.add(optFactions);
        optTech = createOptionRadioButton("Technology", checkboxIcon, checkboxSelectedIcon);
        optionPanel.add(optTech);
        optIndustry = createOptionRadioButton("Industry", checkboxIcon, checkboxSelectedIcon);
        optionPanel.add(optIndustry);
        optRawMaterials = createOptionRadioButton("Raw Materials", checkboxIcon, checkboxSelectedIcon);
        optionPanel.add(optRawMaterials);
        optOutput = createOptionRadioButton("Output", checkboxIcon, checkboxSelectedIcon);
        optionPanel.add(optOutput);
        optAgriculture = createOptionRadioButton("Agriculture", checkboxIcon, checkboxSelectedIcon);
        optionPanel.add(optAgriculture);
        optPopulation = createOptionRadioButton("Population", checkboxIcon, checkboxSelectedIcon);
        optionPanel.add(optPopulation);
        optHPG = createOptionRadioButton("HPG", checkboxIcon, checkboxSelectedIcon);
        optionPanel.add(optHPG);
        optRecharge = createOptionRadioButton("Recharge Stations", checkboxIcon, checkboxSelectedIcon);
        optionPanel.add(optRecharge);

        ButtonGroup colorChoice = new ButtonGroup();
        colorChoice.add(optFactions);
        colorChoice.add(optTech);
        colorChoice.add(optIndustry);
        colorChoice.add(optRawMaterials);
        colorChoice.add(optOutput);
        colorChoice.add(optAgriculture);
        colorChoice.add(optPopulation);
        colorChoice.add(optHPG);
        colorChoice.add(optRecharge);
        //factions by default
        optFactions.setSelected(true);

        optionPanel.add(Box.createRigidArea(new Dimension(0,10)));
        optionPanel.add(createLabel("Options:"));
        optEmptySystems = createOptionCheckBox("Empty systems", checkboxIcon, checkboxSelectedIcon);
        optEmptySystems.setSelected(true);
        optionPanel.add(optEmptySystems);
        optISWAreas = createOptionCheckBox("ISW Areas", checkboxIcon, checkboxSelectedIcon);
        optionPanel.add(optISWAreas);
        optHPGNetwork = createOptionCheckBox("HPG Network", checkboxIcon, checkboxSelectedIcon);
        optionPanel.add(optHPGNetwork);

        optionButton = new JButton();
        optionButton.setPreferredSize(new Dimension(24, 24));
        optionButton.setMargin(new Insets(0, 0, 0, 0));
        optionButton.setBorder(BorderFactory.createEmptyBorder());
        optionButton.setBackground(new Color(0, 100, 230, 150));
        optionButton.setFocusable(false);
        optionButton.setIcon(new ImageIcon("data/images/misc/option_button.png"));
        optionButton.addActionListener(e -> {
            optionPanelHidden = !optionPanelHidden;
            optionPanelTimer.start();
        });
        optionPanel.add(optionButton);

        optionView = new JViewport();
        optionView.add(optionPanel);

        pane.add(optionView, Integer.valueOf(10));

        add(pane);

        optionPanelTimer.start();
    }

    public void setCampaign(Campaign c) {
        this.campaign = c;
        this.systems = campaign.getSystems();
        repaint();
    }

    public void setJumpPath(JumpPath path) {
        jumpPath = path;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        int width = getWidth();
        int height = getHeight();
        pane.setBounds(0, 0, width, height);
        mapPanel.setBounds(0, 0, width, height);
        optionView.setBounds(width - 10 - optionView.getWidth(), height - 10 - optionView.getHeight(), optionView.getWidth(), optionView.getHeight());

        super.paintComponent(g);
    }

    private JLabel createLabel(String text) {
        JLabel label = new JLabel(text);
        label.setOpaque(false);
        label.setForeground(new Color(150, 220, 255));
        label.setFont(label.getFont().deriveFont(Font.BOLD));
        return(label);
    }

    private JCheckBox createOptionCheckBox(String text, Icon checkboxIcon, Icon checkboxSelectedIcon) {
        JCheckBox checkBox = new JCheckBox(text);
        checkBox.setOpaque(false);
        checkBox.setForeground(new Color(150, 220, 255));
        checkBox.setFocusable(false);
        checkBox.setFont(checkBox.getFont().deriveFont(Font.BOLD));
        checkBox.setPreferredSize(new Dimension(150, 20));
        checkBox.setIcon(checkboxIcon);
        checkBox.setSelectedIcon(checkboxSelectedIcon);
        checkBox.setSelected(false);
        checkBox.addActionListener(e -> repaint());
        return checkBox;
    }

    private JRadioButton createOptionRadioButton(String text, Icon checkboxIcon, Icon checkboxSelectedIcon) {
        JRadioButton radioButton = new JRadioButton(text);
        radioButton.setOpaque(false);
        radioButton.setForeground(new Color(150, 220, 255));
        radioButton.setFocusable(false);
        radioButton.setFont(radioButton.getFont().deriveFont(Font.BOLD));
        radioButton.setPreferredSize(new Dimension(150, 20));
        radioButton.setIcon(checkboxIcon);
        radioButton.setSelectedIcon(checkboxSelectedIcon);
        radioButton.setSelected(false);
        radioButton.addActionListener(e -> repaint());
        return radioButton;
    }

    private void setupHexPath(@Nullable GeneralPath path, double centerX, double centerY, double radius) {
        if (null == path) {
            return;
        }
        radius *= Math.sqrt(4.0 / 3.0);
        path.reset();
        path.moveTo(centerX + radius * BASE_HEXCOORDS[0].x, centerY + radius * BASE_HEXCOORDS[0].y);
        for (int i = 1; i < 6; ++ i) {
            path.lineTo(centerX + radius * BASE_HEXCOORDS[i].x, centerY + radius * BASE_HEXCOORDS[i].y);
        }
        path.closePath();
    }

    /**
     * Computes the map-coordinate from the screen coordinate system
     */
    private double scr2mapX(double x) {
        return (x - getWidth() / 2.0) / conf.scale - conf.centerX;
    }

    private double map2scrX(double x) {
        return getWidth() / 2.0 + (x + conf.centerX) * conf.scale;
    }

    private double scr2mapY(double y) {
        return (getHeight() / 2.0 - y) / conf.scale + conf.centerY;
    }

    private double map2scrY(double y) {
        return getHeight() / 2.0 - (y - conf.centerY) * conf.scale;
    }

    private AffineTransform getMap2ScrTransform() {
        AffineTransform transform = new AffineTransform();
        transform.translate(getWidth() / 2.0, getHeight() / 2.0);
        transform.scale(conf.scale, - conf.scale);
        transform.translate(conf.centerX, - conf.centerY);
        return transform;
    }
    public void setSelectedSystem(PlanetarySystem p) {
        selectedSystem = p;
        if (conf.scale < 4.0) {
            conf.scale = 4.0;
        }
        center(selectedSystem);
        repaint();
    }

     /**
      * Calculate the nearest neighbour for the given point
      * If anyone has a better algorithm than this stupid kind of shit, please, feel free to
      * exchange my brute force thing... An good idea would be an voronoi diagram and the sweep
      * algorithm from Steven Fortune.
      */
    private PlanetarySystem nearestNeighbour(double x, double y) {
        double minDiff = Double.MAX_VALUE;
        double diff;
        PlanetarySystem minPlanet = null;
        for (PlanetarySystem p : systems) {
            diff = Math.sqrt(Math.pow(x - p.getX(), 2) + Math.pow(y - p.getY(), 2));
            if (diff < minDiff) {
                minDiff = diff;
                minPlanet = p;
            }
        }
        return minPlanet;
    }

    private boolean isSystemEmpty(PlanetarySystem system) {
        Set<Faction> factions = system.getFactionSet(now);
        if ((null == factions) || factions.isEmpty()) {
            return true;
        }

        return factions.stream()
                .allMatch(faction -> faction.is(Tag.ABANDONED));
    }

    private boolean isSystemVisible(PlanetarySystem system, boolean hideEmpty) {
        if (null == system) {
            return false;
        }
        // The current planet and the selected one are always visible
        if (system.equals(campaign.getCurrentSystem()) || system.equals(selectedSystem)) {
            return true;
        }
        // viewport check
        double x = system.getX();
        double y = system.getY();
        if ((x < minX) || (x > maxX) || (y < minY) || (y > maxY)) {
            return false;
        }
        if (hideEmpty) {
            // Filter out "empty" systems
            return !isSystemEmpty(system);
        }
        return true;
    }

    /**
     * Activate and Center
     */
    private void center(PlanetarySystem p) {
        if (p == null) {
            return;
        }
        conf.centerX = - p.getX();
        conf.centerY = p.getY();
        repaint();
    }

    private void zoom(double percent, Point pos) {
        if (null != pos) {
            // TODO : Calculate offset to zoom at mouse position
        }
        conf.scale *= percent;
        repaint();
    }

    public PlanetarySystem getSelectedSystem() {
        return selectedSystem;
    }

    public JumpPath getJumpPath() {
        return jumpPath;
    }

    public void changeSelectedSystem(PlanetarySystem p) {
        selectedSystem = p;
        jumpPath = new JumpPath();
        notifyListeners();
    }

    /**
     * Return a planet color based on what the user has selected from the radio button options
     * @param p PlanetarySystem object
     * @return a Color
     */
    public Color getSystemColor(PlanetarySystem p) {
        // color shading is from the Viridis color palettes
        long pop = p.getPopulation(campaign.getLocalDate());

        // if no population, then just return black no matter what we asked for
        if (pop == 0L) {
            return Color.BLACK;
        }

        SocioIndustrialData socio = p.getSocioIndustrial(campaign.getLocalDate());

        if (null != socio && optTech.isSelected()) {
            switch (socio.tech) {
                case EquipmentType.RATING_F:
                case EquipmentType.RATING_E:
                    return new Color(68,1,84);
                case EquipmentType.RATING_D:
                    return new Color(59,82,139);
                case EquipmentType.RATING_C:
                    return new Color(33,144,140);
                case EquipmentType.RATING_B:
                    return new Color(93,200,99);
                case EquipmentType.RATING_A:
                    return new Color(253,231,37);
                default:
                    return Color.BLACK;
            }
        }
        if (null != socio && optIndustry.isSelected()) {
            switch (socio.industry) {
                case EquipmentType.RATING_F:
                case EquipmentType.RATING_E:
                    return new Color(0,0,4);
                case EquipmentType.RATING_D:
                    return new Color(81,18,124);
                case EquipmentType.RATING_C:
                    return new Color(182,54,121);
                case EquipmentType.RATING_B:
                    return new Color(251,136,97);
                case EquipmentType.RATING_A:
                    return new Color(252,253,191);
                default:
                    return Color.BLACK;
            }
        }
        if (null != socio && optRawMaterials.isSelected()) {
            switch (socio.rawMaterials) {
                case EquipmentType.RATING_F:
                case EquipmentType.RATING_E:
                    return new Color(13,8,135);
                case EquipmentType.RATING_D:
                    return new Color(126,3,168);
                case EquipmentType.RATING_C:
                    return new Color(204,70,120);
                case EquipmentType.RATING_B:
                    return new Color(248,148,65);
                case EquipmentType.RATING_A:
                    return new Color(240,249,33);
                default:
                    return Color.BLACK;
            }
        }
        if (null != socio && optOutput.isSelected()) {
            switch (socio.output) {
                case EquipmentType.RATING_F:
                case EquipmentType.RATING_E:
                    return new Color(0,0,4);
                case EquipmentType.RATING_D:
                    return new Color(86,15,110);
                case EquipmentType.RATING_C:
                    return new Color(187,55,84);
                case EquipmentType.RATING_B:
                    return new Color(249,140,10);
                case EquipmentType.RATING_A:
                    return new Color(252,255,164);
                default:
                    return Color.BLACK;
            }
        }
        if (null != socio && optAgriculture.isSelected()) {
            switch (socio.agriculture) {
                case EquipmentType.RATING_F:
                case EquipmentType.RATING_E:
                    return new Color(0,32,77);
                case EquipmentType.RATING_D:
                    return new Color(66,77,107);
                case EquipmentType.RATING_C:
                    return new Color(124,123,120);
                case EquipmentType.RATING_B:
                    return new Color(188,175,111);
                case EquipmentType.RATING_A:
                    return new Color(255,234,70);
                default:
                    return Color.BLACK;
            }
        }

        if (optPopulation.isSelected()) {
            // numbers based roughly on deciles of population distribution in 2750
            if (pop >= 3000000000L) {
                return new Color(253,231, 37);
            } else if (pop >= 1500000000L) {
                return new Color(180,222,44);
            } else if (pop >= 1000000000L) {
                return new Color(109,205,89);
            } else if (pop >= 500000000L) {
                return new Color(53,183,121);
            } else if (pop >= 300000000L) {
                return new Color(31,158,137);
            } else if (pop >= 200000000L) {
                return new Color(38,130,142);
            } else if (pop >= 100000000L) {
                return new Color(49,104,142);
            } else if (pop >= 25000000L) {
                return new Color(62,74,137);
            } else if (pop >= 1000000L) {
                return new Color(72,40,120);
            } else if (pop > 0L) {
                return new Color(68,1,84);
            } else {
                return Color.GRAY;
            }
        }

        if (optHPG.isSelected()) {
            Integer hpg = p.getHPG(campaign.getLocalDate());
            if (null == hpg) {
                return Color.BLACK;
            }
            // use two shades of grey for C and D as this is pony express
            switch (hpg) {
                case EquipmentType.RATING_D:
                    return new Color(84,84,84);
                case EquipmentType.RATING_C:
                    return new Color(168,168,168);
                case EquipmentType.RATING_B:
                    return new Color(222,73,104);
                case EquipmentType.RATING_A:
                    return new Color(252,253,191);
                default:
                    return Color.BLACK;
            }
        }

        if (optRecharge.isSelected()) {
            // use two shades of grey for C and D as this is pony express
            switch (p.getNumberRechargeStations(campaign.getLocalDate())) {
                case 2:
                    return new Color(240,249,33);
                case 1:
                    return new Color(225,100,98);
                case 0:
                    return new Color(128,128,128);
                default:
                    return Color.BLACK;
            }
        }

        return Color.GRAY;
    }

    /*
     * TODO: re-enable later
    private void openPlanetEventEditor(Planet p) {
        NewPlanetaryEventDialog editor = new NewPlanetaryEventDialog(null, campaign, selectedSystem);
        editor.setVisible(true);
        List<Planet.PlanetaryEvent> result = editor.getChangedEvents();
        if ((null != result) && !result.isEmpty()) {
            Planets.getInstance().updatePlanetaryEvents(p.getId(), result, true);
            Planets.getInstance().recalcHPGNetwork();
            repaint();
            notifyListeners();
        }
    }
    */

    /**
     * All configuration behaviour of InterStellarMap are saved here.
     *
     * @author Imi (immanuel.scholz@gmx.de)
     */
    static public final class InnerStellarMapConfig {
        /**
         * Whether to scale planet dots on zoom or not
         */
        int minDotSize = 3;
        int maxdotSize = 25;
        /**
         * The scaling maximum dimension
         */
        int reverseScaleMax = 100;
        /**
         * The scaling minimum dimension
         */
        int reverseScaleMin = 2;
        /**
         * Threshold to not show planet names. 0 means show always
         */
        double showPlanetNamesThreshold = 3.0;
        /**
         * The actual scale factor. 1.0 for default, higher means bigger.
         */
        double scale = 0.5;
        /**
         * The scrolling offset
         */
        double centerX = 0.0;
        double centerY = 0.0;
        /**
         * The current selected Planet-id
         */
        int planetID;
    }

    private transient List<ActionListener> listeners = new ArrayList<>();

    public void addActionListener(ActionListener l) {
        if (!listeners.contains(l)) {
            listeners.add(l);
        }
    }

    public void removeActionListener(ActionListener l) {
        listeners.remove(l);
    }

    private void notifyListeners() {
        ActionEvent ev = new ActionEvent(this, ActionEvent.ACTION_FIRST, "refresh");
        listeners.forEach(l -> l.actionPerformed(ev));
    }

    public boolean isFactionsSelected() {
        return optFactions.isSelected();
    }
}
