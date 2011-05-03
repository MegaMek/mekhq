/*
 * InterstellarMapPanel
 *
 * Created on May 3, 2011
 */

package mekhq;

import java.awt.Color;
import java.awt.Graphics;
import java.util.ArrayList;

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
	
	public InterstellarMapPanel(ArrayList<Planet> p) {
		planets = p;
	}
	
	/**
     * Computes the map-coordinate from the screen koordinate system
     */
	//TODO: need some sort of scaling
    private double scr2mapX(int x) {
        return Math.round((x - getWidth() / 2));
    }

    private int map2scrX(double x) {
        return (int) Math.round(getWidth() / 2 + x);
    }

    private double scr2mapY(int y) {
        return Math.round((getHeight() / 2 - y));
    }

    private int map2scrY(double y) {
        return (int) Math.round(getHeight() / 2 + y);
    }

	
	protected void paintComponent(Graphics g) {
		g.setColor(Color.BLACK);
		g.fillRect(0, 0, getWidth(), getHeight());
		
		for(Planet planet : planets) {
			g.setColor(Faction.getFactionColor(planet.getFaction()));
			g.fillArc(map2scrX(planet.getX()), map2scrY(planet.getY()), 5, 5, 0, 360);		
		}
	}
	
}
	