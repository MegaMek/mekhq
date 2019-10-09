package mekhq.gui;

import java.awt.BasicStroke;
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
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.AffineTransform;
import java.awt.geom.Arc2D;
import java.awt.geom.GeneralPath;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import javax.swing.ImageIcon;
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
    
    private JPanel mapPanel;
    
    private Campaign campaign;
    private PlanetarySystem system;
    private int selectedPlanet = 0;
    int diameter;

    public PlanetarySystemMapPanel(Campaign c) {
        
        this.campaign = c;
        this.system = campaign.getCurrentSystem();
        selectedPlanet = system.getPrimaryPlanetPosition();
        diameter = 64;
        
        mapPanel = new JPanel() {
            private static final long serialVersionUID = -6666762147393179909L;
    
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g;
                g2.setFont(new Font("Helvetica", Font.PLAIN, 18));
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(Color.BLACK);
                g2.fillRect(0, 0, getWidth(), getHeight());

                //split canvas into n+1 equal rectangles where n is the number of planetary systems.
                //the first rectangle is for the star
                int n = system.getPlanets().size();
                int rectWidth = getWidth() / (n+1);
                int midpoint = rectWidth / 2;
                int y = getHeight() / 2;
                int x = 0;
                
                //adjust diameter based on rectangle width but only up to some maximum and minimum
                diameter = rectWidth-32;
                if(diameter < 64) {
                    diameter = 64;
                } else if(diameter > 128) {
                    diameter = 128;
                }
                int radius = diameter / 2;
                
                Color[] colors = new Color[] {Color.PINK, Color.GREEN, Color.BLUE, Color.CYAN, Color.MAGENTA, Color.ORANGE, Color.YELLOW};
                
                Arc2D.Double arc = new Arc2D.Double();

                for(int i = 0; i < (n+1); i++) {
                    x = rectWidth*i+midpoint;
                    //for testing
                    //g2.setColor(colors[i]);
                    //g2.fillRect(rectWidth*i, 0, rectWidth, getHeight());
                    Planet p = system.getPlanet(i);
                    if(null != p) {
                        if(i > 0 & selectedPlanet==i) {
                            //lest try rings
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
                        Image planetIcon = ImageUtil.loadImageFromFile("data/" + StarUtil.getIconImage(p));
                        g2.drawImage(planetIcon, x-radius, y-radius, diameter, diameter, null);
                        final String planetName = p.getPrintableName(Utilities.getDateTimeDay(campaign.getCalendar()));
                        g2.setColor(Color.WHITE);
                        drawCenteredString(g2, planetName, x, y+radius+12+g.getFontMetrics().getHeight());
                    }
                }
            }
        };
        add(mapPanel);
        
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
        
        repaint();
    }
    
    @Override
    protected void paintComponent(Graphics g) {
        int width = getWidth();
        int height = getHeight();
        mapPanel.setBounds(0, 0, width, height);
        super.paintComponent(g);
    }
    
    private void drawCenteredString(Graphics g, String text, int x, int y) {
        FontMetrics metrics = g.getFontMetrics();
        x = x - (metrics.stringWidth(text) / 2);
        y = y - (metrics.getHeight() / 2);
        g.drawString(text, x, y);
    }
    
    public Planet getSelectedPlanet() {
        return system.getPlanet(selectedPlanet);
    }
    
    private void changeSelectedPlanet(int pos) {
        selectedPlanet = pos;
        //notifyListeners();
    }
    
    /**
     * Calculate the nearest neighbour for the given point If anyone has a better algorithm than this stupid kind of shit, please, feel free to exchange my brute force thing... An good idea would be an voronoi diagram and the sweep algorithm from Steven Fortune.
     */
    private int nearestNeighbour(double x, double y) {
        int n = system.getPlanets().size();
        int rectWidth = getWidth() / (n+1);
        int midpoint = rectWidth / 2;
        int xTarget = 0;
        int yTarget = getHeight() / 2;
        //add a little wiggle room to radius
        int radius = (diameter / 2)+2;
        
        for(int i = 1; i < (n+1); i++) {
            xTarget = rectWidth*i+midpoint;
            //must be within radius
            if(x <= (xTarget+radius) & x >= (xTarget-radius) &
                    y <= (yTarget+radius) & y >= (yTarget-radius)) {
                return i;
            }
        }
        return 0;
    }
}