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

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
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
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;

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

    private ArrayList<Planet> planets;
    private JumpPath jumpPath;
    private Campaign campaign;
    private InnerStellarMapConfig conf = new InnerStellarMapConfig();
    private CampaignGUI hqview;
    private Planet selectedPlanet = null;
    private Point lastMousePos = null;
    private int mouseMod = 0;

    public InterstellarMapPanel(Campaign c, CampaignGUI view) {
        campaign = c;
        planets = campaign.getPlanets();
        hqview = view;
        jumpPath = new JumpPath();

        setBorder(BorderFactory.createLineBorder(Color.black));

        addKeyListener(new KeyAdapter() {
            /** Handle the key pressed event from the text field. */
            @Override
            public void keyPressed(KeyEvent e) {
                int keyCode = e.getKeyCode();
                boolean moved = false;
                if(keyCode == KeyEvent.VK_LEFT) {
                    conf.offset.y -= conf.scale;
                    moved = true;
                }
                if(keyCode == KeyEvent.VK_RIGHT) {
                    conf.offset.y += conf.scale;
                    moved = true;
                }
                if(keyCode == KeyEvent.VK_DOWN) {
                    conf.offset.x += conf.scale;
                    moved = true;
                }
                if(keyCode == KeyEvent.VK_UP) {
                    conf.offset.x -= conf.scale;
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
                            zoom(1.5);
                        }
                    });
                    popup.add(item);
                    item = new JMenuItem("Zoom Out");
                    item.addActionListener(new ActionListener() {
                        @Override
                        public void actionPerformed(ActionEvent ae) {
                            zoom(0.5);
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
                            conf.offset = new Point();
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
                    conf.offset.x -= lastMousePos.x - e.getX();
                    conf.offset.y -= lastMousePos.y - e.getY();
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
                 zoom(Math.pow(1.5,-1 * e.getWheelRotation()));
             }
        });
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

    /**
     * Computes the map-coordinate from the screen coordinate system
     */
    private double scr2mapX(int x) {
        return Math.round((x - getWidth() / 2 - conf.offset.x) / conf.scale);
    }

    private double map2scrX(double x) {
        return Math.round(getWidth() / 2 + x * conf.scale) + conf.offset.x;
    }

    private double scr2mapY(int y) {
        return Math.round((getHeight() / 2 - (y - conf.offset.y)) / conf.scale);
    }

    private double map2scrY(double y) {
        return Math.round(getHeight() / 2 - y * conf.scale) + conf.offset.y;
    }

    public void setSelectedPlanet(Planet p) {
        selectedPlanet = p;
        if(conf.scale < 4.0) {
            conf.scale = 4.0;
        }
        center(selectedPlanet);
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g;
        g2.setColor(Color.BLACK);
        g2.fillRect(0, 0, getWidth(), getHeight());
        double size = 1 + 5 * Math.log(conf.scale);
        size = Math.max(Math.min(size, conf.maxdotSize), conf.minDotSize);
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
            Set<Faction> factions = planet.getFactionSet(new DateTime(campaign.getCalendar()));
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

        //cycle through planets again and assign names - to make sure names go on outside
        for(Planet planet : planets) {
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

    /**
     * Activate and Center
     */
    private void center(Planet p) {

        if (p == null) {
            return;
        }
        conf.offset.setLocation(-p.getX() * conf.scale, p.getY() * conf.scale);
        repaint();
    }

    private void zoom(double percent) {
        conf.scale *= percent;
        if (selectedPlanet != null) {
            conf.offset.setLocation(-selectedPlanet.getX() * conf.scale, selectedPlanet.getY() * conf.scale);
        }
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
        Point offset = new Point();
        /**
         * The current selected Planet-id
         */
        int planetID;
    }
}
