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
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.util.ArrayList;

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
	MekHQView hqview;
	private Planet selectedPlanet = null;
	Point lastMousePos = null;
    int mouseMod = 0;
	
	public InterstellarMapPanel(ArrayList<Planet> p, MekHQView view) {
		planets = p;
		hqview = view;
		
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
            	maybeShowPopup(e);
                mouseMod = 0;
            }
            
            public void mousePressed(MouseEvent e) {
            	maybeShowPopup(e);
            	mouseMod = e.getButton();
                if (e.isPopupTrigger() || e.getButton() != MouseEvent.BUTTON1) {
                    return;
                }
            	changeSelectedPlanet(nearestNeighbour(scr2mapX(e.getX()), scr2mapY(e.getY())));  
            	repaint();
            }          
            
     
            public void maybeShowPopup(MouseEvent e) {
                if (e.isPopupTrigger()) {
                	JPopupMenu popup = new JPopupMenu();
                	JMenuItem item;
                	item = new JMenuItem("Zoom In");
                	item.addActionListener(new ActionListener() {
                        public void actionPerformed(ActionEvent ae) {
                            zoom(1.5);
                        }
                    });
                	popup.add(item);
                	item = new JMenuItem("Zoom Out");
                	item.addActionListener(new ActionListener() {
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
                    //set up a series of menus by alphabet and allow user to select
                	JMenu selectM = new JMenu("Select Planet");
                	JMenu aMenu = new JMenu("A");
                	JMenu bMenu = new JMenu("B");
                	JMenu cMenu = new JMenu("C");
                	JMenu dMenu = new JMenu("D");
                	JMenu eMenu = new JMenu("E");
                	JMenu fMenu = new JMenu("F");
                	JMenu gMenu = new JMenu("G");
                	JMenu hMenu = new JMenu("H");
                	JMenu iMenu = new JMenu("I");
                	JMenu jMenu = new JMenu("J");
                	JMenu kMenu = new JMenu("K");
                	JMenu lMenu = new JMenu("L");
                	JMenu mMenu = new JMenu("M");
                	JMenu nMenu = new JMenu("N");
                	JMenu oMenu = new JMenu("O");
                	JMenu pMenu = new JMenu("P");
                	JMenu qMenu = new JMenu("Q");
                	JMenu rMenu = new JMenu("R");
                	JMenu sMenu = new JMenu("S");
                	JMenu tMenu = new JMenu("T");
                	JMenu uMenu = new JMenu("U");
                	JMenu vMenu = new JMenu("V");
                	JMenu wMenu = new JMenu("W");
                	JMenu xMenu = new JMenu("X");
                	JMenu yMenu = new JMenu("Y");
                	JMenu zMenu = new JMenu("Z");
                	for(int i = 0; i < planets.size(); i++) {
                		Planet p = planets.get(i);
                		item = new JMenuItem(p.getName() + " (" + Faction.getFactionName(p.getFaction()) + ")");
    					item.setActionCommand(Integer.toString(i));
            			item.addActionListener(new ActionListener() {
                            public void actionPerformed(ActionEvent ae) {
                            	int pos = Integer.parseInt(ae.getActionCommand());
                                changeSelectedPlanet(planets.get(pos));
                                center(selectedPlanet);
                            }
                        });
                		if(p.getName().toLowerCase().startsWith("a")) {
                			aMenu.add(item);
                		} else if(p.getName().toLowerCase().startsWith("b")) {
                			bMenu.add(item);
                		} else if(p.getName().toLowerCase().startsWith("c")) {
                			cMenu.add(item);
                		} else if(p.getName().toLowerCase().startsWith("d")) {
                			dMenu.add(item);
                		} else if(p.getName().toLowerCase().startsWith("e")) {
                			eMenu.add(item);
                		} else if(p.getName().toLowerCase().startsWith("f")) {
                			fMenu.add(item);
                		} else if(p.getName().toLowerCase().startsWith("g")) {
                			gMenu.add(item);
                		} else if(p.getName().toLowerCase().startsWith("h")) {
                			hMenu.add(item);
                		} else if(p.getName().toLowerCase().startsWith("i")) {
                			iMenu.add(item);
                		} else if(p.getName().toLowerCase().startsWith("j")) {
                			jMenu.add(item);
                		} else if(p.getName().toLowerCase().startsWith("k")) {
                			kMenu.add(item);
                		} else if(p.getName().toLowerCase().startsWith("l")) {
                			lMenu.add(item);
                		} else if(p.getName().toLowerCase().startsWith("m")) {
                			mMenu.add(item);
                		} else if(p.getName().toLowerCase().startsWith("n")) {
                			nMenu.add(item);
                		} else if(p.getName().toLowerCase().startsWith("o")) {
                			oMenu.add(item);
                		} else if(p.getName().toLowerCase().startsWith("p")) {
                			pMenu.add(item);
                		} else if(p.getName().toLowerCase().startsWith("q")) {
                			qMenu.add(item);
                		} else if(p.getName().toLowerCase().startsWith("r")) {
                			rMenu.add(item);
                		} else if(p.getName().toLowerCase().startsWith("s")) {
                			sMenu.add(item);
                		} else if(p.getName().toLowerCase().startsWith("t")) {
                			tMenu.add(item);
                		} else if(p.getName().toLowerCase().startsWith("u")) {
                			uMenu.add(item);
                		} else if(p.getName().toLowerCase().startsWith("v")) {
                			vMenu.add(item);
                		} else if(p.getName().toLowerCase().startsWith("w")) {
                			wMenu.add(item);
                		} else if(p.getName().toLowerCase().startsWith("x")) {
                			xMenu.add(item);
                		} else if(p.getName().toLowerCase().startsWith("y")) {
                			yMenu.add(item);
                		} else if(p.getName().toLowerCase().startsWith("z")) {
                			zMenu.add(item);
                		}
                		
                	}
                	MenuScroller.setScrollerFor(aMenu, 30);
                    selectM.add(aMenu);
                	MenuScroller.setScrollerFor(aMenu, 30);
                    selectM.add(bMenu);
                	MenuScroller.setScrollerFor(bMenu, 30);
                    selectM.add(cMenu);
                	MenuScroller.setScrollerFor(cMenu, 30);
                    selectM.add(dMenu);
                	MenuScroller.setScrollerFor(dMenu, 30);
                    selectM.add(eMenu);
                	MenuScroller.setScrollerFor(eMenu, 30);
                    selectM.add(fMenu);
                	MenuScroller.setScrollerFor(fMenu, 30);
                    selectM.add(gMenu);
                	MenuScroller.setScrollerFor(gMenu, 30);
                    selectM.add(hMenu);
                	MenuScroller.setScrollerFor(hMenu, 30);
                    selectM.add(iMenu);
                	MenuScroller.setScrollerFor(iMenu, 30);
                    selectM.add(jMenu);
                	MenuScroller.setScrollerFor(jMenu, 30);
                    selectM.add(kMenu);
                	MenuScroller.setScrollerFor(kMenu, 30);
                    selectM.add(lMenu);
                	MenuScroller.setScrollerFor(lMenu, 30);
                    selectM.add(mMenu);
                	MenuScroller.setScrollerFor(mMenu, 30);
                    selectM.add(nMenu);
                	MenuScroller.setScrollerFor(nMenu, 30);
                    selectM.add(oMenu);
                	MenuScroller.setScrollerFor(oMenu, 30);
                    selectM.add(pMenu);
                	MenuScroller.setScrollerFor(pMenu, 30);
                    selectM.add(qMenu);
                	MenuScroller.setScrollerFor(qMenu, 30);
                    selectM.add(rMenu);
                	MenuScroller.setScrollerFor(rMenu, 30);
                    selectM.add(sMenu);
                	MenuScroller.setScrollerFor(sMenu, 30);
                    selectM.add(tMenu);
                	MenuScroller.setScrollerFor(tMenu, 30);
                    selectM.add(uMenu);
                	MenuScroller.setScrollerFor(uMenu, 30);
                    selectM.add(vMenu);
                	MenuScroller.setScrollerFor(vMenu, 30);
                    selectM.add(wMenu);
                	MenuScroller.setScrollerFor(wMenu, 30);
                    selectM.add(xMenu);
                	MenuScroller.setScrollerFor(xMenu, 30);
                    selectM.add(yMenu);
                	MenuScroller.setScrollerFor(yMenu, 30);
                    selectM.add(zMenu);
                	MenuScroller.setScrollerFor(zMenu, 30);
                    popup.add(selectM);
                	popup.show(e.getComponent(), e.getX() + 10, e.getY() + 10);
                }
            }
                
            public void mouseClicked(MouseEvent e) {
            	if (e.getButton() == MouseEvent.BUTTON1) {
        		
            		if (e.getClickCount() >= 2) {
            			//center and zoom
            			changeSelectedPlanet(nearestNeighbour(scr2mapX(e.getX()), scr2mapY(e.getY())));  
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
        		 zoom(Math.pow(1.5,-1 * e.getWheelRotation()));
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
    
    private void changeSelectedPlanet(Planet p) {
    	selectedPlanet = p;
    	hqview.refreshPlanetView();
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
	