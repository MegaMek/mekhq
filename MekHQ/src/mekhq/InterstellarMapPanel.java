/*
 * InterstellarMapPanel
 *
 * Created on May 3, 2011
 */

package mekhq;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseAdapter;
import java.util.ArrayList;
import java.util.Iterator;

import javax.swing.BorderFactory;

import mekhq.campaign.Faction;
import mekhq.campaign.Planet;


/**
 * This is not functional yet. Just testing things out.
 * A lot of this code is borrowed from InterstellarMap.java in MekWars
 * @author  Jay Lawson <jaylawson39 at yahoo.com>
 */
public class InterstellarMapPanel extends javax.swing.JPanel {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = -1110105822399704646L;

	private ArrayList<Planet> planets;
	InnerStellarMapConfig conf = new InnerStellarMapConfig();
	
	public InterstellarMapPanel(ArrayList<Planet> p) {
		planets = p;
		
		setBorder(BorderFactory.createLineBorder(Color.black));
        
        addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent e) {
            	Planet selectedPlanet = nearestNeighbour(scr2mapX(e.getX()), scr2mapY(e.getY()));  
            	center(selectedPlanet);
            }          
        });
	}
	
	/**
     * Computes the map-coordinate from the screen koordinate system
     */
    private double scr2mapX(int x) {
        return Math.round((x - getWidth() / 2 - conf.offset.x) / conf.scale);
    }

    private int map2scrX(double x) {
        return (int) Math.round(getWidth() / 2 + x * conf.scale) + conf.offset.x;
    }

    private double scr2mapY(int y) {
        return Math.round((getHeight() / 2 - (y - conf.offset.y)) / conf.scale);
    }

    private int map2scrY(double y) {
        return (int) Math.round(getHeight() / 2 - y * conf.scale) + conf.offset.y;
    }

	
	protected void paintComponent(Graphics g) {
		g.setColor(Color.BLACK);
		g.fillRect(0, 0, getWidth(), getHeight());
		int size = (int) Math.round(Math.max(5, Math.log(conf.scale) * 15 + 5));
        size = Math.max(Math.min(size, conf.maxdotSize), conf.minDotSize);
        
		for(Planet planet : planets) {
			int x = map2scrX(planet.getX());
			int y = map2scrY(planet.getY());
			g.setColor(Faction.getFactionColor(planet.getFaction()));
			g.fillArc(x, y, size, size, 0, 360);		
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
    public void center(Planet p) {

        if (p == null) {
            return;
        }
        conf.offset.setLocation(-p.getX() * conf.scale, p.getY() * conf.scale);
        repaint();
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
        int minDotSize = 2;
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
        double showPlanetNamesThreshold = 0.0;
        /**
         * brightness correction for colors. This is no gamma correction! Gamma correction brightens medium level colors more than extreme ones. 0 means no brightening.
         */
        double colorAdjustment = 0.5;
        /**
         * The maps background color
         */
        String backgroundColor = "#000000";

        /**
         * The actual scale factor. 1.0 for default, higher means bigger.
         */
        double scale = 1.0;
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
	