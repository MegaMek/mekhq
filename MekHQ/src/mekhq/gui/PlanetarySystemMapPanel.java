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
import java.awt.image.AffineTransformOp;
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

import megamek.client.ui.swing.util.PlayerColors;
import megamek.common.Dropship;
import megamek.common.Entity;
import megamek.common.EquipmentType;
import megamek.common.Jumpship;
import megamek.common.MechFileParser;
import megamek.common.MechSummary;
import megamek.common.MechSummaryCache;
import megamek.common.loaders.EntityLoadingException;
import megamek.common.util.DirectoryItems;
import megamek.common.util.ImageUtil;
import mekhq.Utilities;
import mekhq.campaign.Campaign;
import mekhq.campaign.JumpPath;
import mekhq.campaign.unit.Unit;
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
    private DirectoryItems camos;
    private PlanetarySystem system;
    private int selectedPlanet = 0;
    
    //get the best dropship and jumpship of the unit for display
    private Unit dropship;
    private Unit jumpship;
    
    private BufferedImage imgJumpPoint;
    private BufferedImage imgRechargeStation;
    private BufferedImage imgDropshipFleet;
    private BufferedImage imgJumpshipFleet;
    private BufferedImage imgSpace;
    
    private static int minDiameter = 16;
    private static int maxDiameter = 64;

    public PlanetarySystemMapPanel(Campaign c, CampaignGUI view) {
        
        this.hqview = view;
        this.campaign = c;
        this.system = campaign.getCurrentSystem();
        selectedPlanet = system.getPrimaryPlanetPosition();
        camos = hqview.getIconPackage().getCamos();
        //TODO: need to update this on new day
        dropship = getBestDropship();
        imgDropshipFleet = getEntityImage("Union (3055)");
        if(null != dropship) {
            imgDropshipFleet = getEntityImage(dropship);
        }
        
        try {
            imgSpace = ImageIO.read(new File("data/images/universe/space.jpg"));
        } catch (IOException e1) {
            imgSpace = null;
            e1.printStackTrace();
        }
        try {
            imgJumpPoint = ImageIO.read(new File("data/images/units/jumpships/invader.png"));
        } catch (IOException e) {
            imgJumpPoint = null;
            e.printStackTrace();
        }
        
        try {
            imgRechargeStation = ImageIO.read(new File("data/images/units/Space Stations/Olympus.png"));
        } catch (IOException e) {
            imgRechargeStation = null;
            e.printStackTrace();
        }
        
        pane = new JLayeredPane();
        
        mapPanel = new JPanel() {
            private static final long serialVersionUID = -6666762147393179909L;
    
            @Override
            protected void paintComponent(Graphics g) {
                int n = system.getPlanets().size();
                
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(Color.BLACK);
                g2.fillRect(0, 0, getWidth(), getHeight());
                //tile the space image
                if(null != imgSpace) {
                    int tileWidth = imgSpace.getWidth();
                    int tileHeight = imgSpace.getHeight();
                    for (int y = 0; y < getHeight(); y += tileHeight) {
                        for (int x = 0; x < getWidth(); x += tileWidth) {
                            g2.drawImage(imgSpace, x, y, this);
                        }
                    }
                }

                int rectWidth = getWidth() / (n+1);
                int midpoint = rectWidth / 2;
                int y = getHeight() / 2;
                int x = 0;
                
                int jumpPointImgSize = 64;
                int shipImgSize = 24;
                int nadirX = midpoint;
                int nadirY = getHeight()-60-jumpPointImgSize;
                int zenithX = midpoint;
                int zenithY = 60;
                
                chooseFont(g2, system, campaign, rectWidth-6);
                
                //split canvas into n+1 equal rectangles where n is the number of planetary systems.
                //the first rectangle is for the star
                
                
                //place the sun first
                Image sunIcon = ImageUtil.loadImageFromFile("data/" + StarUtil.getIconImage(system));
                //int sunHeight = sunIcon.getHeight(null);
                //int sunWidth = sunIcon.getWidth(null);
                //double sunRatio = sunHeight/sunWidth;
                int maxSunWidth = (int) Math.round(rectWidth * 0.9);
                //int maxSunHeight =  (int) Math.round(maxSunWidth * sunRatio);
                g2.drawImage(sunIcon, x, y-150, maxSunWidth, 300, null);

                //draw nadir and zenith points
                g2.setPaint(Color.WHITE);
                if(system.isZenithCharge(Utilities.getDateTimeDay(campaign.getCalendar()))) {
                    if(null != imgRechargeStation) {
                        drawRotatedImage(g2, imgRechargeStation, 90.0, zenithX, zenithY, jumpPointImgSize, jumpPointImgSize);
                    }
                } else {
                    if(null != imgJumpPoint) {
                        drawRotatedImage(g2, imgJumpPoint, 90.0, zenithX, zenithY, jumpPointImgSize, jumpPointImgSize);
                    }
                }
                if(system.isNadirCharge(Utilities.getDateTimeDay(campaign.getCalendar()))) {
                    if(null != imgRechargeStation) {
                        drawRotatedImage(g2, imgRechargeStation, 90.0, nadirX, nadirY, jumpPointImgSize, jumpPointImgSize);
                    }
                } else {
                    if(null != imgJumpPoint) {
                        drawRotatedImage(g2, imgJumpPoint, 90.0, nadirX, nadirY, jumpPointImgSize, jumpPointImgSize);
                    }
                }

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
                        
                        //check for current location - we assume you are on primary planet for now
                        if(campaign.getLocation().getCurrentSystem().equals(system) && i==system.getPrimaryPlanetPosition()) {
                            JumpPath jp = campaign.getLocation().getJumpPath();
                            
                            if(null != jp && (!campaign.getLocation().isAtJumpPoint() || jp.getLastSystem().equals(system))) {
                                //the unit has a flight plan in this system so draw the line
                                //in transit so draw a path
                                g2.setColor(Color.YELLOW);
                                Stroke dashed = new BasicStroke(3, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 0, new float[]{9}, 0);
                                g2.setStroke(dashed);
                                g2.drawLine(x, y-radius, zenithX+jumpPointImgSize, zenithY+jumpPointImgSize);
                            }
                            if(campaign.getLocation().isAtJumpPoint()) {
                                //draw at jump point
                                drawRing(g2, zenithX + jumpPointImgSize+8+(shipImgSize/2), zenithY+(jumpPointImgSize/2), shipImgSize/2, Color.ORANGE);
                                if(null != imgDropshipFleet) {
                                    g2.drawImage(imgDropshipFleet, zenithX + jumpPointImgSize+8, zenithY+(jumpPointImgSize/2) - (shipImgSize/2), shipImgSize, shipImgSize, null);
                                }
                            } else if(campaign.getLocation().isOnPlanet()) {
                                drawRing(g2, x, y, radius, Color.ORANGE);
                                if(null != imgDropshipFleet) {
                                    g2.drawImage(imgDropshipFleet, x-radius-shipImgSize, y-radius-shipImgSize, shipImgSize, shipImgSize, null);
                                }
                            } else { 
                                if(null != imgDropshipFleet) {
                                    //TODO: figure out correct angles
                                    int lengthX = x-zenithX-jumpPointImgSize;
                                    int lengthY = y-radius-zenithY-jumpPointImgSize;
                                    double rotationRequired = (-1) * Math.toDegrees(Math.atan(lengthX / ((1.0 * lengthY))));
                                    if(null != jp && jp.getLastSystem().equals(system)) {
                                        //inbound
                                        rotationRequired = 180+rotationRequired;
                                    }                                   
                                    int partialX = (int) Math.round((lengthX)*campaign.getLocation().getPercentageTransit()+zenithX+jumpPointImgSize);
                                    int partialY = (int) Math.round((lengthY)*campaign.getLocation().getPercentageTransit()+zenithY+jumpPointImgSize);
                                    drawRing(g2, partialX+(shipImgSize/2), partialY+(shipImgSize/2), shipImgSize/2, Color.ORANGE);
                                    drawRotatedImage(g2, imgDropshipFleet, rotationRequired, partialX, partialY, shipImgSize, shipImgSize);
                                }
                            }
                        }
                        
                        //add ring for selected planet
                        if(i > 0 & selectedPlanet==i) {
                            drawRing(g2, x, y, radius, Color.WHITE);
                        }
                        
                        //draw the planet icon
                        Image planetIcon = ImageUtil.loadImageFromFile("data/" + StarUtil.getIconImage(p));
                        g2.drawImage(planetIcon, x-radius, y-radius, diameter, diameter, null);
                        final String planetName = p.getPrintableName(Utilities.getDateTimeDay(campaign.getCalendar()));
                        
                        //planet name
                        g2.setColor(Color.WHITE);
                        drawCenteredString(g2, planetName, x, y+(biggestDiameterPixels/2)+12+g.getFontMetrics().getHeight(), rectWidth-6);
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
    
    private BufferedImage getEntityImage(String name) {
        MechSummary ms = MechSummaryCache.getInstance().getMech(name);
        if(ms==null) {
            return null;
        }
        Entity e;
        Image img = null;
        try {
            e = new MechFileParser(ms.getSourceFile(), ms.getEntryName()).getEntity();
            if(null == e) {
                return null;
            }
            img = hqview.getIconPackage().getMechTiles().imageFor(e, this, -1);
            int tint = PlayerColors.getColorRGB(campaign.getColorIndex());
            EntityImage entityImage = new EntityImage(img, tint, getCamo(), this);
            img = entityImage.loadPreviewImage();
            return Utilities.toBufferedImage(img);
        } catch (EntityLoadingException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
            return null;
        }
    }
    
    private BufferedImage getEntityImage(Unit u) {
        Image img = null;
        img = hqview.getIconPackage().getMechTiles().imageFor(u.getEntity(), this, -1);
        if(img == null) {
            return null;
        }
        int tint = PlayerColors.getColorRGB(campaign.getColorIndex());
        EntityImage entityImage = new EntityImage(img, tint, getCamo(), this);
        img = entityImage.loadPreviewImage();
        return Utilities.toBufferedImage(img);
    }
    
    private Image getCamo() {
        Image camo = null;
        try {
            camo = (Image) camos.getItem(campaign.getCamoCategory(), campaign.getCamoFileName());
        } catch (Exception err) {
            err.printStackTrace();
        }
        return camo;
    }
    
    private void drawRotatedImage(Graphics2D g, BufferedImage image, double degrees, int x, int y, int width, int height) {
        double rotationRequired = Math.toRadians(degrees);
        double locationX = image.getWidth() / 2;
        double locationY = image.getHeight() / 2;
        AffineTransform tx = AffineTransform.getRotateInstance(rotationRequired, locationX, locationY);
        AffineTransformOp op = new AffineTransformOp(tx, AffineTransformOp.TYPE_BILINEAR);
        // Drawing the rotated image at the required drawing locations
        g.drawImage(op.filter(image, null), x, y, width, height, null);
    }
    
    private void drawRing(Graphics2D g, int x, int y, int radius, Color c) {
        Arc2D.Double arc = new Arc2D.Double();
        g.setPaint(c);
        arc.setArcByCenter(x, y, radius+6, 0, 360, Arc2D.OPEN);
        g.fill(arc);
        g.setPaint(Color.BLACK);
        arc.setArcByCenter(x, y, radius+4, 0, 360, Arc2D.OPEN);
        g.fill(arc);
        g.setPaint(c);
        arc.setArcByCenter(x, y, radius+3, 0, 360, Arc2D.OPEN);
        g.fill(arc);
        g.setPaint(Color.BLACK);
        arc.setArcByCenter(x, y, radius+2, 0, 360, Arc2D.OPEN);
        g.fill(arc);
    };
    
    private Unit getBestDropship() {
        Unit bestUnit = null;
        double bestWeight = 0.0;
        for(Unit u : campaign.getUnits()) {
            if(u.getEntity() instanceof Dropship && u.getEntity().getWeight() > bestWeight) {
                bestUnit = u;
                bestWeight = u.getEntity().getWeight();
            }
        }
        return bestUnit;
    }
    
    private Unit getBestJumpship() {
        Unit bestUnit = null;
        double bestWeight = 0.0;
        for(Unit u : campaign.getUnits()) {
            if(u.getEntity() instanceof Jumpship && u.getEntity().getWeight() > bestWeight) {
                bestUnit = u;
                bestWeight = u.getEntity().getWeight();
            }
        }
        return bestUnit;
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