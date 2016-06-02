/*
 * Copyright (C) 2011-2016 MegaMek team
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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with MekHQ.  If not, see <http://www.gnu.org/licenses/>.
 */
package mekhq.gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.geom.Arc2D;
import java.awt.geom.Line2D;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLayeredPane;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JViewport;
import javax.swing.Timer;

import org.joda.time.DateTime;

import mekhq.campaign.Campaign;
import mekhq.campaign.JumpPath;
import mekhq.campaign.universe.Faction;
import mekhq.campaign.universe.Planet;
import mekhq.campaign.universe.Planets;
import mekhq.gui.dialog.NewPlanetaryEventDialog;

/**
 * This is not functional yet. Just testing things out.
 * A lot of this code is borrowed from InterstellarMap.java in MekWars
 * @author  Jay Lawson <jaylawson39 at yahoo.com>
 */
public class InterstellarMapPanel extends JPanel {
    private static final long serialVersionUID = -1110105822399704646L;
    
    private JLayeredPane pane;
    private JPanel mapPanel;
    private JViewport optionView;
    private JPanel optionPanel;
    private JButton optionButton;
    private JCheckBox optEmptySystems;
    
    private Timer optionPanelTimer;
    private boolean optionPanelHidden;
    
    private ArrayList<Planet> planets;
    private JumpPath jumpPath;
    private Campaign campaign;
    private InnerStellarMapConfig conf = new InnerStellarMapConfig();
    private CampaignGUI hqview;
    private Planet selectedPlanet = null;
    private Point lastMousePos = null;
    private int mouseMod = 0;

    private transient double minX;
    private transient double minY;
    private transient double maxX;
    private transient double maxY;
    private transient DateTime now;

    public InterstellarMapPanel(Campaign c, CampaignGUI view) {
        campaign = c;
        planets = campaign.getPlanets();
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
                if(optionPanelHidden && ((width !=  minWidth) || (height != minHeight))) {
                    width = Math.max(width - maxWidth / 5, minWidth);
                    height = Math.max(height - maxHeight / 5, minHeight);
                } else if(!optionPanelHidden && ((width != maxWidth) || (height != maxHeight))) {
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
                if(keyCode == KeyEvent.VK_LEFT) {
                    conf.centerY -= 1.0;
                    moved = true;
                }
                if(keyCode == KeyEvent.VK_RIGHT) {
                    conf.centerY += 1.0;
                    moved = true;
                }
                if(keyCode == KeyEvent.VK_DOWN) {
                    conf.centerX += 1.0;
                    moved = true;
                }
                if(keyCode == KeyEvent.VK_UP) {
                    conf.centerX -= 1.0;
                    moved = true;
                }
                if(moved) {
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
                    JMenuItem item;
                    item = new JMenuItem("Zoom In");
                    item.addActionListener(new ActionListener() {
                        @Override
                        public void actionPerformed(ActionEvent ae) {
                            zoom(1.5, lastMousePos);
                        }
                    });
                    popup.add(item);
                    item = new JMenuItem("Zoom Out");
                    item.addActionListener(new ActionListener() {
                        @Override
                        public void actionPerformed(ActionEvent ae) {
                            zoom(0.5, lastMousePos);
                        }
                    });
                    popup.add(item);
                    JMenu centerM = new JMenu("Center Map");
                    item = new JMenuItem("On Selected Planet");
                    item.setEnabled(selectedPlanet != null);
                    if (selectedPlanet != null) {// only add if there is a planet to center on
                        item.addActionListener(new ActionListener() {
                            @Override
                            public void actionPerformed(ActionEvent ae) {
                                center(selectedPlanet);
                            }
                        });
                    }
                    centerM.add(item);
                    item = new JMenuItem("On Current Location");
                    item.setEnabled(campaign.getCurrentPlanet() != null);
                    if (campaign.getCurrentPlanet() != null) {// only add if there is a planet to center on
                        item.addActionListener(new ActionListener() {
                            @Override
                            public void actionPerformed(ActionEvent ae) {
                                selectedPlanet = campaign.getCurrentPlanet();
                                center(campaign.getCurrentPlanet());
                            }
                        });
                    }
                    centerM.add(item);
                    item = new JMenuItem("On Terra");
                    item.addActionListener(new ActionListener() {
                        @Override
                        public void actionPerformed(ActionEvent ae) {
                            conf.centerX = 0.0;
                            conf.centerY = 0.0;
                            repaint();
                        }
                    });
                    centerM.add(item);
                    popup.add(centerM);
                    item = new JMenuItem("Cancel Current Trip");
                    item.setEnabled(null != campaign.getLocation().getJumpPath());
                    item.addActionListener(new ActionListener() {
                        @Override
                        public void actionPerformed(ActionEvent ae) {
                            campaign.getLocation().setJumpPath(null);
                            repaint();
                        }
                    });
                    popup.add(item);
                    JMenu menuGM = new JMenu("GM Mode");
                    item = new JMenuItem("Move to selected planet");
                    item.setEnabled(selectedPlanet != null && campaign.isGM());
                    if (selectedPlanet != null) {// only add if there is a planet to center on
                        item.addActionListener(new ActionListener() {
                            @Override
                            public void actionPerformed(ActionEvent ae) {
                                campaign.getLocation().setCurrentPlanet(selectedPlanet);
                                campaign.getLocation().setTransitTime(0.0);
                                campaign.getLocation().setJumpPath(null);
                                jumpPath = new JumpPath();
                                center(selectedPlanet);
                                hqview.refreshLocation();
                            }
                        });
                    }
                    menuGM.add(item);
                    item = new JMenuItem("Edit planetary events");
                    item.setEnabled(selectedPlanet != null && campaign.isGM());
                    if (selectedPlanet != null) {
                        item.setText("Edit planetary events for " + selectedPlanet.getPrintableName(new DateTime(campaign.getCalendar())));
                        item.addActionListener(new ActionListener() {
                            @Override
                            public void actionPerformed(ActionEvent ae) {
                                openPlanetEventEditor(selectedPlanet);
                            }
                        });
                    }
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
                        changeSelectedPlanet(nearestNeighbour(scr2mapX(e.getX()), scr2mapY(e.getY())));
                        if(conf.scale < 4.0) {
                            conf.scale = 4.0;
                        }
                        center(selectedPlanet);
                    } else {
                        Planet target = nearestNeighbour(scr2mapX(e.getX()), scr2mapY(e.getY()));
                        if(null == target) {
                            return;
                        }
                        if(e.isAltDown()) {
                            //calculate a new jump path from the current location
                            jumpPath = campaign.calculateJumpPath(campaign.getCurrentPlanet(), target);
                            selectedPlanet = target;
                            repaint();
                            hqview.refreshPlanetView();
                            return;

                        }
                        else if(e.isShiftDown()) {
                            //add to the existing jump path
                            Planet lastPlanet = jumpPath.getLastPlanet();
                            if(null == lastPlanet) {
                                lastPlanet = campaign.getCurrentPlanet();
                            }
                            JumpPath addPath = campaign.calculateJumpPath(lastPlanet, target);
                              if(!jumpPath.isEmpty()) {
                                  addPath.removeFirstPlanet();
                              }
                            jumpPath.addPlanets(addPath.getPlanets());
                              selectedPlanet = target;
                              repaint();
                              hqview.refreshPlanetView();
                              return;
                        }
                        changeSelectedPlanet(target);
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
                 zoom(Math.pow(1.5,-1 * e.getWheelRotation()), e.getPoint());
             }
        });
        
        pane = new JLayeredPane();
        mapPanel = new JPanel() {
            private static final long serialVersionUID = -6666762147393179909L;

            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g;
                g2.setColor(Color.BLACK);
                g2.fillRect(0, 0, getWidth(), getHeight());
                double size = 1 + 5 * Math.log(conf.scale);
                size = Math.max(Math.min(size, conf.maxdotSize), conf.minDotSize);
                
                minX = scr2mapX(- size * 2.0);
                minY = scr2mapY(getHeight() + size * 2.0);
                maxX = scr2mapX(getWidth() + size * 2.0);
                maxY = scr2mapY(- size * 2.0);
                now = new DateTime(campaign.getCalendar());
                
                Arc2D.Double arc = new Arc2D.Double();
                //first get the jump diameter for selected planet
                if(null != selectedPlanet && conf.scale > conf.showPlanetNamesThreshold) {
                    double x = map2scrX(selectedPlanet.getX());
                    double y = map2scrY(selectedPlanet.getY());
                    double z = map2scrX(selectedPlanet.getX() + 30);
                    double jumpRadius = (z - x);
                    g2.setPaint(Color.DARK_GRAY);
                    arc.setArcByCenter(x, y, jumpRadius, 0, 360, Arc2D.OPEN);
                    g2.fill(arc);
                }
                
                //draw a jump path
                for(int i = 0; i < jumpPath.size(); i++) {
                    Planet planetB = jumpPath.get(i);
                    double x = map2scrX(planetB.getX());
                    double y = map2scrY(planetB.getY());
                    //lest try rings
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
                    if(i > 0) {
                        Planet planetA = jumpPath.get(i-1);
                        g2.setPaint(Color.WHITE);
                        g2.draw(new Line2D.Double(map2scrX(planetA.getX()), map2scrY(planetA.getY()), map2scrX(planetB.getX()), map2scrY(planetB.getY())));
                    }
                }

                //check to see if the unit is traveling on a jump path currently and if so
                //draw this one too, in a different color
                if(null != campaign.getLocation().getJumpPath()) {
                    for(int i = 0; i < campaign.getLocation().getJumpPath().size(); i++) {
                        Planet planetB = campaign.getLocation().getJumpPath().get(i);
                        double x = map2scrX(planetB.getX());
                        double y = map2scrY(planetB.getY());
                        //lest try rings
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
                        if(i > 0) {
                            Planet planetA = campaign.getLocation().getJumpPath().get(i-1);
                            g2.setPaint(Color.YELLOW);
                            g2.draw(new Line2D.Double(map2scrX(planetA.getX()), map2scrY(planetA.getY()), map2scrX(planetB.getX()), map2scrY(planetB.getY())));
                        }
                    }
                }

                for(Planet planet : planets) {
                    if(isPlanetVisible(planet, false)) {
                        double x = map2scrX(planet.getX());
                        double y = map2scrY(planet.getY());
                        if(planet.equals(campaign.getCurrentPlanet())) {
                            //lest try rings
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
                        if(null != selectedPlanet && selectedPlanet.equals(planet)) {
                            //lest try rings
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
                        Set<Faction> factions = planet.getFactionSet(now);
                        if(null != factions) {
                            int i = 0;
                            for(Faction faction : factions) {
                                g2.setPaint(faction.getColor());
                                arc.setArcByCenter(x, y, size, 0, 360.0 * (1-((double)i)/factions.size()), Arc2D.PIE);
                                g2.fill(arc);
                                ++ i;
                            }
                        } else {
                            // Just a black circle then
                            g2.setPaint(Color.BLACK);
                            arc.setArcByCenter(x, y, size, 0, 360.0, Arc2D.PIE);
                            g2.fill(arc);
                        }
                    }
                }

                //cycle through planets again and assign names - to make sure names go on outside
                for(Planet planet : planets) {
                    if(isPlanetVisible(planet, !optEmptySystems.isSelected())) {
                        double x = map2scrX(planet.getX());
                        double y = map2scrY(planet.getY());
                        if (conf.showPlanetNamesThreshold == 0 || conf.scale > conf.showPlanetNamesThreshold
                                || jumpPath.contains(planet)
                                || (null != campaign.getLocation().getJumpPath() && campaign.getLocation().getJumpPath().contains(planet))) {
                            g2.setPaint(Color.WHITE);
                            g2.drawString(planet.getPrintableName(new DateTime(campaign.getCalendar())), (float)(x+size * 1.8), (float)y);
                        }
                    }
                }
            }
        };
        pane.add(mapPanel, Integer.valueOf(1));
        
        optionPanel = new JPanel(new BorderLayout(0, 3));
        optionPanel.setBackground(new Color(0, 100, 230, 200));
        optionPanel.setBorder(BorderFactory.createEmptyBorder(3, 3, 3, 3));
        
        optEmptySystems = new JCheckBox("Empty systems");
        optEmptySystems.setOpaque(false);
        optEmptySystems.setForeground(new Color(150, 220, 255));
        optEmptySystems.setFocusable(false);
        optEmptySystems.setFont(optEmptySystems.getFont().deriveFont(Font.BOLD));
        optEmptySystems.setPreferredSize(new Dimension(150, 20));
        optEmptySystems.setIcon(new ImageIcon("data/images/misc/checkbox_unselected.png"));
        optEmptySystems.setSelectedIcon(new ImageIcon("data/images/misc/checkbox_selected.png"));
        optEmptySystems.setSelected(true);
        optEmptySystems.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                repaint();
            }
        });
        
        optionPanel.add(optEmptySystems, BorderLayout.NORTH);
        
        optionButton = new JButton();
        optionButton.setPreferredSize(new Dimension(24, 24));
        optionButton.setMargin(new Insets(0, 0, 0, 0));
        optionButton.setBorder(BorderFactory.createEmptyBorder());
        optionButton.setBackground(new Color(0, 100, 230, 150));
        optionButton.setFocusable(false);
        optionButton.setIcon(new ImageIcon("data/images/misc/option_button.png"));
        optionButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                optionPanelHidden = !optionPanelHidden;
                optionPanelTimer.start();
            }
        });
        optionPanel.add(optionButton, BorderLayout.WEST);

        optionView = new JViewport();
        optionView.add(optionPanel);
        
        pane.add(optionView, Integer.valueOf(10));
        
        add(pane);
        
        optionPanelTimer.start();
    }

    public void setCampaign(Campaign c) {
        this.campaign = c;
        this.planets = campaign.getPlanets();
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

    public void setSelectedPlanet(Planet p) {
        selectedPlanet = p;
        if(conf.scale < 4.0) {
            conf.scale = 4.0;
        }
        center(selectedPlanet);
        repaint();
    }


     /**
     * Calculate the nearest neighbour for the given point If anyone has a better algorithm than this stupid kind of shit, please, feel free to exchange my brute force thing... An good idea would be an voronoi diagram and the sweep algorithm from Steven Fortune.
     */
    private Planet nearestNeighbour(double x, double y) {
        double minDiff = Double.MAX_VALUE;
        double diff = 0.0;
        Planet minPlanet = null;
        for(Planet p : planets) {
            diff = Math.sqrt(Math.pow(x - p.getX(), 2) + Math.pow(y - p.getY(), 2));
            if (diff < minDiff) {
                minDiff = diff;
                minPlanet = p;
            }
        }
        return minPlanet;
    }

    private boolean isPlanetVisible(Planet planet, boolean hideEmpty) {
        if(null == planet) {
            return false;
        }
        // The current planet and the selected one are always visible
        if(planet.equals(campaign.getCurrentPlanet()) || planet.equals(selectedPlanet)) {
            return true;
        }
        // viewport check
        double x = planet.getX().doubleValue();
        double y = planet.getY().doubleValue();
        if((x < minX) || (x > maxX) || (y < minY) || (y > maxY)) {
            return false;
        }
        if(hideEmpty) {
            // Filter out "empty" systems
            Set<Faction> factions = planet.getFactionSet(now);
            if((null == factions) || factions.isEmpty()) {
                return false;
            }
            boolean empty = true;
            for(Faction faction : factions) {
                String id = faction.getShortName();
                // TODO: Replace with proper methods instead of magic strings
                if(!id.equals("UND") && !id.equals("ABN") && !id.equals("NONE")) { //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
                    empty = false;
                }
            }
            return !empty;
        }
        return true;
    }
    
    /**
     * Activate and Center
     */
    private void center(Planet p) {

        if (p == null) {
            return;
        }
        conf.centerX = - p.getX();
        conf.centerY = p.getY();
        repaint();
    }

    private void zoom(double percent, Point pos) {
        if(null != pos) {
            // TODO: Calculate offset to zoom at mouse position
        }
        conf.scale *= percent;
        repaint();
    }

    public Planet getSelectedPlanet() {
        return selectedPlanet;
    }

    public JumpPath getJumpPath() {
        return jumpPath;
    }

    private void changeSelectedPlanet(Planet p) {
        selectedPlanet = p;
        jumpPath = new JumpPath();
        hqview.refreshPlanetView();
    }

    private void openPlanetEventEditor(Planet p) {
        NewPlanetaryEventDialog editor = new NewPlanetaryEventDialog(null, campaign, selectedPlanet);
        editor.setVisible(true);
        List<Planet.PlanetaryEvent> result = editor.getChangedEvents();
        if((null != result) && !result.isEmpty()) {
            Planets.getInstance().updatePlanetaryEvents(p.getId(), result, true);
            repaint();
            hqview.refreshPlanetView();
        }
    }
    
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
}
