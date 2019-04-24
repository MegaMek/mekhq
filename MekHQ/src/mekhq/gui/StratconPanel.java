package mekhq.gui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Polygon;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.AffineTransform;

import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.SwingUtilities;

import megamek.common.Coords;
import mekhq.campaign.Campaign;
import mekhq.campaign.mission.AtBContract;
import mekhq.campaign.stratcon.StratconCampaignState;
import mekhq.campaign.stratcon.StratconRulesManager;
import mekhq.campaign.stratcon.StratconScenario;
import mekhq.campaign.stratcon.StratconTrackState;
import mekhq.gui.stratcon.StratconScenarioWizard;
import mekhq.gui.stratcon.TrackForceAssignmentUI;

/**
 * This panel handles AtB-Stratcon GUI interactions with a specific scenario track.
 * @author NickAragua
 *
 */
public class StratconPanel extends JPanel implements ActionListener {
    /**
     * 
     */
    private static final long serialVersionUID = 7405934788894417292L;
    
    public static final int HEX_X_RADIUS = 42;
    public static final int HEX_Y_RADIUS = 37;
    
    private static final String RCLICK_COMMAND_MANAGE_FORCES = "ManageForces";
    private static final String RCLICK_COMMAND_MANAGE_SCENARIO = "ManageScenario";

    private enum DrawHexType {
        Hex,
        Outline,
        Dryrun
    }

    private float scale = 1f;

    private StratconTrackState currentTrack;
    private StratconCampaignState campaignState;
    private Campaign campaign;

    private BoardState boardState = new BoardState();

    private Point clickedPoint;
    private JPopupMenu rightClickMenu;
    private JMenuItem menuItemInitiateScenario;
    private JMenuItem menuItemManageScenario;
    
    private StratconScenarioWizard scenarioWizard;
    private TrackForceAssignmentUI assignmentUI;

    StratconPanel(CampaignGUI gui) {
        campaign = gui.getCampaign();
        scenarioWizard = new StratconScenarioWizard(campaign);
        
        buildRightClickMenu(null);
        
        assignmentUI = new TrackForceAssignmentUI(campaign);
        assignmentUI.setVisible(false);
        
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseReleased(MouseEvent e) {
                mouseReleasedHandler(e);
            }
        });
    }

    public void selectTrack(StratconCampaignState campaignState, StratconTrackState track) {
        this.campaignState = campaignState;
        currentTrack = track;
        
        this.repaint();
    }
    
    private void buildRightClickMenu(StratconScenario scenario) {
        rightClickMenu = new JPopupMenu();
        
        JMenuItem itemManageForceAssignments = new JMenuItem();
        itemManageForceAssignments.setText("Manage Force Assignments");
        itemManageForceAssignments.setActionCommand(RCLICK_COMMAND_MANAGE_FORCES);
        itemManageForceAssignments.addActionListener(this);
        rightClickMenu.add(itemManageForceAssignments);
        
        //if(scenario != null) {
            menuItemManageScenario = new JMenuItem();
            menuItemManageScenario.setText("Manage Scenario");
            menuItemManageScenario.setActionCommand(RCLICK_COMMAND_MANAGE_SCENARIO);
            menuItemManageScenario.addActionListener(this);
            rightClickMenu.add(menuItemManageScenario);
        //}
    }

    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        if(campaignState == null || currentTrack == null) {
            return;
        }

        Graphics2D g2D = (Graphics2D) g;
        AffineTransform initialTransform = g2D.getTransform();
        performInitialTransform(g2D);
        AffineTransform originTransform = g2D.getTransform();

        drawHexes(g2D, DrawHexType.Hex);
        g2D.setTransform(originTransform);
        drawHexes(g2D, DrawHexType.Outline);
        g2D.setTransform(originTransform);
        g2D.translate(HEX_X_RADIUS, HEX_Y_RADIUS);
        drawScenarios(g2D);
        g2D.setTransform(originTransform);
        g2D.translate(HEX_X_RADIUS, HEX_Y_RADIUS);
        drawFacilities(g2D);

        g2D.setTransform(initialTransform);
        if(clickedPoint != null) {
            g2D.setColor(Color.BLUE);
            g2D.drawRect((int) clickedPoint.getX(), (int) clickedPoint.getY(), 2, 2);
        }
    }

    /**
     * This method contains a dirty secret hack, but I forget what it is.
     * The point of it is to draw all the hexes for the board. 
     * If it's a "dry run", we don't actually draw the hexes, we just pretend to
     * until we "draw" one that encompasses the clicked point.
     * @param g2D - graphics object on which to draw
     * @param drawHexType - whether to draw the hex backgrounds, hex outlines or a dry run for click detection
     */
    private boolean drawHexes(Graphics2D g2D, DrawHexType drawHexType) {
        Polygon graphHex = new Polygon();
        int xRadius = HEX_X_RADIUS;
        int yRadius = HEX_Y_RADIUS;
        boolean pointFound = false;

        graphHex.addPoint(-xRadius/2, -yRadius);
        graphHex.addPoint(-xRadius, 0);
        graphHex.addPoint(-xRadius/2, yRadius);
        graphHex.addPoint(xRadius/2, yRadius);
        graphHex.addPoint(xRadius, 0);
        graphHex.addPoint(xRadius/2, -yRadius);

        graphHex.translate(xRadius, yRadius);

        Point translatedClickedPoint = null;

        // this was derived somewhat experimentally
        // the clicked point always seems a little off, so we 
        // a) apply the current transform to it, prior to drawing all the hexes
        // b) subtract an additional Y_RADIUS x 2 (Y_DIAMETER)
        // this gets us the point within the clicked hex
        // it's probably finicky, so any major changes to the rendering mechanism will likely break the detection
        if(clickedPoint != null) {
            translatedClickedPoint = (Point) clickedPoint.clone();
            
            // since we have the possibility of scrolling, we need to convert the on-screen clicked coordinates
            // to on-board coordinates. Thankfully, SwingUtilities provides the main computational ability for that
            Point actualPanelPoint = SwingUtilities.convertPoint(this, translatedClickedPoint, this.getParent());
            translatedClickedPoint.translate((int) -(actualPanelPoint.getX() - translatedClickedPoint.getX()), 
                                                (int) -(actualPanelPoint.getY() - translatedClickedPoint.getY()));
            
            // now we translate to the starting point of where we're drawing and then go down a hex
            translatedClickedPoint.translate((int) g2D.getTransform().getTranslateX(), (int) g2D.getTransform().getTranslateY());
            translatedClickedPoint.translate(0, -(HEX_Y_RADIUS * 2));
        }

        for(int x = 0; x < currentTrack.getWidth(); x++) {            
            for(int y = 0; y < currentTrack.getHeight(); y++) {
                if(drawHexType == DrawHexType.Outline) {
                    g2D.setColor(new Color(0, 0, 0));
                    g2D.drawPolygon(graphHex);                    
                } else if(drawHexType == DrawHexType.Hex) {
                    if(translatedClickedPoint != null && graphHex.contains(translatedClickedPoint)) {
                        g2D.setColor(Color.WHITE);
                        boardState.selectedX = x;
                        boardState.selectedY = y;
                        pointFound = true;
                    } else {
                        g2D.setColor(Color.DARK_GRAY);
                    }

                    g2D.fillPolygon(graphHex);
                } else if(drawHexType == DrawHexType.Dryrun) {
                    if(translatedClickedPoint != null && graphHex.contains(translatedClickedPoint)) {
                        boardState.selectedX = x;
                        boardState.selectedY = y;
                        pointFound = true;
                    }
                }

                if(drawHexType == DrawHexType.Hex) {
                    g2D.setColor(Color.GREEN);
                    g2D.drawString(x + "," + y, graphHex.xpoints[0] + (xRadius / 4), graphHex.ypoints[0] + yRadius);
                }

                int[] downwardVector = getDownwardYVector();
                graphHex.translate(downwardVector[0], downwardVector[1]);
            }

            int[] translationVector = getRightAndUPVector(x % 2 == 0);
            graphHex.translate(translationVector[0], translationVector[1]);
        }

        return pointFound;
    }

    private void drawScenarios(Graphics2D g2D) {
        Polygon scenarioMarker = new Polygon();
        int xRadius = HEX_X_RADIUS / 3;
        int yRadius = HEX_Y_RADIUS / 3;

        scenarioMarker.addPoint(-xRadius, -yRadius);
        scenarioMarker.addPoint(-xRadius, yRadius);
        scenarioMarker.addPoint(xRadius, yRadius);
        scenarioMarker.addPoint(xRadius, -yRadius);

        for(int x = 0; x < currentTrack.getWidth(); x++) {            
            for(int y = 0; y < currentTrack.getHeight(); y++) {
                if(currentTrack.getScenario(new Coords(x, y)) != null) {
                    g2D.setColor(Color.RED);
                    g2D.drawPolygon(scenarioMarker);
                }

                int[] downwardVector = getDownwardYVector();
                scenarioMarker.translate(downwardVector[0], downwardVector[1]);
            }

            int[] translationVector = getRightAndUPVector(x % 2 == 0);
            scenarioMarker.translate(translationVector[0], translationVector[1]);
        }
    }
    
    private void drawFacilities(Graphics2D g2D) {
        Polygon facilityMarker = new Polygon();
        int xRadius = HEX_X_RADIUS / 3;
        int yRadius = HEX_Y_RADIUS / 3;

        facilityMarker.addPoint(-xRadius, -yRadius);
        facilityMarker.addPoint(-xRadius, yRadius);
        facilityMarker.addPoint(xRadius, yRadius);
        facilityMarker.addPoint(xRadius, -yRadius);

        for(int x = 0; x < currentTrack.getWidth(); x++) {            
            for(int y = 0; y < currentTrack.getHeight(); y++) {
                if(currentTrack.getFacility(new Coords(x, y)) != null) {
                    g2D.setColor(Color.GREEN);
                    g2D.drawPolygon(facilityMarker);
                }

                int[] downwardVector = getDownwardYVector();
                facilityMarker.translate(downwardVector[0], downwardVector[1]);
            }

            int[] translationVector = getRightAndUPVector(x % 2 == 0);
            facilityMarker.translate(translationVector[0], translationVector[1]);
        }
    }

    /**
     * Returns the translation that we need to make to render the "next downward" hex.
     * @return Two dimensional array with the first element being the x vector and the second being the y vector
     */
    private int[] getDownwardYVector() {
        return new int[] { 0, (int) (HEX_Y_RADIUS * 2) };
    }

    /**
     * Returns the translation that we need to make to move from the bottom of a column to the top of the next
     * column to the right.
     * @param evenColumn Whether the column we're currently in is odd or even
     * @return Two dimensional array with the first element being the x vector and the second being the y vector
     */
    private int[] getRightAndUPVector(boolean evenColumn) {
        int yRadius = (int) (HEX_Y_RADIUS);
        int xRadius = (int) (HEX_X_RADIUS);

        int yTranslation = currentTrack.getHeight() * yRadius * 2;
        if(evenColumn) {
            yTranslation += yRadius;
        } else {
            yTranslation -= yRadius;
        }

        return new int[] {(int) (xRadius * 1.5), -yTranslation};
    }

    private void performInitialTransform(Graphics2D g2D) {
        g2D.translate(0, 0 + HEX_Y_RADIUS);
        g2D.scale(scale, scale);
    }

    /** 
     * Worker function that takes the current clicked point and a graphics 2D object
     * and detects which hex was clicked by doing a dry run hex render.
     * 
     * Dependent upon clickedPoint being set and having an active graphics object for this class.
     * 
     * Side effects: the dry run sets the boardState clicked hex coordinates.
     * @return Whether or not the clicked point was found on the hex board
     */
    private boolean detectClickedHex() {
        Graphics2D g2D = (Graphics2D) getGraphics();
        AffineTransform transform = g2D.getTransform();
        performInitialTransform(g2D);
        boolean pointFoundOnBoard = drawHexes(g2D, DrawHexType.Dryrun);
        g2D.setTransform(transform);

        return pointFoundOnBoard;
    }

    public void mouseReleasedHandler(MouseEvent e) {
        if(e.getSource() != this) {
            return;
        }

        if(e.getButton() == MouseEvent.BUTTON1) {        
            clickedPoint = e.getPoint();
            boolean pointFoundOnBoard = detectClickedHex();

            repaint();
        } else if(e.getButton() == MouseEvent.BUTTON3) {
            clickedPoint = new Point(e.getX(), e.getY());
            boolean pointFoundOnBoard = detectClickedHex();

            StratconScenario selectedScenario = null;
            if(pointFoundOnBoard) {
                selectedScenario = currentTrack.getScenario(new Coords(boardState.selectedX, boardState.selectedY));
            }
             
            menuItemManageScenario.setEnabled(selectedScenario != null);
            repaint();
            rightClickMenu.show(this, e.getX(), e.getY());
        }
    }

    public void focusLostHandler(FocusEvent e) {
        //clickedPoint = null;
        //repaint();
    }

    private class BoardState {
        public int selectedX;
        public int selectedY;
        public int selectedTrack;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        switch(e.getActionCommand()) {
        case RCLICK_COMMAND_MANAGE_FORCES:
            assignmentUI.display(campaignState, 0);
            assignmentUI.setVisible(true);
            break;
        case RCLICK_COMMAND_MANAGE_SCENARIO:
            scenarioWizard.setCurrentScenario(currentTrack.getScenario(new Coords(boardState.selectedX, boardState.selectedY)),
                    currentTrack,
                    campaignState);
            scenarioWizard.setVisible(true);
            break;
        }
        
        repaint();
    }

    @Override
    public Dimension getPreferredSize() {
        int xDimension = (int) (HEX_X_RADIUS * 1.75 * currentTrack.getWidth());
        int yDimension = (int) (HEX_Y_RADIUS * 2.1 * currentTrack.getHeight());
        
        return new Dimension(xDimension, yDimension);
    }
}
