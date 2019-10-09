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
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(Color.BLACK);
                g2.fillRect(0, 0, getWidth(), getHeight());

                double y = fullHeight / 2.0;
                int nPlanets = system.getPlanets().size();
                
                for(Planet p : system.getPlanets()) {
                    double x = fullWidth * p.getSystemPosition()/(nPlanets * 1.0);
                    Image planetIcon = ImageUtil.loadImageFromFile("data/" + StarUtil.getIconImage(p));
                    g2.drawImage(planetIcon, (int) x, (int) y, 64, 64, null);
                    final String planetName = p.getPrintableName(Utilities.getDateTimeDay(campaign.getCalendar()));
                    g2.setPaint(Color.WHITE);
                    drawCenteredString(g2, planetName, new Rectangle((int) x, (int) (y+64), 64, 30), new Font("Helvetica", Font.PLAIN, 18));
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
    
    public void drawCenteredString(Graphics g, String text, Rectangle rect, Font font) {
        // Get the FontMetrics
        FontMetrics metrics = g.getFontMetrics(font);
        // Determine the X coordinate for the text
        int x = rect.x + (rect.width - metrics.stringWidth(text)) / 2;
        // Determine the Y coordinate for the text (note we add the ascent, as in java 2d 0 is top of the screen)
        int y = rect.y + ((rect.height - metrics.getHeight()) / 2) + metrics.getAscent();
        // Set the font
        //g.setFont(font);
        // Draw the String
        g.drawString(text, x, y);
    }
}