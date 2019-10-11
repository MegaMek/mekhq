package mekhq.gui;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Insets;
import java.awt.LinearGradientPaint;
import java.awt.MultipleGradientPaint;
import java.awt.Paint;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.AffineTransform;
import java.awt.geom.Arc2D;
import java.awt.geom.GeneralPath;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.imageio.ImageIO;
import javax.swing.AbstractAction;
import javax.swing.ActionMap;
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.InputMap;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLayeredPane;
import javax.swing.JPanel;
import javax.swing.KeyStroke;

import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;

import megamek.common.EquipmentType;
import megamek.common.util.ImageUtil;
import mekhq.Utilities;
import mekhq.campaign.Campaign;
import mekhq.campaign.JumpPath;
import mekhq.campaign.universe.Faction;
import mekhq.campaign.universe.Planet;
import mekhq.campaign.universe.PlanetarySystem;
import mekhq.campaign.universe.StarUtil;
import mekhq.campaign.universe.Systems;

public class PlanetarySystemMapPanel extends JPanel {

    /**
     * 
     */
    private static final long serialVersionUID = 2756160214370516878L;
    
    private JLayeredPane pane;
    private JPanel mapPanel;
    private JButton btnBack;
    
    private Campaign campaign;
    private CampaignGUI hqview;
    private PlanetarySystem system;
    private int selectedPlanet = 0;
    private BufferedImage spaceImage;
    
    private static int minDiameter = 16;
    private static int maxDiameter = 64;

    public PlanetarySystemMapPanel(Campaign c, CampaignGUI view) {
        
        this.hqview = view;
        this.campaign = c;
        this.system = campaign.getCurrentSystem();
        selectedPlanet = system.getPrimaryPlanetPosition();
        
        try {
            spaceImage = ImageIO.read(new File("data/images/universe/space.jpg"));
        } catch (IOException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        }
        //spaceImage = Toolkit.getDefaultToolkit().createImage("data/images/universe/space.jpg");
        
        pane = new JLayeredPane();
        
        mapPanel = new JPanel() {
            private static final long serialVersionUID = -6666762147393179909L;
    
            @Override
            protected void paintComponent(Graphics g) {
                int n = system.getPlanets().size();
                
                Graphics2D g2 = (Graphics2D) g;
                Arc2D.Double arc = new Arc2D.Double();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(Color.BLACK);
                g2.fillRect(0, 0, getWidth(), getHeight());
                //tile the space image
                if(null != spaceImage) {
                    int tileWidth = spaceImage.getWidth();
                    int tileHeight = spaceImage.getHeight();
                    for (int y = 0; y < getHeight(); y += tileHeight) {
                        for (int x = 0; x < getWidth(); x += tileWidth) {
                            g2.drawImage(spaceImage, x, y, this);
                        }
                    }
                }

                int rectWidth = getWidth() / (n+1);
                int midpoint = rectWidth / 2;
                int y = getHeight() / 2;
                int x = 0;
                chooseFont(g2, system, campaign, rectWidth-6);
                
                //split canvas into n+1 equal rectangles where n is the number of planetary systems.
                //the first rectangle is for the star
                
                
                //place the sun first
                Image sunIcon = ImageUtil.loadImageFromFile("data/" + StarUtil.getIconImage(system));
                int sunHeight = sunIcon.getHeight(null);
                int sunWidth = sunIcon.getWidth(null);
                double sunRatio = sunHeight/sunWidth;
                int maxSunWidth = (int) Math.round(rectWidth * 0.9);
                int maxSunHeight =  (int) Math.round(maxSunWidth * sunRatio);
                g2.drawImage(sunIcon, x, y-150, maxSunWidth, 300, null);

                //draw nadir and zenith points
                g2.setPaint(Color.WHITE);
                arc.setArcByCenter(rectWidth / 2, 60, 10, 0, 360, Arc2D.OPEN);
                g2.fill(arc);
                arc.setArcByCenter(rectWidth / 2, getHeight()-60, 10, 0, 360, Arc2D.OPEN);
                g2.fill(arc);
                
                //get the biggest diameter allowed within this space for a planet
                int biggestDiameterPixels = rectWidth-32;
                if(biggestDiameterPixels < minDiameter) {
                    biggestDiameterPixels = minDiameter;
                } else if(biggestDiameterPixels > maxDiameter) {
                    biggestDiameterPixels = maxDiameter;
                }
                
                //find the biggest diameter among all planets
                double biggestDiameter = 0;
                for(Planet p : system.getPlanets()) {
                    if(p.getDiameter() > biggestDiameter) {
                        biggestDiameter = p.getDiameter();
                    }
                }
                
                for(int i = 0; i < (n+1); i++) {
                    x = rectWidth*i+midpoint;
                    Planet p = system.getPlanet(i);
                    if(i > 0 & null != p) {
                        //diameters need to be scaled relative to largest planet, but linear 
                        //scale will make all but gas/ice giants tiny. log scale made sizes too close,
                        //but cubic root scale seems to work pretty well.
                        int diameter = (int) ((biggestDiameterPixels) * (Math.cbrt(p.getDiameter())/Math.cbrt(biggestDiameter)));
                        if(diameter < minDiameter) {
                            diameter = minDiameter;
                        } else if(diameter > maxDiameter) {
                            diameter = maxDiameter;
                        }
                        int radius = diameter / 2;
                        
                        //add ring for selected planet
                        if(i > 0 & selectedPlanet==i) {
                            g2.setPaint(Color.ORANGE);
                            arc.setArcByCenter(x, y, radius+6, 0, 360, Arc2D.OPEN);
                            g2.fill(arc);
                            g2.setPaint(Color.BLACK);
                            arc.setArcByCenter(x, y, radius+4, 0, 360, Arc2D.OPEN);
                            g2.fill(arc);
                            g2.setPaint(Color.ORANGE);
                            arc.setArcByCenter(x, y, radius+3, 0, 360, Arc2D.OPEN);
                            g2.fill(arc);
                            g2.setPaint(Color.BLACK);
                            arc.setArcByCenter(x, y, radius+2, 0, 360, Arc2D.OPEN);
                            g2.fill(arc);
                        }
                        
                        //draw the planet icon
                        Image planetIcon = ImageUtil.loadImageFromFile("data/" + StarUtil.getIconImage(p));
                        g2.drawImage(planetIcon, x-radius, y-radius, diameter, diameter, null);
                        final String planetName = p.getPrintableName(Utilities.getDateTimeDay(campaign.getCalendar()));
                        
                        //planet name
                        g2.setColor(Color.WHITE);
                        drawCenteredString(g2, planetName, x, y+(biggestDiameterPixels/2)+12+g.getFontMetrics().getHeight(), rectWidth-6);
                        
                        //check for current location - we assume you are on primary planet for now
                        if(campaign.getLocation().getCurrentSystem().equals(system) && i==system.getPrimaryPlanetPosition()) {
                            g2.setColor(Color.WHITE);
                            JumpPath jp = campaign.getLocation().getJumpPath();
                            if(null != jp && (!campaign.getLocation().isAtJumpPoint() || jp.getLastSystem().equals(system))) {
                                //the unit has a flight plan in this system so draw the line
                                //in transit so draw a path
                                Stroke dashed = new BasicStroke(3, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 0, new float[]{9}, 0);
                                g2.setStroke(dashed);
                                g2.drawLine(x, y-radius, rectWidth / 2, 60);
                            }
                            if(campaign.getLocation().isAtJumpPoint()) {
                                //draw at jump point
                                arc.setArcByCenter((rectWidth / 2) + 12, 60, 10, 0, 360, Arc2D.OPEN);
                                g2.fill(arc);
                            } else if(campaign.getLocation().isOnPlanet()) {
                                arc.setArcByCenter(x-radius, y-radius+12, 10, 0, 360, Arc2D.OPEN);
                                g2.fill(arc);
                            } else {                       
                                arc.setArcByCenter((x-(rectWidth / 2))*campaign.getLocation().getPercentageTransit()+(rectWidth / 2), (y-radius-60)*campaign.getLocation().getPercentageTransit()+60, 10, 0, 360, Arc2D.OPEN);
                                g2.fill(arc);
                            }
                        }
                    }
                }
                
            }
        };
        
        btnBack = new JButton(); // NOI18N
        btnBack.setFocusable(false);
        btnBack.setPreferredSize(new Dimension(36, 36));
        btnBack.setMargin(new Insets(0, 0, 0, 0));
        btnBack.setBorder(BorderFactory.createEmptyBorder());
        btnBack.setToolTipText("Back to Interstellar Map");
        btnBack.setBackground(Color.DARK_GRAY);
        btnBack.setIcon(new ImageIcon("data/images/misc/back_button.png"));
        btnBack.addActionListener(ev -> back());
        
        //set up key bindings
        getInputMap(WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "back");
        getActionMap().put("back", new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                back();
            }
        });
        
        getInputMap(WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_A, 0), "left");
        getInputMap(WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_W, 0), "left");
        getInputMap(WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, 0), "left");
        getInputMap(WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_KP_LEFT, 0), "left");
        getInputMap(WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_UP, 0), "left");
        getActionMap().put("left", new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                int target_pos = selectedPlanet-1;
                if(target_pos<1) {
                    return;
                }
                changeSelectedPlanet(target_pos);
                repaint();
            }
        });
        
        getInputMap(WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_D, 0), "right");
        getInputMap(WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_S, 0), "right");
        getInputMap(WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, 0), "right");
        getInputMap(WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_KP_RIGHT, 0), "right");
        getInputMap(WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, 0), "right");
        getActionMap().put("right", new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                int target_pos = selectedPlanet+1;
                if(target_pos>(system.getPlanets().size())) {
                    return;
                }
                changeSelectedPlanet(target_pos);
                repaint();
            }
        });

        
        
        mapPanel.addMouseListener(new MouseAdapter() {
            
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getButton() == MouseEvent.BUTTON1) {
                    int target_pos = nearestNeighbour(e.getX(), e.getY());
                    if(target_pos < 1) {
                        return;
                    }
                    changeSelectedPlanet(target_pos);
                    repaint();
                }
            }
        });
        
        pane.add(mapPanel, Integer.valueOf(1));
        pane.add(btnBack, Integer.valueOf(10));
        
        add(pane);
        
        repaint();
        
    }
    
    @Override
    protected void paintComponent(Graphics g) {
        int width = getWidth();
        int height = getHeight();
        pane.setBounds(0, 0, width, height);
        mapPanel.setBounds(0, 0, width, height);
        btnBack.setBounds(4, 4, 40, 40);
        super.paintComponent(g);
    }
    
    private void chooseFont(Graphics g, PlanetarySystem system, Campaign c, int limit) {
        //start with 16
        int fontSize = 16;
        g.setFont(new Font("Helvetica", Font.PLAIN, fontSize));
        while(areNamesTooBig(g, system, Utilities.getDateTimeDay(campaign.getCalendar()), limit) && fontSize>=10) {
            fontSize--;
            g.setFont(new Font("Helvetica", Font.PLAIN, fontSize));
        }
    }
    
    private boolean areNamesTooBig(Graphics g, PlanetarySystem system, DateTime when, int limit) {
        for(Planet p : system.getPlanets()) {
            String name = p.getName(when);
            if(g.getFontMetrics().stringWidth(name) > limit) {
                //try splitting
                if(name.contains(" ")) {
                    for (String line : name.split("\\s+")) {
                        if(g.getFontMetrics().stringWidth(line) > limit) {
                            return true;
                        }
                    }
                } else if(name.contains("-")) {
                    for (String line : name.split("-")) {
                        if(g.getFontMetrics().stringWidth(line) > limit) {
                            return true;
                        }
                    }
                }else {
                    return true;
                }
            }
        }
        return false;
    }
    
    private void drawCenteredString(Graphics2D g, String text, int x, int y, int limit) {
        FontMetrics metrics = g.getFontMetrics();
        y = y - (metrics.getHeight() / 2);
        if(metrics.stringWidth(text) > limit && (text.contains(" ") || text.contains("-"))) {
            if(text.contains(" ")) {
                //try spaces first
                for (String line : text.split("\\s+")) {
                    g.drawString(line, x- (metrics.stringWidth(line) / 2), y += g.getFontMetrics().getHeight());
                }
            } else {
                //otherwise break on the hyphen
                String[] lines = text.split("-");
                for (int i = 0; i < lines.length; i++) {
                    String line = lines[i];
                    if(i != (lines.length-1)) {
                        line = line + "-";
                    }
                    g.drawString(line, x- (metrics.stringWidth(line) / 2), y += g.getFontMetrics().getHeight());
                }
            }
        } else {
            g.drawString(text, x- (metrics.stringWidth(text) / 2), y += g.getFontMetrics().getHeight());
        }
    }
    
    public void updatePlanetarySystem(PlanetarySystem s) {
        this.system = s;
        selectedPlanet = system.getPrimaryPlanetPosition();
        mapPanel.repaint();
    }
    
    public int getSelectedPlanetPosition() {
        return selectedPlanet;
    }
    
    private void changeSelectedPlanet(int pos) {
        selectedPlanet = pos;
        notifyListeners();
    }
    
    private int nearestNeighbour(double x, double y) {
        int n = system.getPlanets().size();
        int rectWidth = getWidth() / (n+1);
        int midpoint = rectWidth / 2;
        int xTarget = 0;
        int yTarget = getHeight() / 2;
        
        for(int i = 1; i < (n+1); i++) {
            xTarget = rectWidth*i+midpoint;
            //must be within total possible radius
            int radius = 32;
            if(x <= (xTarget+radius) & x >= (xTarget-radius) &
                    y <= (yTarget+radius) & y >= (yTarget-radius)) {
                return i;
            }
        }
        return 0;
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
    
    private void back() {
        ((MapTab)hqview.getTab(GuiTabType.MAP)).switchSystemsMap();
    }
}