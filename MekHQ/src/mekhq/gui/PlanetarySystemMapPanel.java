package mekhq.gui;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
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
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLayeredPane;
import javax.swing.JPanel;

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
    private int[] diameters;
    private BufferedImage spaceImage;
    
    private static int minDiameter = 16;
    private static int maxDiameter = 64;

    public PlanetarySystemMapPanel(Campaign c, CampaignGUI view) {
        
        this.hqview = view;
        this.campaign = c;
        this.system = campaign.getCurrentSystem();
        selectedPlanet = system.getPrimaryPlanetPosition();
        diameters = new int[system.getPlanets().size()];
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
                Graphics2D g2 = (Graphics2D) g;
                Arc2D.Double arc = new Arc2D.Double();
                g2.setFont(new Font("Helvetica", Font.PLAIN, 14));
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


                //split canvas into n+1 equal rectangles where n is the number of planetary systems.
                //the first rectangle is for the star
                int n = system.getPlanets().size();
                int rectWidth = getWidth() / (n+1);
                int midpoint = rectWidth / 2;
                int y = getHeight() / 2;
                int x = 0;
                
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
                        diameters[i-1] = diameter;
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
                        drawCenteredString(g2, planetName, x, y+(biggestDiameterPixels/2)+12+g.getFontMetrics().getHeight());
                        
                    }
                }
            }
        };
        
        btnBack = new JButton("< Back"); // NOI18N
        btnBack.addActionListener(ev -> back());
        
        addMouseListener(new MouseAdapter() {
            
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
        btnBack.setBounds(0, 0, 100, 50);
        super.paintComponent(g);
    }
    
    private void drawCenteredString(Graphics g, String text, int x, int y) {
        FontMetrics metrics = g.getFontMetrics();
        x = x - (metrics.stringWidth(text) / 2);
        y = y - (metrics.getHeight() / 2);
        g.drawString(text, x, y);
    }
    
    public void updatePlanetarySystem(PlanetarySystem s) {
        this.system = s;
        selectedPlanet = system.getPrimaryPlanetPosition();
        diameters = new int[system.getPlanets().size()];
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
            //must be within radius
            //add a little wiggle room to radius
            int radius = (diameters[i-1] / 2)+2;
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