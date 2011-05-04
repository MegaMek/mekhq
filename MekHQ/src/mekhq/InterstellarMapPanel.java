/*
 * InterstellarMapPanel
 *
 * Created on May 3, 2011
 */

package mekhq;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseWheelEvent;
import java.util.ArrayList;
import java.util.Iterator;

import javax.swing.BorderFactory;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;

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
	private Planet selectedPlanet = null;
	Point lastMousePos = null;
    int mouseMod = 0;
	
	public InterstellarMapPanel(ArrayList<Planet> p) {
		planets = p;
		
		setBorder(BorderFactory.createLineBorder(Color.black));
        
		//TODO: get the key listener working
		addKeyListener(new KeyAdapter() {
			/** Handle the key pressed event from the text field. */
			public void keyPressed(KeyEvent e) {
				int keyCode = e.getKeyCode();
	        
				if (keyCode == 37)// left arrow
		        {
		        	conf.offset.y -= conf.scale;
		        } else if (keyCode == 38) // uparrow
		        {
		        	conf.offset.x -= conf.scale;
		        } else if (keyCode == 39)// right arrow
		        {
	     	      conf.offset.y += conf.scale;
		        } else if (keyCode == 40)// down arrow
		        {
		        	conf.offset.x += conf.scale;
		        } else {
		        	return;
		        }
		        repaint();
			}
		});
		
        addMouseListener(new MouseAdapter() {
        	
        	public void mouseEntered(MouseEvent e) {
                lastMousePos = new Point(e.getX(), e.getY());
            }

            public void mouseExited(MouseEvent e) {
                lastMousePos = null;
            }
            
            public void mouseReleased(MouseEvent e) {
                mouseMod = 0;
            }
            
            public void mousePressed(MouseEvent e) {
            	mouseMod = e.getButton();
                if (e.getButton() != MouseEvent.BUTTON1) {
                    return;
                }
            	selectedPlanet = nearestNeighbour(scr2mapX(e.getX()), scr2mapY(e.getY()));  
            	repaint();
            }          
            
            public void mouseClicked(MouseEvent e) {

                if (e.getButton() == MouseEvent.BUTTON3) {
                	JPopupMenu popup = new JPopupMenu();
                	JMenuItem item;
                	item = new JMenuItem("Zoom In");
                	item.addActionListener(new ActionListener() {
                        public void actionPerformed(ActionEvent ae) {
                            zoom(0.5);
                        }
                    });
                	popup.add(item);
                	item = new JMenuItem("Zoom Out");
                	item.addActionListener(new ActionListener() {
                        public void actionPerformed(ActionEvent ae) {
                            zoom(-0.5);
                        }
                    });
                	popup.add(item);
                	JMenu centerM = new JMenu("Center Map");
                    item = new JMenuItem("On Selected Planet");
                    item.setEnabled(selectedPlanet != null);
                    if (selectedPlanet != null) {// only add if there is a planet to center on
                        item.addActionListener(new ActionListener() {
                            public void actionPerformed(ActionEvent ae) {
                                center(selectedPlanet);
                            }
                        });
                    }
                    centerM.add(item);
                    item = new JMenuItem("On Terra");
                    item.addActionListener(new ActionListener() {
                        public void actionPerformed(ActionEvent ae) {
                            conf.offset = new Point();
                            repaint();
                        }
                    });
                    centerM.add(item);
                    popup.add(centerM);
                	popup.show(e.getComponent(), e.getX() + 10, e.getY() + 10);
                }
                else if (e.getButton() == MouseEvent.BUTTON1) {

                    if (e.getClickCount() >= 2) {
                    	//center and zoom
                    	selectedPlanet = nearestNeighbour(scr2mapX(e.getX()), scr2mapY(e.getY()));  
                    	if(conf.scale < 4.0) {
                    		conf.scale = 4.0;
                    	}
                    	center(selectedPlanet);             	
                    }
                }
            }
        });
        
        addMouseMotionListener(new MouseAdapter() {
        	
        	public void mouseDragged(MouseEvent e) {
                if (mouseMod != MouseEvent.BUTTON1) {
                    return;
                }
                //TODO: dragging is too fast and awkward
                if (lastMousePos != null) {
                    conf.offset.x -= lastMousePos.x - e.getX();
                    conf.offset.y -= lastMousePos.y - e.getY();
                }
                repaint();
            }
        	
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
        	 public void mouseWheelMoved(MouseWheelEvent e) {
        	       zoom(e.getWheelRotation() * -0.5);
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
			
			if(null != selectedPlanet && selectedPlanet.equals(planet)) {
				g.setColor(Color.WHITE);
				int adjust = size/2;
				g.fillArc(x-adjust/2, y-adjust/2, size+adjust, size+adjust, 0, 360);	
			}
			g.setColor(Faction.getFactionColor(planet.getFaction()));
			g.fillArc(x, y, size, size, 0, 360);	
			//name
			if (conf.showPlanetNamesThreshold == 0 || conf.scale > conf.showPlanetNamesThreshold) {
                g.drawString(planet.getName(), x + size, y);
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
    public void center(Planet p) {

        if (p == null) {
            return;
        }
        conf.offset.setLocation(-p.getX() * conf.scale, p.getY() * conf.scale);
        repaint();
    }
    
    public void zoom(double percent) {
    	conf.scale *= (1+percent);
        if (selectedPlanet != null) {
            conf.offset.setLocation(-selectedPlanet.getX() * conf.scale, selectedPlanet.getY() * conf.scale);
        }
        repaint();
    }
    
    public Planet getSelectedPlanet() {
    	return selectedPlanet;
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
        double showPlanetNamesThreshold = 3.0;
        /**
         * brightness correction for colors. This is no gamma correction! Gamma correction brightens medium level colors more than extreme ones. 0 means no brightening.
         */
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
	