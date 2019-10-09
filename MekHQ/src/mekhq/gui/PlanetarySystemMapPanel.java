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

    public PlanetarySystemMapPanel(Campaign c) {
        
        this.campaign = c;
        this.system = campaign.getCurrentSystem();
        
        mapPanel = new JPanel() {
            private static final long serialVersionUID = -6666762147393179909L;
    
            @Override
            protected void paintComponent(Graphics g) {
                int fullWidth = getWidth();
                int fullHeight = getHeight();
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
                
                Color[] colors = new Color[] {Color.PINK, Color.GREEN, Color.BLUE, Color.CYAN, Color.MAGENTA, Color.ORANGE, Color.YELLOW};
                
                for(int i = 0; i < (n+1); i++) {
                    //for testing
                    //g2.setColor(colors[i]);
                    //g2.fillRect(rectWidth*i, 0, rectWidth, getHeight());
                    Planet p = system.getPlanet(i);
                    if(null != p) {
                        Image planetIcon = ImageUtil.loadImageFromFile("data/" + StarUtil.getIconImage(p));
                        g2.drawImage(planetIcon, rectWidth*i+midpoint-32, y-32, 64, 64, null);
                        final String planetName = p.getPrintableName(Utilities.getDateTimeDay(campaign.getCalendar()));
                        g2.setColor(Color.WHITE);
                        drawCenteredString(g2, planetName, rectWidth*i+midpoint, y+32+12+g.getFontMetrics().getHeight());
                    }
                }
            }
        };
        add(mapPanel);
        repaint();
    }
    
    @Override
    protected void paintComponent(Graphics g) {
        int width = getWidth();
        int height = getHeight();
        mapPanel.setBounds(0, 0, width, height);
        super.paintComponent(g);
    }
    
    public void drawCenteredString(Graphics g, String text, int x, int y) {
        FontMetrics metrics = g.getFontMetrics();
        x = x - (metrics.stringWidth(text) / 2);
        y = y - (metrics.getHeight() / 2);
        g.drawString(text, x, y);
    }
}